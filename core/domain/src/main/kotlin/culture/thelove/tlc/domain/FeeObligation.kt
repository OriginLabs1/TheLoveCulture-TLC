package culture.thelove.tlc.domain

/** Identity of one fee obligation (DAILY/MONTHLY period of a Love Model couple). */
public data class ObligationId(public val value: String) {
    init {
        require(value.isNotBlank()) { "ObligationId must not be blank." }
    }
}

public enum class FeePeriod { ONE_OFF, MONTHLY }

/**
 * The life of a Pioneer fee. Every state is an *assertion*, never a balance.
 *
 * DUE -> INSTRUCTION_ISSUED -> PROOF_SUBMITTED -> ATTESTED
 *   \-> EXPIRED           \-> EXCEPTION (three-way match failed)
 *   \-> WAIVED (voluntary tokens only)
 */
public enum class ObligationState {
    DUE,
    INSTRUCTION_ISSUED,
    PROOF_SUBMITTED,
    ATTESTED,
    EXCEPTION,
    WAIVED,
    EXPIRED,
    ;

    public val terminal: Boolean
        get() = this == ATTESTED || this == WAIVED || this == EXPIRED
}

public data class ProofRecord(
    public val proofHash: String,
    public val statedAmount: Money,
    public val bankReference: String,
    public val submittedBy: PartyId,
    public val at: Long,
) {
    init {
        require(proofHash.length == 64) { "Proof hash must be a SHA-256 hex digest." }
        require(bankReference.isNotBlank()) { "A proof needs the sender's bank reference." }
    }
}

public data class AttestationRecord(
    public val attestedBy: PartyId,
    public val secondAttestor: PartyId?,
    public val amount: Money,
    public val statementRef: String,
    public val at: Long,
) {
    init {
        require(statementRef.isNotBlank()) { "An attestation needs a statement reference." }
    }
}

public data class WaiverRecord(public val by: PartyId, public val note: String, public val at: Long)

public data class FeeObligation(
    public val id: ObligationId,
    public val dyad: Set<PartyId>,
    public val kind: TokenKind,
    public val amount: Money,
    public val period: FeePeriod,
    public val state: ObligationState = ObligationState.DUE,
    public val instructionIssuedAt: Long? = null,
    public val instructionExpiresAt: Long? = null,
    public val proof: ProofRecord? = null,
    public val attestation: AttestationRecord? = null,
    public val exceptionCode: Rejection? = null,
    public val waiver: WaiverRecord? = null,
    public val revision: Int = 0,
) {
    init {
        require(dyad.size == 2) { "A fee obligation belongs to a dyad of exactly two members." }
        require(kind.pioneerFee) { "Only platform tokens create Pioneer obligations: $kind." }
        require(amount > Money.ZERO) { "An obligation of zero has no meaning." }
    }

    public fun isMember(party: PartyId): Boolean = party in dyad
}

/** Everything that can happen to an obligation. Closed set: no unknown events can be invented. */
public sealed interface FeeEvent {
    public val actor: Actor
    public val at: Long

    public data class IssueInstruction(
        override val actor: Actor,
        override val at: Long,
        public val ttlMillis: Long = INSTRUCTION_TTL_MILLIS,
    ) : FeeEvent

    public data class SubmitProof(
        override val actor: Actor,
        override val at: Long,
        public val proofHash: String,
        public val statedAmount: Money,
        public val bankReference: String,
    ) : FeeEvent

    public data class Attest(
        override val actor: Actor,
        override val at: Long,
        public val amount: Money,
        public val statementRef: String,
        public val secondAttestor: PartyId? = null,
    ) : FeeEvent

    public data class FlagException(
        override val actor: Actor,
        override val at: Long,
        public val code: Rejection,
    ) : FeeEvent

    public data class Waive(
        override val actor: Actor,
        override val at: Long,
        public val note: String,
    ) : FeeEvent

    public data class Expire(
        override val actor: Actor,
        override val at: Long,
    ) : FeeEvent

    public companion object {
        /** BANK INSTRUCTION DETAILS EXPIRE — Pioneer account data is never cached client-side. */
        public const val INSTRUCTION_TTL_MILLIS: Long = 15 * 60 * 1000

        /** ₦50,000 in kobo: at or above this, two independent attestors are required. */
        public const val FOUR_EYES_THRESHOLD_KOBO: Long = 5_000_000
    }
}

/** Side effects a transition asks the shells to perform. None of them moves money (INV-01). */
public sealed interface Effect {
    /** Structurally false for every TLC effect: custody is not expressible. */
    public val requiresCustody: Boolean get() = false

    public data class ReleasePioneerDetails(public val ttlMillis: Long) : Effect

    public data class IssueReceipt(public val obligation: ObligationId) : Effect

    public data class AnchorPending(public val obligation: ObligationId) : Effect

    public data class NotifyDyad(public val obligation: ObligationId, public val code: Rejection) : Effect

    public data object AlreadyRecorded : Effect

    public data object NoReceivableCreated : Effect
}

public enum class Rejection {
    ILLEGAL_TRANSITION,
    WRONG_ACTOR,
    NOT_A_DYAD_MEMBER,
    SELF_ATTESTATION,
    SYSTEM_CANNOT_ATTEST,
    AMOUNT_MISMATCH,
    DUPLICATE_PROOF,
    PROOF_ALREADY_SUBMITTED,
    MISSING_STATEMENT_REF,
    FOUR_EYES_REQUIRED,
    INSTRUCTION_EXPIRED,
    NOT_WAIVABLE,
}

public sealed interface Transition {
    public data class Applied(public val next: FeeObligation, public val effects: List<Effect>) : Transition

    public data class Rejected(public val reason: Rejection) : Transition
}

/**
 * SK-01: the whole fee lifecycle is one total function over (state, event).
 * No other code path may mutate an obligation — shells call [apply] and render the result.
 */
public object FeeMachine {

    public fun apply(
        obligation: FeeObligation,
        event: FeeEvent,
        now: Long = event.at,
    ): Transition {
        // ATTESTED / WAIVED / EXPIRED are final. An EXCEPTION is recoverable, but only by the
        // explicit exception-resolution path, never by replaying a normal event.
        if (obligation.state.terminal) return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        return when (event) {
            is FeeEvent.IssueInstruction -> issueInstruction(obligation, event)
            is FeeEvent.SubmitProof -> submitProof(obligation, event)
            is FeeEvent.Attest -> attest(obligation, event, now)
            is FeeEvent.FlagException -> flagException(obligation, event)
            is FeeEvent.Waive -> waive(obligation, event)
            is FeeEvent.Expire -> expire(obligation, event)
        }
    }

    private fun issueInstruction(o: FeeObligation, e: FeeEvent.IssueInstruction): Transition {
        if (o.state != ObligationState.DUE) return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        if (e.actor.kind != ActorKind.MEMBER || !o.isMember(e.actor.id)) {
            return Transition.Rejected(Rejection.NOT_A_DYAD_MEMBER)
        }
        if (e.ttlMillis <= 0) return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        return Transition.Applied(
            next = o.copy(
                state = ObligationState.INSTRUCTION_ISSUED,
                instructionIssuedAt = e.at,
                instructionExpiresAt = e.at + e.ttlMillis,
                revision = o.revision + 1,
            ),
            effects = listOf(Effect.ReleasePioneerDetails(e.ttlMillis)),
        )
    }

    private fun submitProof(o: FeeObligation, e: FeeEvent.SubmitProof): Transition {
        if (o.state != ObligationState.INSTRUCTION_ISSUED && o.state != ObligationState.PROOF_SUBMITTED) {
            return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        }
        if (e.actor.kind != ActorKind.MEMBER || !o.isMember(e.actor.id)) {
            return Transition.Rejected(Rejection.NOT_A_DYAD_MEMBER)
        }
        val expiresAt = o.instructionExpiresAt ?: return Transition.Rejected(Rejection.INSTRUCTION_EXPIRED)
        if (e.at > expiresAt) return Transition.Rejected(Rejection.INSTRUCTION_EXPIRED)

        // Idempotent replay of the same evidence is not an error; it must not double-attest.
        val existing = o.proof
        if (existing != null && existing.proofHash == e.proofHash) {
            return Transition.Applied(o, listOf(Effect.AlreadyRecorded))
        }
        if (existing != null) return Transition.Rejected(Rejection.PROOF_ALREADY_SUBMITTED)
        if (e.statedAmount != o.amount) {
            // Three-way match failed at step one: route to the exception queue, never to ATTESTED.
            return Transition.Applied(
                next = o.copy(
                    state = ObligationState.EXCEPTION,
                    exceptionCode = Rejection.AMOUNT_MISMATCH,
                    proof = ProofRecord(e.proofHash, e.statedAmount, e.bankReference, e.actor.id, e.at),
                    revision = o.revision + 1,
                ),
                effects = listOf(Effect.NotifyDyad(o.id, Rejection.AMOUNT_MISMATCH)),
            )
        }
        return Transition.Applied(
            next = o.copy(
                state = ObligationState.PROOF_SUBMITTED,
                proof = ProofRecord(e.proofHash, e.statedAmount, e.bankReference, e.actor.id, e.at),
                revision = o.revision + 1,
            ),
            effects = emptyList(),
        )
    }

    private fun attest(o: FeeObligation, e: FeeEvent.Attest, now: Long): Transition {
        if (o.state != ObligationState.PROOF_SUBMITTED) {
            return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        }
        if (e.actor.kind == ActorKind.SYSTEM) return Transition.Rejected(Rejection.SYSTEM_CANNOT_ATTEST)
        if (!e.actor.isAuthority) return Transition.Rejected(Rejection.WRONG_ACTOR)
        if (e.actor.id in o.dyad) return Transition.Rejected(Rejection.SELF_ATTESTATION)

        val proof = o.proof ?: return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        if (e.amount != proof.statedAmount || e.amount != o.amount) {
            return Transition.Rejected(Rejection.AMOUNT_MISMATCH)
        }
        if (e.statementRef.isBlank()) return Transition.Rejected(Rejection.MISSING_STATEMENT_REF)

        val needsFourEyes = o.amount.kobo >= FeeEvent.FOUR_EYES_THRESHOLD_KOBO
        if (needsFourEyes) {
            val second = e.secondAttestor
            if (second == null || second == e.actor.id) {
                return Transition.Rejected(Rejection.FOUR_EYES_REQUIRED)
            }
            if (second in o.dyad) return Transition.Rejected(Rejection.SELF_ATTESTATION)
        }

        return Transition.Applied(
            next = o.copy(
                state = ObligationState.ATTESTED,
                attestation = AttestationRecord(
                    attestedBy = e.actor.id,
                    secondAttestor = e.secondAttestor,
                    amount = e.amount,
                    statementRef = e.statementRef,
                    at = now,
                ),
                revision = o.revision + 1,
            ),
            effects = listOf(Effect.IssueReceipt(o.id), Effect.AnchorPending(o.id)),
        )
    }

    private fun flagException(o: FeeObligation, e: FeeEvent.FlagException): Transition {
        if (o.state == ObligationState.EXCEPTION) {
            return Transition.Applied(o, listOf(Effect.AlreadyRecorded))
        }
        if (o.state != ObligationState.PROOF_SUBMITTED && o.state != ObligationState.DUE) {
            return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        }
        if (!e.actor.isAuthority) return Transition.Rejected(Rejection.WRONG_ACTOR)
        return Transition.Applied(
            next = o.copy(
                state = ObligationState.EXCEPTION,
                exceptionCode = e.code,
                revision = o.revision + 1,
            ),
            effects = listOf(Effect.NotifyDyad(o.id, e.code)),
        )
    }

    private fun waive(o: FeeObligation, e: FeeEvent.Waive): Transition {
        if (o.state != ObligationState.DUE && o.state != ObligationState.INSTRUCTION_ISSUED) {
            return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        }
        if (!e.actor.isAuthority) return Transition.Rejected(Rejection.WRONG_ACTOR)
        // Only voluntary tokens can be waived. Dues (Activation/Reactivation) are real obligations.
        if (!o.kind.voluntary) return Transition.Rejected(Rejection.NOT_WAIVABLE)
        return Transition.Applied(
            next = o.copy(
                state = ObligationState.WAIVED,
                waiver = WaiverRecord(e.actor.id, e.note, e.at),
                revision = o.revision + 1,
            ),
            effects = listOf(Effect.NoReceivableCreated),
        )
    }

    private fun expire(o: FeeObligation, e: FeeEvent.Expire): Transition {
        if (o.state != ObligationState.DUE && o.state != ObligationState.INSTRUCTION_ISSUED) {
            return Transition.Rejected(Rejection.ILLEGAL_TRANSITION)
        }
        if (e.actor.kind != ActorKind.SYSTEM && !e.actor.isAuthority) {
            return Transition.Rejected(Rejection.WRONG_ACTOR)
        }
        return Transition.Applied(
            next = o.copy(state = ObligationState.EXPIRED, revision = o.revision + 1),
            effects = listOf(Effect.NotifyDyad(o.id, Rejection.INSTRUCTION_EXPIRED)),
        )
    }
}
