package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.Actor
import culture.thelove.tlc.domain.ActorKind
import culture.thelove.tlc.domain.AttestationRecord
import culture.thelove.tlc.domain.Effect
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeeMachine
import culture.thelove.tlc.domain.FeeObligation
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.domain.ObligationId
import culture.thelove.tlc.domain.PartyId
import culture.thelove.tlc.domain.ProofRecord
import culture.thelove.tlc.domain.Rejection
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.domain.Transition
import culture.thelove.tlc.domain.WaiverRecord
import culture.thelove.tlc.ledger.Hasher
import culture.thelove.tlc.ledger.Hex

/**
 * The bridge between the durable due record and the domain's [FeeMachine]. Keeping this translation
 * in one tested place means the shell can never invent a transition the machine would reject.
 */
public object Dues {

    public fun create(
        id: String,
        kind: TokenKind,
        amountKobo: Long,
        period: FeePeriod,
        members: List<String>,
        now: Long,
    ): DueRecord = DueRecord(
        id = id,
        kind = kind,
        amountKobo = amountKobo,
        period = period,
        members = members,
        state = culture.thelove.tlc.domain.ObligationState.DUE,
        createdAt = now,
    )

    public fun memberActor(id: String): Actor = Actor(PartyId(id), ActorKind.MEMBER)

    public fun stewardActor(id: String): Actor = Actor(PartyId(id), ActorKind.STEWARD)

    /** The Concept Pioneer is the only party who may attest a Pioneer due. The app transcribes it. */
    public fun pioneerActor(): Actor = Actor(PartyId(PIONEER_ID), ActorKind.PIONEER)

    public const val PIONEER_ID: String = "concept-pioneer"

    /** A deterministic digest of the proof the user is recording; no image ever enters the record. */
    public fun proofHash(hasher: Hasher, amountKobo: Long, bankReference: String, at: Long): String {
        val text = "tlc-due-proof-v1|$amountKobo|$bankReference|$at"
        return Hex.encode(hasher.sha256(text.toByteArray(Charsets.UTF_8)))
    }

    public fun apply(record: DueRecord, event: FeeEvent): DueTransition =
        when (val result = FeeMachine.apply(record.toObligation(), event, event.at)) {
            is Transition.Applied -> DueTransition.Applied(
                next = dueRecordFrom(result.next, record.createdAt),
                effects = result.effects,
            )
            is Transition.Rejected -> DueTransition.Rejected(result.reason)
        }

    public fun instruction(actor: Actor, at: Long): FeeEvent = FeeEvent.IssueInstruction(actor, at)

    public fun submitProof(
        record: DueRecord,
        actor: Actor,
        at: Long,
        proofHash: String,
        statedAmountKobo: Long,
        bankReference: String,
    ): FeeEvent = FeeEvent.SubmitProof(
        actor = actor,
        at = at,
        proofHash = proofHash,
        statedAmount = Money(statedAmountKobo),
        bankReference = bankReference,
    )

    public fun attest(
        record: DueRecord,
        at: Long,
        statementRef: String,
        secondAttestor: String? = null,
    ): FeeEvent = FeeEvent.Attest(
        actor = pioneerActor(),
        at = at,
        amount = Money(record.amountKobo),
        statementRef = statementRef,
        secondAttestor = secondAttestor?.let(::PartyId),
    )

    public fun waive(record: DueRecord, actor: Actor, at: Long, note: String): FeeEvent =
        FeeEvent.Waive(actor, at, note)
}

public sealed interface DueTransition {
    public data class Applied(
        public val next: DueRecord,
        public val effects: List<Effect>,
    ) : DueTransition

    public data class Rejected(public val reason: Rejection) : DueTransition
}

/** Rehydrate the domain object. Validation in [DueRecord] guarantees the required fields exist. */
public fun DueRecord.toObligation(): FeeObligation {
    val proof = if (proofHash != null && statedAmountKobo != null && bankReference != null &&
        submittedBy != null && proofAt != null
    ) {
        ProofRecord(
            proofHash = proofHash,
            statedAmount = Money(statedAmountKobo),
            bankReference = bankReference,
            submittedBy = PartyId(submittedBy),
            at = proofAt,
        )
    } else {
        null
    }
    val attestation = if (attestedBy != null && statementRef != null && attestedAt != null) {
        AttestationRecord(
            attestedBy = PartyId(attestedBy),
            secondAttestor = secondAttestor?.let(::PartyId),
            amount = Money(amountKobo),
            statementRef = statementRef,
            at = attestedAt,
        )
    } else {
        null
    }
    val waiver = if (waivedBy != null && waiverNote != null) {
        WaiverRecord(PartyId(waivedBy), waiverNote, waivedAt ?: createdAt)
    } else {
        null
    }
    return FeeObligation(
        id = ObligationId(id),
        dyad = members.map(::PartyId).toSet(),
        kind = kind,
        amount = Money(amountKobo),
        period = period,
        state = state,
        instructionIssuedAt = instructionIssuedAt,
        instructionExpiresAt = instructionExpiresAt,
        proof = proof,
        attestation = attestation,
        exceptionCode = exception,
        waiver = waiver,
        revision = revision,
    )
}

public fun dueRecordFrom(obligation: FeeObligation, createdAt: Long): DueRecord = DueRecord(
    id = obligation.id.value,
    kind = obligation.kind,
    amountKobo = obligation.amount.kobo,
    period = obligation.period,
    members = obligation.dyad.map { it.value }.sorted(),
    state = obligation.state,
    createdAt = createdAt,
    instructionIssuedAt = obligation.instructionIssuedAt,
    instructionExpiresAt = obligation.instructionExpiresAt,
    proofHash = obligation.proof?.proofHash,
    statedAmountKobo = obligation.proof?.statedAmount?.kobo,
    bankReference = obligation.proof?.bankReference,
    submittedBy = obligation.proof?.submittedBy?.value,
    proofAt = obligation.proof?.at,
    attestedBy = obligation.attestation?.attestedBy?.value,
    secondAttestor = obligation.attestation?.secondAttestor?.value,
    statementRef = obligation.attestation?.statementRef,
    attestedAt = obligation.attestation?.at,
    waivedBy = obligation.waiver?.by?.value,
    waiverNote = obligation.waiver?.note,
    waivedAt = obligation.waiver?.at,
    exception = obligation.exceptionCode,
    revision = obligation.revision,
)
