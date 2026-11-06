package culture.thelove.tlc.android.data

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import culture.thelove.tlc.domain.BankAccount
import culture.thelove.tlc.domain.BankAccountRef
import culture.thelove.tlc.domain.BankDirectory
import culture.thelove.tlc.domain.ContactChannel
import culture.thelove.tlc.domain.ContactPurpose
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Handle
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.BundleCodec
import culture.thelove.tlc.experience.BundleResult
import culture.thelove.tlc.experience.Dues
import culture.thelove.tlc.experience.GrantEvent
import culture.thelove.tlc.experience.Preferences
import culture.thelove.tlc.experience.ReceiptAudience
import culture.thelove.tlc.experience.ReceiptSubject
import culture.thelove.tlc.experience.StateFile
import culture.thelove.tlc.experience.StateResult
import culture.thelove.tlc.experience.TokenReceipt
import culture.thelove.tlc.experience.Workspace
import culture.thelove.tlc.experience.WorkspaceChange
import culture.thelove.tlc.experience.WorkspaceResult
import culture.thelove.tlc.ledger.LedgerCodec
import culture.thelove.tlc.ledger.LedgerDraft
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The app's single coordinator: the durable record, the durable preferences/circle state, and the
 * operations that connect them.
 *
 * A change is committed in two steps: the facts are appended to the hash chain first, then the
 * derived snapshot is written atomically. If the record refuses a fact, the snapshot is not written
 * at all. That is why the ledger can always explain the state, even after a crash.
 */
public class AppState(context: Context) {

    private val filesDir: File = context.applicationContext.filesDir
    public val journal: JournalState = JournalState(filesDir)
    private val stateFile = StateFile(filesDir)
    private val hasher = PlatformSha256()
    private val vault = BankVault(filesDir)
    private val media = MediaFiles(context)

    public var loading: Boolean by mutableStateOf(true)
        private set

    public var loadError: String? by mutableStateOf(null)
        private set

    public var snapshot: AppSnapshot? by mutableStateOf(null)
        private set

    /** Full accounts, decrypted in memory only. The screens must reveal them deliberately. */
    public var vaultAccounts: List<BankAccount> by mutableStateOf(emptyList())
        private set

    public var vaultError: String? by mutableStateOf(null)
        private set

    public var busy: Boolean by mutableStateOf(false)
        private set

    public val entries get() = journal.entries
    public val headHash get() = journal.headHash
    public val merkleRoot get() = journal.merkleRoot
    public val status get() = journal.status
    public val report get() = journal.report
    public val healthy get() = journal.healthy
    public val blockedReason get() = journal.blockedReason

    public suspend fun load() {
        journal.load()
        val stored = withContext(Dispatchers.IO) { runCatching { stateFile.read() } }
        stored.onSuccess { result ->
            when (result) {
                null -> Unit
                is StateResult.Ok -> snapshot = result.snapshot
                is StateResult.Invalid -> loadError = result.reason
            }
        }.onFailure {
            loadError = it.message ?: "The state could not be read."
        }
        if (journal.loadError != null) loadError = journal.loadError
        vaultAccounts = withContext(Dispatchers.IO) { vault.load() }
        vaultError = vault.lastError
        loading = false
    }

    public suspend fun onboard(
        displayName: String,
        circleName: String,
        role: Role,
        handle: Handle? = null,
    ): String? {
        val result = Workspace.onboard(
            displayName = displayName,
            circleName = circleName,
            role = role,
            recordId = UUID.randomUUID().toString().replace("-", ""),
            now = System.currentTimeMillis(),
            handle = handle,
        )
        return commit(result)
    }

    public suspend fun claimHandle(handle: Handle): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.claimHandle(current, handle, System.currentTimeMillis()))
    }

    public suspend fun updatePreferences(preferences: Preferences): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.updatePreferences(current, preferences))
    }

    // -----------------------------------------------------------------------------------------
    // Profile and receipts (v0.0.6). Files are written first; state is committed second; a
    // rejected commit removes the new file, so the folder can never hold orphaned evidence.
    // -----------------------------------------------------------------------------------------

    public fun mediaFile(name: String?): File? = media.file(name)

    /** Delete private images; called only when the person resets to a fresh circle. */
    public suspend fun clearMedia() {
        media.clear()
    }

    public suspend fun updateProfile(displayName: String, circleName: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.updateProfile(
                snapshot = current,
                displayName = displayName,
                circleName = circleName,
                photoRef = current.profile.photoRef,
                now = System.currentTimeMillis(),
            ),
        )
    }

    public suspend fun setProfilePhoto(
        uri: Uri,
        zoom: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val imported = media.importAvatar(uri, zoom, offsetX, offsetY) ?: return "That image could not be read."
        val previous = current.profile.photoRef
        val error = commit(
            Workspace.updateProfile(
                snapshot = current,
                displayName = current.profile.displayName,
                circleName = current.profile.circleName,
                photoRef = imported.fileName,
                now = System.currentTimeMillis(),
            ),
        )
        if (error != null) {
            media.delete(imported.fileName)
            return error
        }
        if (previous != imported.fileName) media.delete(previous)
        return null
    }

    public suspend fun removeProfilePhoto(): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val previous = current.profile.photoRef ?: return null
        val error = commit(
            Workspace.updateProfile(
                snapshot = current,
                displayName = current.profile.displayName,
                circleName = current.profile.circleName,
                photoRef = null,
                now = System.currentTimeMillis(),
            ),
        )
        if (error == null) media.delete(previous)
        return error
    }

    public suspend fun attachReceipt(
        subjectKind: ReceiptSubject,
        subjectId: String,
        uri: Uri,
        audience: ReceiptAudience,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val imported = media.importReceipt(uri) ?: return "That image could not be read."
        val receipt = TokenReceipt(
            id = "receipt-${UUID.randomUUID().toString().take(8)}",
            subjectKind = subjectKind,
            subjectId = subjectId,
            fileName = imported.fileName,
            hash = imported.hash,
            audience = audience,
            addedAt = System.currentTimeMillis(),
        )
        val previous = current.receipt(subjectKind, subjectId)?.fileName
        val error = commit(Workspace.attachReceipt(current, receipt, System.currentTimeMillis()))
        if (error != null) {
            media.delete(imported.fileName)
            return error
        }
        if (previous != imported.fileName) media.delete(previous)
        return null
    }

    public suspend fun removeReceipt(receiptId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val receipt = current.receipts.firstOrNull { it.id == receiptId }
            ?: return "That receipt is no longer here."
        val error = commit(Workspace.removeReceipt(current, receiptId))
        if (error == null) media.delete(receipt.fileName)
        return error
    }

    public suspend fun setReceiptAudience(receiptId: String, audience: ReceiptAudience): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.setReceiptAudience(current, receiptId, audience))
    }

    public suspend fun addCoSteward(name: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.addCoSteward(current, "pair-${UUID.randomUUID().toString().take(8)}", name))
    }

    // -----------------------------------------------------------------------------------------
    // Trust & Contact
    // -----------------------------------------------------------------------------------------

    public suspend fun addPerson(
        handle: Handle,
        displayName: String,
        role: Role,
        bankRefId: String? = null,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.addPerson(
                snapshot = current,
                id = "person-${UUID.randomUUID().toString().take(8)}",
                handle = handle,
                displayName = displayName,
                role = role,
                now = System.currentTimeMillis(),
                bankRefId = bankRefId,
            ),
        )
    }

    public suspend fun attachBank(personId: String, bankRefId: String?): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.attachBank(current, personId, bankRefId))
    }

    public suspend fun removePerson(personId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.removePerson(current, personId))
    }

    /**
     * Encrypt the full account first, then record only its reference. If the record refuses the
     * reference, the vault is put back exactly as it was.
     */
    public suspend fun saveBankAccount(
        bankCode: String,
        accountNumber: String,
        holderName: String,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val bank = BankDirectory.byCode(bankCode) ?: return "Choose a bank from the list."
        val account = try {
            BankAccount(bank, accountNumber.filter { it.isDigit() }, holderName)
        } catch (e: IllegalArgumentException) {
            return e.message ?: "Those transfer details are not valid."
        }
        val previous = vaultAccounts
        val next = previous + account
        val written = withContext(Dispatchers.IO) { runCatching { vault.save(next) } }
        if (written.isFailure) {
            return written.exceptionOrNull()?.message ?: "The encrypted vault could not be written."
        }
        vaultAccounts = next
        val error = commit(
            Workspace.saveBankAccount(
                snapshot = current,
                id = "bank-${UUID.randomUUID().toString().take(8)}",
                account = account,
                now = System.currentTimeMillis(),
            ),
        )
        if (error != null) {
            withContext(Dispatchers.IO) { runCatching { vault.save(previous) } }
            vaultAccounts = previous
        }
        return error
    }

    public suspend fun removeBankAccount(refId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val ref = current.bankAccounts.firstOrNull { it.id == refId }
            ?: return "Those transfer details are no longer here."
        val previous = vaultAccounts
        val next = previous.filterNot { it.fingerprint() == ref.fingerprint }
        val written = withContext(Dispatchers.IO) { runCatching { vault.save(next) } }
        if (written.isFailure) {
            return written.exceptionOrNull()?.message ?: "The encrypted vault could not be written."
        }
        vaultAccounts = next
        val error = commit(Workspace.removeBankAccount(current, refId))
        if (error != null) {
            withContext(Dispatchers.IO) { runCatching { vault.save(previous) } }
            vaultAccounts = previous
        }
        return error
    }

    /** Returns the full account only for the moment of a deliberate reveal. */
    public suspend fun revealBankAccount(refId: String): BankAccount? {
        val current = snapshot ?: return null
        val ref = current.bankAccounts.firstOrNull { it.id == refId } ?: return null
        val account = vaultAccounts.firstOrNull { it.fingerprint() == ref.fingerprint } ?: return null
        commit(Workspace.markBankRevealed(current, refId, System.currentTimeMillis()))
        return account
    }

    public suspend fun sendSupport(
        toHandle: Handle,
        kind: TokenKind,
        amountKobo: Long?,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.sendSupport(
                snapshot = current,
                id = "support-${UUID.randomUUID().toString().take(8)}",
                toHandle = toHandle,
                kind = kind,
                amountKobo = amountKobo,
                now = System.currentTimeMillis(),
            ),
        )
    }

    public suspend fun claimSupport(id: String, reference: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.claimSupport(current, id, reference, System.currentTimeMillis()))
    }

    public suspend fun acknowledgeSupport(id: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.acknowledgeSupport(current, id, System.currentTimeMillis()))
    }

    public suspend fun logContact(
        channel: ContactChannel,
        purpose: ContactPurpose,
        category: String?,
        tokenRef: String? = null,
    ): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.logContact(
                snapshot = current,
                id = "contact-${UUID.randomUUID().toString().take(8)}",
                channel = channel,
                purpose = purpose,
                category = category,
                now = System.currentTimeMillis(),
                tokenRef = tokenRef,
            ),
        )
    }

    public suspend fun clearContactLog(): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.clearContactLog(current))
    }

    public suspend fun propose(action: SensitiveAction, summary: String?): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.propose(
                snapshot = current,
                id = "proposal-${UUID.randomUUID().toString().take(8)}",
                action = action,
                summary = summary,
                now = System.currentTimeMillis(),
            ),
        )
    }

    public suspend fun approve(proposalId: String, actorId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.approve(current, proposalId, actorId, System.currentTimeMillis()))
    }

    public suspend fun veto(proposalId: String, actorId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.veto(current, proposalId, actorId, System.currentTimeMillis()))
    }

    public suspend fun addGrant(beneficiary: String, purpose: String, amountKobo: Long?): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.addGrant(
                snapshot = current,
                id = "grant-${UUID.randomUUID().toString().take(8)}",
                beneficiary = beneficiary,
                purpose = purpose,
                amountKobo = amountKobo,
                now = System.currentTimeMillis(),
            ),
        )
    }

    public suspend fun advanceGrant(grantId: String, event: GrantEvent): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.advanceGrant(current, grantId, event))
    }

    public suspend fun addDue(kind: TokenKind, amountKobo: Long, period: FeePeriod): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(
            Workspace.addDue(
                snapshot = current,
                id = "due-${UUID.randomUUID().toString().take(8)}",
                kind = kind,
                amountKobo = amountKobo,
                period = period,
                now = System.currentTimeMillis(),
            ),
        )
    }

    public suspend fun issueInstruction(dueId: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val actor = Dues.memberActor(current.primary.id)
        return commit(
            Workspace.advanceDue(
                current,
                dueId,
                FeeEvent.IssueInstruction(actor, System.currentTimeMillis()),
            ),
        )
    }

    public suspend fun recordProof(dueId: String, amountKobo: Long, bankReference: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val due = current.dues.firstOrNull { it.id == dueId } ?: return "That due is no longer here."
        val at = System.currentTimeMillis()
        val hash = Dues.proofHash(hasher, amountKobo, bankReference, at)
        return commit(
            Workspace.advanceDue(
                current,
                dueId,
                Dues.submitProof(
                    record = due,
                    actor = Dues.memberActor(current.primary.id),
                    at = at,
                    proofHash = hash,
                    statedAmountKobo = amountKobo,
                    bankReference = bankReference,
                ),
            ),
        )
    }

    public suspend fun attestDue(dueId: String, statementRef: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val due = current.dues.firstOrNull { it.id == dueId } ?: return "That due is no longer here."
        return commit(
            Workspace.advanceDue(
                current,
                dueId,
                Dues.attest(due, System.currentTimeMillis(), statementRef),
            ),
        )
    }

    public suspend fun waiveDue(dueId: String, note: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        val due = current.dues.firstOrNull { it.id == dueId } ?: return "That due is no longer here."
        return commit(
            Workspace.advanceDue(
                current,
                dueId,
                Dues.waive(due, Dues.pioneerActor(), System.currentTimeMillis(), note),
            ),
        )
    }

    public suspend fun addRecognition(who: String, what: String): SaveOutcome =
        journal.addRecognition(who, what)

    public suspend fun appendFact(draft: LedgerDraft): SaveOutcome = journal.append(draft)

    public suspend fun exportRecord(): String = journal.exportText()

    public suspend fun exportBundle(): String? {
        val current = snapshot ?: return null
        return BundleCodec.encode(current, journal.exportText(), System.currentTimeMillis())
    }

    /** Import a verified backup. Returns an error string, or `null` when it was accepted. */
    public suspend fun importBundle(text: String): String? {
        val decoded = BundleCodec.decode(text, hasher)
        if (decoded !is BundleResult.Ok) {
            return (decoded as BundleResult.Invalid).reason
        }
        val recordError = journal.replaceRecord(decoded.record)
        if (recordError != null) return recordError
        val written = withContext(Dispatchers.IO) {
            runCatching { stateFile.write(decoded.snapshot) }
        }
        if (written.isFailure) {
            return written.exceptionOrNull()?.message ?: "The restored state could not be saved."
        }
        snapshot = decoded.snapshot
        return null
    }

    /**
     * Abandon an unreadable state and start over.
     *
     * The record is replaced with an empty, sealed one rather than deleted, so a damaged file is
     * never mistaken for a healthy empty ledger. This is the only path that discards history, and
     * it is only offered when the app has already refused to open the state.
     */
    public suspend fun startFresh(): String? {
        val recordError = journal.replaceRecord(LedgerCodec.encodeAll(emptyList()))
        if (recordError != null) return recordError
        val deleted = withContext(Dispatchers.IO) { runCatching { stateFile.delete() } }
        if (deleted.isFailure) {
            return deleted.exceptionOrNull()?.message ?: "The state file could not be cleared."
        }
        snapshot = null
        loadError = null
        return null
    }

    private suspend fun commit(result: WorkspaceResult): String? {
        val change: WorkspaceChange = when (result) {
            is WorkspaceResult.Rejected -> return result.reason
            is WorkspaceResult.Ok -> result.change
        }
        busy = true
        return try {
            for (fact in change.facts) {
                when (val outcome = journal.append(fact)) {
                    is SaveOutcome.Failed -> return outcome.reason
                    is SaveOutcome.Saved -> Unit
                }
            }
            val written = withContext(Dispatchers.IO) { runCatching { stateFile.write(change.snapshot) } }
            if (written.isFailure) {
                written.exceptionOrNull()?.message ?: "The circle state could not be saved."
            } else {
                snapshot = change.snapshot
                null
            }
        } finally {
            busy = false
        }
    }

    public companion object {
        public const val RECORD_ID_LENGTH: Int = 32
    }
}
