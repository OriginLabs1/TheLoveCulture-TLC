package culture.thelove.tlc.experience

/**
 * The Growth Grant lifecycle, as a total function over (stage, event).
 *
 * The machine records *claims*: a pledge was made, evidence was attached, a witness verified the
 * milestone, a payment was recorded. It never moves value, and it never awards a stage that the
 * evidence does not support (INV-01, INV-02, INV-08).
 */
public sealed interface GrantEvent {
    public val at: Long

    public data class AttachEvidence(public val evidence: String, override val at: Long) : GrantEvent

    public data class Verify(public val witness: String, override val at: Long) : GrantEvent

    public data class RecordPayment(public val bankReference: String, override val at: Long) : GrantEvent

    public data class Withdraw(public val reason: String, override val at: Long) : GrantEvent
}

public sealed interface GrantTransition {
    public data class Applied(public val next: GrowthGrant) : GrantTransition

    public data class Rejected(public val reason: String) : GrantTransition
}

public object GrantLifecycle {

    public fun apply(grant: GrowthGrant, event: GrantEvent): GrantTransition {
        if (event.at <= 0) return GrantTransition.Rejected("A step needs a real time.")
        return when (event) {
            is GrantEvent.AttachEvidence -> attach(grant, event)
            is GrantEvent.Verify -> verify(grant, event)
            is GrantEvent.RecordPayment -> pay(grant, event)
            is GrantEvent.Withdraw -> withdraw(grant, event)
        }
    }

    private fun attach(grant: GrowthGrant, event: GrantEvent.AttachEvidence): GrantTransition {
        if (grant.stage != GrantStage.PLEDGED) {
            return GrantTransition.Rejected("Evidence can only be added to a pledge.")
        }
        val evidence = event.evidence.trim()
        if (evidence.isEmpty()) return GrantTransition.Rejected("Describe the evidence you have.")
        if (evidence.length > GrowthGrant.MAX_EVIDENCE) {
            return GrantTransition.Rejected("That evidence note is too long for one entry.")
        }
        return GrantTransition.Applied(
            grant.copy(stage = GrantStage.EVIDENCE_RECORDED, evidence = evidence),
        )
    }

    private fun verify(grant: GrowthGrant, event: GrantEvent.Verify): GrantTransition {
        if (grant.stage != GrantStage.EVIDENCE_RECORDED) {
            return GrantTransition.Rejected("Attach evidence before asking for verification.")
        }
        val witness = event.witness.trim()
        if (witness.isEmpty()) return GrantTransition.Rejected("Name who verified the milestone.")
        if (witness.length > GrowthGrant.MAX_BENEFICIARY) {
            return GrantTransition.Rejected("That witness name is too long.")
        }
        return GrantTransition.Applied(
            grant.copy(
                stage = GrantStage.VERIFIED,
                witness = witness,
                verifiedAt = event.at,
            ),
        )
    }

    private fun pay(grant: GrowthGrant, event: GrantEvent.RecordPayment): GrantTransition {
        if (grant.stage != GrantStage.VERIFIED) {
            return GrantTransition.Rejected("A payment is recorded only after the milestone is verified.")
        }
        val reference = event.bankReference.trim()
        if (reference.isEmpty()) return GrantTransition.Rejected("Record the bank reference you have.")
        if (reference.length > GrowthGrant.MAX_REFERENCE) {
            return GrantTransition.Rejected("That reference is too long for one entry.")
        }
        return GrantTransition.Applied(
            grant.copy(
                stage = GrantStage.PAID,
                bankReference = reference,
                paidAt = event.at,
            ),
        )
    }

    private fun withdraw(grant: GrowthGrant, event: GrantEvent.Withdraw): GrantTransition {
        if (grant.stage == GrantStage.PAID) {
            return GrantTransition.Rejected("A paid grant is a receipt; it cannot be withdrawn.")
        }
        if (grant.stage == GrantStage.WITHDRAWN) {
            return GrantTransition.Rejected("This grant is already withdrawn.")
        }
        val reason = event.reason.trim()
        if (reason.isEmpty()) return GrantTransition.Rejected("Say why the pledge is withdrawn.")
        if (reason.length > GrowthGrant.MAX_PURPOSE) {
            return GrantTransition.Rejected("That reason is too long for one entry.")
        }
        return GrantTransition.Applied(
            grant.copy(
                stage = GrantStage.WITHDRAWN,
                withdrawnAt = event.at,
                withdrawnReason = reason,
            ),
        )
    }
}
