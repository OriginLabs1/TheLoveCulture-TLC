package culture.thelove.tlc.domain

/**
 * SK-04 / INV-04: Complementary Stewardship, role-based and symmetric.
 *
 * Two members own one space. Structural authority (Primary) and full visibility with veto (Co)
 * are distinct *roles*, chosen by the couple — never assigned by gender, age or default.
 * Irreversible actions need both signatures; either member can veto; no single party, and no
 * link, can authorise them alone.
 */
public enum class SensitiveAction {
    CHANGE_BANK_DETAILS,
    DISSOLVE_DYAD,
    GRANT_DELEGATION,
    REVOKE_DELEGATION,
    EXPORT_SHARED_RECORD,
}

public data class Dyad(
    public val id: String,
    public val primary: PartyId,
    public val co: PartyId,
) {
    init {
        require(id.isNotBlank()) { "Dyad id must not be blank." }
        require(primary != co) { "A dyad needs two distinct people." }
    }

    public val members: Set<PartyId> get() = setOf(primary, co)

    public fun isMember(party: PartyId): Boolean = party in members
}

public sealed interface DyadDecision {
    public data object Approved : DyadDecision

    public data class Pending(public val missing: Set<PartyId>) : DyadDecision

    /** Any single member can stop an irreversible action. Veto is symmetric. */
    public data class Blocked(public val by: PartyId) : DyadDecision
}

public object Stewardship {

    /**
     * @param approvals members who signed
     * @param vetoes    members who objected
     * Deterministic and total: every combination yields exactly one decision.
     */
    public fun decide(
        dyad: Dyad,
        action: SensitiveAction,
        approvals: Set<PartyId>,
        vetoes: Set<PartyId> = emptySet(),
    ): DyadDecision {
        val foreign = (approvals + vetoes).filterNot(dyad::isMember)
        if (foreign.isNotEmpty()) return DyadDecision.Blocked(foreign.first())
        vetoes.firstOrNull()?.let { return DyadDecision.Blocked(it) }
        if (!actionsRequiringBothSignatures(action)) {
            return if (approvals.isNotEmpty()) DyadDecision.Approved else DyadDecision.Pending(dyad.members)
        }
        val missing = dyad.members - approvals
        return if (missing.isEmpty()) DyadDecision.Approved else DyadDecision.Pending(missing)
    }

    /** v0.0.1: every listed action is irreversible enough to need two keys. */
    public fun actionsRequiringBothSignatures(action: SensitiveAction): Boolean = when (action) {
        SensitiveAction.CHANGE_BANK_DETAILS,
        SensitiveAction.DISSOLVE_DYAD,
        SensitiveAction.GRANT_DELEGATION,
        SensitiveAction.REVOKE_DELEGATION,
        SensitiveAction.EXPORT_SHARED_RECORD,
        -> true
    }
}

/** Receptive Mode: explicit, opt-in permission to receive community support. */
public enum class ReceptiveMode { OFF, ON }

public sealed interface SupportGate {
    public data object Allowed : SupportGate

    public data class Refused(public val reason: Rejection) : SupportGate
}

/**
 * INV-01/INV-02: the gate only decides whether an expression of support may be *offered*.
 * It never authorises a transfer and never implies that TLC will move value.
 */
public object SupportPolicy {

    public fun gate(
        recipientRole: Role,
        mode: ReceptiveMode,
        from: PartyId,
        to: PartyId,
        kind: TokenKind,
    ): SupportGate {
        if (from == to) return SupportGate.Refused(Rejection.WRONG_ACTOR)
        if (kind !in TokenKind.community) return SupportGate.Refused(Rejection.WRONG_ACTOR)
        if (kind == TokenKind.GROWTH_GRANT) {
            // Growth Grants are witnessed pledges, not free support: they bypass Receptive Mode
            // but still require an explicit acceptance by the recipient elsewhere.
            return SupportGate.Allowed
        }
        return when (recipientRole) {
            Role.SINGLE -> if (mode == ReceptiveMode.ON) {
                SupportGate.Allowed
            } else {
                SupportGate.Refused(Rejection.WRONG_ACTOR)
            }
            Role.I_PARTNER, Role.LOVE_MODEL -> SupportGate.Allowed
        }
    }
}

/** Doctrine constants that the shells must render verbatim (CONTEXT §4). */
public object DoctrineCopy {
    public const val FINANCIAL_NOTICE: String =
        "TLC facilitates direct peer-to-peer support. It does not hold, pool, escrow or move money. " +
            "Activation, Reactivation, Courtesy and Joy are sent directly to the Concept Pioneer's " +
            "own account and sustain the platform. Supportive Tokens, Growth Grants and Recognition " +
            "are sent directly to the intended recipient. TLC Tokens are recognition signals: they " +
            "have no cash value, are not transferable, not redeemable, and are not an investment or " +
            "cryptocurrency."

    /** Words that imply custody, yield or a payment institution. Banned in UI and in code. */
    public val BANNED_TERMS: Set<String> = setOf(
        "escrow", "wallet", "balance", "payout", "yield", "invest", "profit", "dividend",
        "refund", "deposit", "withdraw", "top up", "topup", "credit",
    )
}
