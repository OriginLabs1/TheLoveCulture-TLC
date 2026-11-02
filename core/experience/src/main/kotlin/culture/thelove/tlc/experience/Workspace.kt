package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Money
import culture.thelove.tlc.domain.PartyId
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.Stewardship
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.domain.Dyad
import culture.thelove.tlc.domain.DyadDecision
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.LedgerDraft

/**
 * The shell-facing use cases of the experience.
 *
 * Every operation is a pure function from the current [AppSnapshot] to a new one plus the
 * [LedgerDraft]s that make the change traceable. The shell writes the facts and then the state;
 * it never re-implements a rule. That keeps the screens honest and makes every flow testable
 * without Android (SK-01, INV-08).
 */
public sealed interface WorkspaceResult {
    public data class Ok(public val change: WorkspaceChange) : WorkspaceResult

    public data class Rejected(public val reason: String) : WorkspaceResult
}

public data class WorkspaceChange(
    public val snapshot: AppSnapshot,
    public val facts: List<LedgerDraft> = emptyList(),
    public val notice: String? = null,
)

public object Workspace {

    public const val PIONEER_DETAILS_NOTE: String =
        "Pioneer bank details are not stored in this build. When the secure fee flow ships, they " +
            "are released only to the payer, only after the due exists, and expire after " +
            "INSTRUCTION_TTL_MILLIS. The reference below is yours to keep; the transfer itself " +
            "happens in your own banking app."

    public fun onboard(
        displayName: String,
        circleName: String,
        role: Role,
        recordId: String,
        now: Long,
    ): WorkspaceResult = guarded {
        val profile = Profile(
            displayName = displayName.trim(),
            circleName = circleName.trim().ifBlank { "My circle" },
            role = role,
            recordId = recordId,
            createdAt = now,
        )
        val snapshot = AppSnapshot(
            profile = profile,
            preferences = Preferences(onboarded = true),
            stewards = listOf(Steward(profile.recordId, profile.displayName, StewardRole.PRIMARY)),
        )
        WorkspaceChange(
            snapshot = snapshot,
            facts = listOf(
                fact(
                    kind = EntryKind.CONSENT_GRANTED,
                    subject = "Joined TLC as ${roleName(role)} · consent recorded on this device",
                    at = now,
                    key = "onboard:${profile.recordId}",
                ),
            ),
            notice = "Your circle is ready. The record is yours; nothing leaves this device.",
        )
    }

    public fun updatePreferences(snapshot: AppSnapshot, preferences: Preferences): WorkspaceResult =
        WorkspaceResult.Ok(WorkspaceChange(snapshot.copy(preferences = preferences)))

    public fun addCoSteward(snapshot: AppSnapshot, id: String, name: String): WorkspaceResult {
        if (snapshot.coSteward != null) {
            return WorkspaceResult.Rejected("This circle already has a Co-Steward.")
        }
        val clean = name.trim()
        if (clean.isBlank()) return WorkspaceResult.Rejected("Name the Co-Steward.")
        if (id == snapshot.primary.id) return WorkspaceResult.Rejected("A person cannot hold both seats.")
        return guarded {
            val steward = Steward(id = id, name = clean, role = StewardRole.CO)
            WorkspaceChange(
                snapshot = snapshot.copy(stewards = snapshot.stewards + steward),
                facts = listOf(
                    fact(
                        kind = EntryKind.CONSENT_GRANTED,
                        subject = "Co-Steward named: $clean · two-key decisions are now available",
                        at = System.currentTimeMillis(),
                        key = "co-steward:$id",
                    ),
                ),
                notice = "Two-key decisions are now available on this device.",
            )
        }
    }

    public fun propose(
        snapshot: AppSnapshot,
        id: String,
        action: SensitiveAction,
        summary: String?,
        now: Long,
    ): WorkspaceResult {
        if (snapshot.proposals.any { it.id == id }) {
            return WorkspaceResult.Rejected("That decision already exists.")
        }
        return guarded {
            val proposal = Proposal(
                id = id,
                action = action,
                summary = summary?.trim()?.ifBlank { defaultSummary(action) } ?: defaultSummary(action),
                createdAt = now,
            )
            WorkspaceChange(
                snapshot = snapshot.copy(proposals = listOf(proposal) + snapshot.proposals),
                notice = "Decision opened. It needs both signatures; either steward can veto.",
            )
        }
    }

    public fun approve(snapshot: AppSnapshot, proposalId: String, actorId: String, now: Long): WorkspaceResult {
        val proposal = snapshot.proposals.firstOrNull { it.id == proposalId }
            ?: return WorkspaceResult.Rejected("That decision is no longer open.")
        if (proposal.closed) return WorkspaceResult.Rejected("That decision is already closed.")
        if (snapshot.stewards.none { it.id == actorId }) {
            return WorkspaceResult.Rejected("Only a steward of this circle can sign.")
        }
        if (actorId in proposal.approvals) {
            return WorkspaceResult.Ok(WorkspaceChange(snapshot, notice = "Your signature is already on it."))
        }
        val dyad = dyadOf(snapshot)
            ?: return WorkspaceResult.Rejected("Name a Co-Steward before opening a two-key decision.")

        val approvals = (proposal.approvals + actorId).map(::PartyId).toSet()
        val vetoes = proposal.vetoes.map(::PartyId).toSet()
        return when (Stewardship.decide(dyad, proposal.action, approvals, vetoes)) {
            is DyadDecision.Approved -> {
                val closed = proposal.copy(approvals = proposal.approvals + actorId, closed = true)
                WorkspaceResult.Ok(
                    WorkspaceChange(
                        snapshot = snapshot.copy(proposals = snapshot.proposals.replace(closed)),
                        facts = listOf(
                            fact(
                                kind = EntryKind.CONSENT_GRANTED,
                                subject = "Two-key decision approved: ${closed.summary}",
                                at = now,
                                key = "proposal:${closed.id}:approved",
                            ),
                        ),
                        notice = "Both signatures recorded. The decision is closed.",
                    ),
                )
            }
            is DyadDecision.Pending -> WorkspaceResult.Ok(
                WorkspaceChange(
                    snapshot = snapshot.copy(
                        proposals = snapshot.proposals.replace(proposal.copy(approvals = proposal.approvals + actorId)),
                    ),
                    notice = "One signature recorded. The other steward must sign.",
                ),
            )
            is DyadDecision.Blocked -> WorkspaceResult.Rejected("The other steward has vetoed this decision.")
        }
    }

    public fun veto(snapshot: AppSnapshot, proposalId: String, actorId: String, now: Long): WorkspaceResult {
        val proposal = snapshot.proposals.firstOrNull { it.id == proposalId }
            ?: return WorkspaceResult.Rejected("That decision is no longer open.")
        if (proposal.closed) return WorkspaceResult.Rejected("That decision is already closed.")
        if (snapshot.stewards.none { it.id == actorId }) {
            return WorkspaceResult.Rejected("Only a steward of this circle can veto.")
        }
        val closed = proposal.copy(vetoes = proposal.vetoes + actorId, closed = true)
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(proposals = snapshot.proposals.replace(closed)),
                facts = listOf(
                    fact(
                        kind = EntryKind.CONSENT_REVOKED,
                        subject = "Veto recorded: ${closed.summary}",
                        at = now,
                        key = "proposal:${closed.id}:vetoed",
                    ),
                ),
                notice = "Veto recorded. A veto stops an irreversible action immediately.",
            ),
        )
    }

    public fun addGrant(
        snapshot: AppSnapshot,
        id: String,
        beneficiary: String,
        purpose: String,
        amountKobo: Long?,
        now: Long,
    ): WorkspaceResult {
        if (amountKobo != null && amountKobo <= 0) {
            return WorkspaceResult.Rejected("A stated amount must be greater than zero.")
        }
        return guarded {
            val grant = GrowthGrant(
                id = id,
                beneficiary = beneficiary.trim(),
                purpose = purpose.trim(),
                amountKobo = amountKobo,
                pledgedAt = now,
            )
            val money = amountKobo?.let { " · ${Money(it).render()}" }.orEmpty()
            WorkspaceChange(
                snapshot = snapshot.copy(grants = listOf(grant) + snapshot.grants),
                facts = listOf(
                    fact(
                        kind = EntryKind.GRANT_PLEDGE,
                        subject = "Pledge to ${grant.beneficiary}: ${grant.purpose}$money",
                        amountKobo = amountKobo,
                        at = now,
                        key = "grant:${grant.id}:pledge",
                    ),
                ),
                notice = "Pledge recorded. The money still moves directly from you to the beneficiary.",
            )
        }
    }

    public fun advanceGrant(snapshot: AppSnapshot, id: String, event: GrantEvent): WorkspaceResult {
        val grant = snapshot.grants.firstOrNull { it.id == id }
            ?: return WorkspaceResult.Rejected("That grant is no longer here.")
        return when (val transition = GrantLifecycle.apply(grant, event)) {
            is GrantTransition.Rejected -> WorkspaceResult.Rejected(transition.reason)
            is GrantTransition.Applied -> {
                val next = transition.next
                val facts = when (event) {
                    is GrantEvent.AttachEvidence -> listOf(
                        fact(
                            kind = EntryKind.GRANT_EVIDENCE,
                            subject = "Evidence for ${next.beneficiary}: ${next.evidence.orEmpty()}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "grant:${next.id}:evidence",
                        ),
                    )
                    is GrantEvent.Verify -> listOf(
                        fact(
                            kind = EntryKind.MILESTONE_VERIFIED,
                            subject = "Milestone verified by ${next.witness}: ${next.purpose}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "grant:${next.id}:verified",
                        ),
                    )
                    is GrantEvent.RecordPayment -> listOf(
                        fact(
                            kind = EntryKind.PROOF_RECORDED,
                            subject = "Direct payment recorded (${next.bankReference}): ${next.purpose}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "grant:${next.id}:paid",
                        ),
                    )
                    is GrantEvent.Withdraw -> listOf(
                        fact(
                            kind = EntryKind.WAIVER,
                            subject = "Pledge withdrawn: ${next.purpose} · ${next.withdrawnReason.orEmpty()}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "grant:${next.id}:withdrawn",
                        ),
                    )
                }
                WorkspaceResult.Ok(
                    WorkspaceChange(
                        snapshot = snapshot.copy(grants = snapshot.grants.replace(next)),
                        facts = facts,
                        notice = grantNotice(next.stage),
                    ),
                )
            }
        }
    }

    public fun addDue(
        snapshot: AppSnapshot,
        id: String,
        kind: TokenKind,
        amountKobo: Long,
        period: FeePeriod,
        now: Long,
    ): WorkspaceResult {
        if (!kind.pioneerFee) return WorkspaceResult.Rejected("Only platform tokens create a Pioneer due.")
        if (snapshot.stewards.size != 2) {
            return WorkspaceResult.Rejected("A Pioneer due belongs to a dyad. Name a Co-Steward first.")
        }
        if (amountKobo <= 0) return WorkspaceResult.Rejected("A due must be greater than zero.")
        return guarded {
            val due = Dues.create(
                id = id,
                kind = kind,
                amountKobo = amountKobo,
                period = period,
                members = snapshot.stewards.map { it.id },
                now = now,
            )
            WorkspaceChange(
                snapshot = snapshot.copy(dues = listOf(due) + snapshot.dues),
                facts = listOf(
                    fact(
                        kind = EntryKind.FEE_INTENT,
                        subject = "${kindLabel(kind)} due recorded · ${Money(amountKobo).render()}",
                        amountKobo = amountKobo,
                        at = now,
                        key = "due:${due.id}:intent",
                    ),
                ),
                notice = "Due recorded as an intention. No money has moved and none is held by TLC.",
            )
        }
    }

    public fun advanceDue(snapshot: AppSnapshot, id: String, event: FeeEvent): WorkspaceResult {
        val due = snapshot.dues.firstOrNull { it.id == id }
            ?: return WorkspaceResult.Rejected("That due is no longer here.")
        return when (val transition = Dues.apply(due, event)) {
            is DueTransition.Rejected -> WorkspaceResult.Rejected(transition.reason.name)
            is DueTransition.Applied -> {
                val next = transition.next
                val facts = when (next.state) {
                    culture.thelove.tlc.domain.ObligationState.PROOF_SUBMITTED -> listOf(
                        fact(
                            kind = EntryKind.PROOF_RECORDED,
                            subject = "${kindLabel(next.kind)} proof recorded · ${next.bankReference.orEmpty()}",
                            amountKobo = next.statedAmountKobo ?: next.amountKobo,
                            at = event.at,
                            key = "due:${next.id}:proof:${next.proofHash}",
                        ),
                    )
                    culture.thelove.tlc.domain.ObligationState.ATTESTED -> listOf(
                        fact(
                            kind = EntryKind.ATTESTATION,
                            subject = "${kindLabel(next.kind)} attested · ${next.statementRef.orEmpty()}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "due:${next.id}:attested",
                        ),
                        fact(
                            kind = EntryKind.RECEIPT,
                            subject = "Receipt for ${kindLabel(next.kind)} due · ${Money(next.amountKobo).render()}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "due:${next.id}:receipt",
                        ),
                    )
                    culture.thelove.tlc.domain.ObligationState.WAIVED -> listOf(
                        fact(
                            kind = EntryKind.WAIVER,
                            subject = "${kindLabel(next.kind)} waived · ${next.waiverNote.orEmpty()}",
                            amountKobo = next.amountKobo,
                            at = event.at,
                            key = "due:${next.id}:waived",
                        ),
                    )
                    else -> emptyList()
                }
                WorkspaceResult.Ok(
                    WorkspaceChange(
                        snapshot = snapshot.copy(dues = snapshot.dues.replace(next)),
                        facts = facts,
                        notice = dueNotice(next),
                    ),
                )
            }
        }
    }

    public fun defaultSummary(action: SensitiveAction): String = when (action) {
        SensitiveAction.CHANGE_BANK_DETAILS -> "Change the verified transfer details"
        SensitiveAction.DISSOLVE_DYAD -> "Dissolve this circle"
        SensitiveAction.GRANT_DELEGATION -> "Delegate a scoped steward role"
        SensitiveAction.REVOKE_DELEGATION -> "Revoke a delegated steward role"
        SensitiveAction.EXPORT_SHARED_RECORD -> "Export the shared record"
    }

    private fun dyadOf(snapshot: AppSnapshot): Dyad? {
        val co = snapshot.coSteward ?: return null
        return Dyad("this-circle", PartyId(snapshot.primary.id), PartyId(co.id))
    }

    private fun List<Proposal>.replace(item: Proposal): List<Proposal> =
        map { if (it.id == item.id) item else it }

    private fun List<GrowthGrant>.replace(item: GrowthGrant): List<GrowthGrant> =
        map { if (it.id == item.id) item else it }

    private fun List<DueRecord>.replace(item: DueRecord): List<DueRecord> =
        map { if (it.id == item.id) item else it }

    private fun fact(
        kind: EntryKind,
        subject: String,
        at: Long,
        key: String,
        amountKobo: Long? = null,
    ): LedgerDraft = LedgerDraft(
        kind = kind,
        subject = subject,
        amountKobo = amountKobo,
        occurredAt = at,
        idempotencyKey = key,
    )

    private inline fun guarded(block: () -> WorkspaceChange): WorkspaceResult =
        try {
            WorkspaceResult.Ok(block())
        } catch (e: IllegalArgumentException) {
            WorkspaceResult.Rejected(e.message ?: "That change is not valid.")
        }

    private fun roleName(role: Role): String = when (role) {
        Role.SINGLE -> "Single"
        Role.I_PARTNER -> "I-Partner"
        Role.LOVE_MODEL -> "Love Model"
    }

    private fun kindLabel(kind: TokenKind): String = when (kind) {
        TokenKind.SUPPORTIVE -> "Supportive"
        TokenKind.RECOGNITION -> "Recognition"
        TokenKind.GROWTH_GRANT -> "Growth Grant"
        TokenKind.ACTIVATION -> "Activation"
        TokenKind.REACTIVATION -> "Reactivation"
        TokenKind.COURTESY -> "Courtesy"
        TokenKind.JOY -> "Joy"
    }

    private fun grantNotice(stage: GrantStage): String = when (stage) {
        GrantStage.PLEDGED -> "Pledge recorded."
        GrantStage.EVIDENCE_RECORDED -> "Evidence recorded. A witness can now verify the milestone."
        GrantStage.VERIFIED -> "Milestone verified. Pay the beneficiary directly, then record the reference."
        GrantStage.PAID -> "Direct payment recorded. The grant is complete."
        GrantStage.WITHDRAWN -> "Pledge withdrawn cleanly. No debt is created."
    }

    private fun dueNotice(due: DueRecord): String = when (due.state) {
        culture.thelove.tlc.domain.ObligationState.DUE -> "Due is open."
        culture.thelove.tlc.domain.ObligationState.INSTRUCTION_ISSUED ->
            "Instruction released for this session. Transfer directly from your own bank."
        culture.thelove.tlc.domain.ObligationState.PROOF_SUBMITTED ->
            "Proof recorded. It is a claim until the Concept Pioneer attests settlement."
        culture.thelove.tlc.domain.ObligationState.ATTESTED ->
            "Attested. A receipt is now part of the record."
        culture.thelove.tlc.domain.ObligationState.EXCEPTION ->
            "The amounts do not match. An exception was raised; nothing was settled."
        culture.thelove.tlc.domain.ObligationState.WAIVED -> "Waived. No receivable was created."
        culture.thelove.tlc.domain.ObligationState.EXPIRED -> "This due has expired cleanly."
    }
}
