package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.BankAccountRef
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Handle
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
        handle: Handle? = null,
    ): WorkspaceResult {
        val cleanName = displayName.trim()
        val claimed = handle ?: Handle.suggest(cleanName, recordId)
        if (Handle.isReserved(claimed.value)) {
            return WorkspaceResult.Rejected("That handle belongs to TLC and cannot be claimed.")
        }
        return guarded {
        val profile = Profile(
            displayName = cleanName,
            circleName = circleName.trim().ifBlank { "My circle" },
            role = role,
            recordId = recordId,
            createdAt = now,
            handle = claimed,
        )
        val snapshot = AppSnapshot(
            profile = profile,
            preferences = Preferences(onboarded = true),
            stewards = listOf(Steward(profile.recordId, profile.displayName, StewardRole.PRIMARY, claimed)),
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
                fact(
                    kind = EntryKind.HANDLE_CLAIMED,
                    subject = "Known here as ${claimed.value}",
                    at = now,
                    key = "handle:${profile.recordId}:${claimed.value}",
                ),
            ),
            notice = "Your circle is ready. The record is yours; nothing leaves this device.",
        )
        }
    }

    /** Claim a new address. Reserved addresses stay with TLC, and no two people share one. */
    public fun claimHandle(snapshot: AppSnapshot, handle: Handle, now: Long): WorkspaceResult {
        if (Handle.isReserved(handle.value)) {
            return WorkspaceResult.Rejected("That handle belongs to TLC and cannot be claimed.")
        }
        if (handle == snapshot.profile.handle) {
            return WorkspaceResult.Ok(WorkspaceChange(snapshot, notice = "That is already your handle."))
        }
        val takenByPerson = snapshot.people.any { it.handle == handle }
        val takenBySteward = snapshot.stewards.any { it.handle == handle }
        if (takenByPerson || takenBySteward) {
            return WorkspaceResult.Rejected("That handle is already used in this circle.")
        }
        return guarded {
            val profile = snapshot.profile.copy(handle = handle)
            val stewards = snapshot.stewards.map { steward ->
                if (steward.role == StewardRole.PRIMARY) steward.copy(handle = handle) else steward
            }
            WorkspaceChange(
                snapshot = snapshot.copy(profile = profile, stewards = stewards),
                facts = listOf(
                    fact(
                        kind = EntryKind.HANDLE_CLAIMED,
                        subject = "Now known as ${handle.value}",
                        at = now,
                        key = "handle:${snapshot.profile.recordId}:${handle.value}:${now}",
                    ),
                ),
                notice = "Your handle is ${handle.value}.",
            )
        }
    }

    public fun updatePreferences(snapshot: AppSnapshot, preferences: Preferences): WorkspaceResult =
        WorkspaceResult.Ok(WorkspaceChange(snapshot.copy(preferences = preferences)))

    /**
     * Update the person's own profile. [photoRef] is the current file name (or `null` to remove
     * the photo); the caller owns the file and must not change state until the file is written.
     */
    public fun updateProfile(
        snapshot: AppSnapshot,
        displayName: String,
        circleName: String,
        photoRef: String?,
        now: Long,
    ): WorkspaceResult {
        val cleanName = displayName.trim()
        if (cleanName.isBlank()) return WorkspaceResult.Rejected("A display name cannot be empty.")
        return guarded {
            val profile = snapshot.profile.copy(
                displayName = cleanName,
                circleName = circleName.trim().ifBlank { snapshot.profile.circleName },
                photoRef = photoRef,
            )
            if (profile == snapshot.profile) {
                return@guarded WorkspaceChange(snapshot, notice = "Nothing changed.")
            }
            val stewards = snapshot.stewards.map { steward ->
                if (steward.role == StewardRole.PRIMARY) steward.copy(name = cleanName) else steward
            }
            val photoChanged = profile.photoRef != snapshot.profile.photoRef
            val details = buildString {
                append("Profile updated")
                if (profile.displayName != snapshot.profile.displayName) append(" · name")
                if (profile.circleName != snapshot.profile.circleName) append(" · circle")
                if (photoChanged) append(if (profile.photoRef == null) " · photo removed" else " · photo")
            }
            WorkspaceChange(
                snapshot = snapshot.copy(profile = profile, stewards = stewards),
                facts = listOf(
                    fact(
                        kind = EntryKind.PROFILE_UPDATED,
                        subject = details,
                        at = now,
                        key = "profile:${snapshot.profile.recordId}:updated:$now",
                    ),
                ),
                notice = "Your profile is saved on this device.",
            )
        }
    }

    /**
     * Attach (or replace) the one current receipt image for a token. Replacing keeps the same
     * subject but a new id, so the timeline shows that the evidence changed.
     */
    public fun attachReceipt(
        snapshot: AppSnapshot,
        receipt: TokenReceipt,
        now: Long,
    ): WorkspaceResult {
        val subjectExists = when (receipt.subjectKind) {
            ReceiptSubject.SUPPORT -> snapshot.supports.any { it.id == receipt.subjectId }
            ReceiptSubject.DUE -> snapshot.dues.any { it.id == receipt.subjectId }
            ReceiptSubject.GRANT -> snapshot.grants.any { it.id == receipt.subjectId }
            ReceiptSubject.RECORD -> true
        }
        if (!subjectExists) return WorkspaceResult.Rejected("That token is no longer here.")
        return guarded {
            val remaining = snapshot.receipts.filterNot {
                it.subjectKind == receipt.subjectKind && it.subjectId == receipt.subjectId
            }
            WorkspaceChange(
                snapshot = snapshot.copy(receipts = remaining + receipt),
                facts = listOf(
                    fact(
                        kind = EntryKind.RECEIPT_ADDED,
                        subject = "Receipt image attached · ${audienceLabel(receipt.audience)}",
                        at = now,
                        key = "receipt:${receipt.id}:added",
                    ),
                ),
                notice = "Receipt image saved on this device.",
            )
        }
    }

    public fun removeReceipt(snapshot: AppSnapshot, receiptId: String): WorkspaceResult {
        val receipt = snapshot.receipts.firstOrNull { it.id == receiptId }
            ?: return WorkspaceResult.Rejected("That receipt is no longer here.")
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(receipts = snapshot.receipts.filterNot { it.id == receiptId }),
                facts = listOf(
                    fact(
                        kind = EntryKind.RECEIPT_REMOVED,
                        subject = "Receipt image removed",
                        at = System.currentTimeMillis(),
                        key = "receipt:${receipt.id}:removed",
                    ),
                ),
                notice = "Receipt image removed. The record keeps only the fact.",
            ),
        )
    }

    public fun setReceiptAudience(
        snapshot: AppSnapshot,
        receiptId: String,
        audience: ReceiptAudience,
    ): WorkspaceResult {
        val receipt = snapshot.receipts.firstOrNull { it.id == receiptId }
            ?: return WorkspaceResult.Rejected("That receipt is no longer here.")
        if (receipt.audience == audience) {
            return WorkspaceResult.Ok(WorkspaceChange(snapshot, notice = "That audience is already set."))
        }
        val next = receipt.copy(audience = audience)
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(
                    receipts = snapshot.receipts.map { if (it.id == receiptId) next else it },
                ),
                facts = listOf(
                    fact(
                        kind = EntryKind.RECEIPT_AUDIENCE_CHANGED,
                        subject = "Receipt audience: ${audienceLabel(audience)}",
                        at = System.currentTimeMillis(),
                        key = "receipt:${receipt.id}:audience:${audience.name}",
                    ),
                ),
                notice = if (audience.isShared) {
                    "This receipt can now be seen by ${audienceLabel(audience).lowercase()}."
                } else {
                    "This receipt is private to you again."
                },
            ),
        )
    }

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

    // -----------------------------------------------------------------------------------------
    // Trust & Contact (v0.0.5). Addresses, people, the encrypted bank vault, Support and the
    // local slice of the Contact & Consent Framework. Every operation is still a pure rule.
    // -----------------------------------------------------------------------------------------

    public fun addPerson(
        snapshot: AppSnapshot,
        id: String,
        handle: Handle,
        displayName: String,
        role: Role,
        now: Long,
        bankRefId: String? = null,
    ): WorkspaceResult {
        if (Handle.isReserved(handle.value)) {
            return WorkspaceResult.Rejected("That handle belongs to TLC and cannot be given to a person.")
        }
        if (handle == snapshot.profile.handle || snapshot.stewards.any { it.handle == handle }) {
            return WorkspaceResult.Rejected("That handle is already used in this circle.")
        }
        if (snapshot.people.any { it.handle == handle }) {
            return WorkspaceResult.Rejected("Someone in this circle already answers to ${handle.value}.")
        }
        if (snapshot.people.any { it.id == id }) {
            return WorkspaceResult.Rejected("That person already exists.")
        }
        if (bankRefId != null && snapshot.bankAccounts.none { it.id == bankRefId }) {
            return WorkspaceResult.Rejected("Those transfer details are not in the vault.")
        }
        return guarded {
            val person = Person(
                id = id,
                handle = handle,
                displayName = displayName.trim(),
                role = role,
                bankRefId = bankRefId,
                receptive = snapshot.preferences.receptiveMode,
                createdAt = now,
            )
            WorkspaceChange(
                snapshot = snapshot.copy(people = listOf(person) + snapshot.people),
                facts = listOf(
                    fact(
                        kind = EntryKind.PERSON_ADDED,
                        subject = "Added ${person.displayName} (${person.handle.value}) as ${person.badge}",
                        at = now,
                        key = "person:${person.id}:added",
                    ),
                ),
                notice = "${person.displayName} is now in your circle.",
            )
        }
    }

    public fun attachBank(snapshot: AppSnapshot, personId: String, bankRefId: String?): WorkspaceResult {
        val person = snapshot.people.firstOrNull { it.id == personId }
            ?: return WorkspaceResult.Rejected("That person is no longer here.")
        if (bankRefId != null && snapshot.bankAccounts.none { it.id == bankRefId }) {
            return WorkspaceResult.Rejected("Those transfer details are not in the vault.")
        }
        val updated = person.copy(bankRefId = bankRefId)
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(people = snapshot.people.replacePerson(updated)),
                notice = if (bankRefId == null) {
                    "Transfer details detached from ${person.displayName}."
                } else {
                    "Transfer details will be suggested for ${person.displayName}."
                },
            ),
        )
    }

    public fun removePerson(snapshot: AppSnapshot, personId: String): WorkspaceResult {
        if (snapshot.people.none { it.id == personId }) {
            return WorkspaceResult.Rejected("That person is no longer here.")
        }
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(people = snapshot.people.filterNot { it.id == personId }),
                notice = "Person removed from this circle. The record keeps its history.",
            ),
        )
    }

    /**
     * Save transfer details. [account] holds the full number, but only its [BankAccountRef] — bank,
     * last four digits, holder and a non-reversible fingerprint — enters app state or the record.
     */
    public fun saveBankAccount(
        snapshot: AppSnapshot,
        id: String,
        account: BankAccount,
        now: Long,
    ): WorkspaceResult {
        val ref = account.ref(id, now)
        if (snapshot.bankAccounts.any { it.id == id }) {
            return WorkspaceResult.Rejected("That vault entry already exists.")
        }
        if (snapshot.bankAccounts.any { it.fingerprint == ref.fingerprint }) {
            return WorkspaceResult.Rejected("Those transfer details are already saved.")
        }
        return guarded {
            WorkspaceChange(
                snapshot = snapshot.copy(bankAccounts = listOf(ref) + snapshot.bankAccounts),
                facts = listOf(
                    fact(
                        kind = EntryKind.BANK_DETAILS_SAVED,
                        subject = "Transfer details saved: ${ref.bankName} ····${ref.last4} · ${ref.holderName}",
                        at = now,
                        key = "bank:${ref.id}:saved",
                    ),
                ),
                notice = "Saved. The full number stays in the encrypted vault.",
            )
        }
    }

    public fun removeBankAccount(snapshot: AppSnapshot, refId: String): WorkspaceResult {
        if (snapshot.bankAccounts.none { it.id == refId }) {
            return WorkspaceResult.Rejected("Those transfer details are no longer here.")
        }
        val people = snapshot.people.map { person ->
            if (person.bankRefId == refId) person.copy(bankRefId = null) else person
        }
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(
                    bankAccounts = snapshot.bankAccounts.filterNot { it.id == refId },
                    people = people,
                ),
                notice = "Transfer details removed from this device.",
            ),
        )
    }

    /** Record that details were shown on purpose. The number itself never enters the entry. */
    public fun markBankRevealed(snapshot: AppSnapshot, refId: String, now: Long): WorkspaceResult {
        val ref = snapshot.bankAccounts.firstOrNull { it.id == refId }
            ?: return WorkspaceResult.Rejected("Those transfer details are no longer here.")
        return WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot,
                facts = listOf(
                    fact(
                        kind = EntryKind.BANK_DETAILS_REVEALED,
                        subject = "Transfer details shown: ${ref.bankName} ····${ref.last4}",
                        at = now,
                        key = "bank:${ref.id}:revealed:$now",
                    ),
                ),
            ),
        )
    }

    public fun sendSupport(
        snapshot: AppSnapshot,
        id: String,
        toHandle: Handle,
        kind: TokenKind,
        amountKobo: Long?,
        now: Long,
    ): WorkspaceResult {
        if (kind !in TokenKind.community) {
            return WorkspaceResult.Rejected("Only Support, Recognition and Growth Grants can be offered as Support.")
        }
        if (snapshot.supports.any { it.id == id }) {
            return WorkspaceResult.Rejected("That Support offer already exists.")
        }
        if (kind == TokenKind.RECOGNITION && amountKobo != null) {
            return WorkspaceResult.Rejected("Recognition carries no amount.")
        }
        if (kind != TokenKind.RECOGNITION && (amountKobo == null || amountKobo <= 0)) {
            return WorkspaceResult.Rejected("Name an amount greater than zero.")
        }
        if (kind == TokenKind.GROWTH_GRANT) {
            return WorkspaceResult.Rejected(
                "Growth Grants are pledged from the Growth Grant screen so the milestone is witnessed.",
            )
        }
        val reference = "TLC-${id.filter { it.isLetterOrDigit() }.takeLast(8).uppercase()}"
        return guarded {
            val support = SupportIntent(
                id = id,
                toHandle = toHandle,
                kind = kind,
                amountKobo = amountKobo,
                state = SupportState.INTENT,
                createdAt = now,
                reference = reference,
            )
            val money = amountKobo?.let { " · ${Money(it).render()}" }.orEmpty()
            WorkspaceChange(
                snapshot = snapshot.copy(supports = listOf(support) + snapshot.supports),
                facts = listOf(
                    fact(
                        kind = EntryKind.SUPPORT_INTENT,
                        subject = "${kindLabel(kind)} offered to ${toHandle.value}$money",
                        amountKobo = amountKobo,
                        at = now,
                        key = "support:${support.id}:intent",
                    ),
                ),
                notice = "Support recorded as an intention. Nothing has moved and TLC holds nothing.",
            )
        }
    }

    public fun claimSupport(
        snapshot: AppSnapshot,
        id: String,
        reference: String,
        now: Long,
    ): WorkspaceResult {
        val support = snapshot.supports.firstOrNull { it.id == id }
            ?: return WorkspaceResult.Rejected("That Support offer is no longer here.")
        if (support.state != SupportState.INTENT) {
            return WorkspaceResult.Rejected("That Support offer has already been claimed.")
        }
        val clean = reference.trim()
        if (clean.isBlank()) return WorkspaceResult.Rejected("Add the bank reference from your own app.")
        return guarded {
            val next = support.copy(state = SupportState.CLAIMED, reference = clean, claimedAt = now)
            WorkspaceChange(
                snapshot = snapshot.copy(supports = snapshot.supports.replaceSupport(next)),
                facts = listOf(
                    fact(
                        kind = EntryKind.SUPPORT_INTENT,
                        subject = "${kindLabel(next.kind)} sent to ${next.toHandle.value} · $clean",
                        amountKobo = next.amountKobo,
                        at = now,
                        key = "support:${next.id}:claim:$clean",
                    ),
                ),
                notice = "Claim recorded. It stays a claim until the recipient acknowledges it.",
            )
        }
    }

    public fun acknowledgeSupport(snapshot: AppSnapshot, id: String, now: Long): WorkspaceResult {
        val support = snapshot.supports.firstOrNull { it.id == id }
            ?: return WorkspaceResult.Rejected("That Support offer is no longer here.")
        if (support.state == SupportState.ACKNOWLEDGED) {
            return WorkspaceResult.Ok(WorkspaceChange(snapshot, notice = "Already acknowledged."))
        }
        return guarded {
            val next = support.copy(state = SupportState.ACKNOWLEDGED, acknowledgedAt = now)
            WorkspaceChange(
                snapshot = snapshot.copy(supports = snapshot.supports.replaceSupport(next)),
                facts = listOf(
                    fact(
                        kind = EntryKind.SUPPORT_ACKNOWLEDGED,
                        subject = "${kindLabel(next.kind)} acknowledged by ${next.toHandle.value}",
                        amountKobo = next.amountKobo,
                        at = now,
                        key = "support:${next.id}:acknowledged",
                    ),
                ),
                notice = "Acknowledged. The record now shows both sides of the gift.",
            )
        }
    }

    public fun logContact(
        snapshot: AppSnapshot,
        id: String,
        channel: ContactChannel,
        purpose: ContactPurpose,
        category: String?,
        now: Long,
        tokenRef: String? = null,
    ): WorkspaceResult {
        if (snapshot.contactLog.any { it.id == id }) {
            return WorkspaceResult.Rejected("That contact event already exists.")
        }
        return guarded {
            val event = ContactEvent(
                id = id,
                channel = channel,
                purpose = purpose,
                category = category?.trim()?.ifBlank { null },
                at = now,
                tokenRef = tokenRef,
            )
            WorkspaceChange(
                snapshot = snapshot.copy(contactLog = listOf(event) + snapshot.contactLog),
                facts = listOf(
                    fact(
                        kind = EntryKind.CONTACT_RECORDED,
                        subject = "Contact opened by ${channel.name.lowercase()} · ${purpose.label}",
                        at = now,
                        key = "contact:${event.id}",
                    ),
                ),
                notice = "Contact noted on this device. The message itself is not stored.",
            )
        }
    }

    public fun clearContactLog(snapshot: AppSnapshot): WorkspaceResult =
        WorkspaceResult.Ok(
            WorkspaceChange(
                snapshot = snapshot.copy(contactLog = emptyList()),
                notice = "Contact history cleared on this device.",
            ),
        )

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

    private fun List<Person>.replacePerson(item: Person): List<Person> =
        map { if (it.id == item.id) item else it }

    private fun List<SupportIntent>.replaceSupport(item: SupportIntent): List<SupportIntent> =
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

    private fun audienceLabel(audience: ReceiptAudience): String = when (audience) {
        ReceiptAudience.ONLY_ME -> "Only me"
        ReceiptAudience.THIS_TRANSFER -> "This transfer"
        ReceiptAudience.MY_CIRCLE -> "My circle"
        ReceiptAudience.ANYONE_WITH_LINK -> "Anyone with the link"
        ReceiptAudience.PUBLIC_IN_RECORD -> "Public in Record"
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
