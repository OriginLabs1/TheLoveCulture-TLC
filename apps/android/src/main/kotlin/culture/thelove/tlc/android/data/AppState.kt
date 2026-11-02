package culture.thelove.tlc.android.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import culture.thelove.tlc.domain.FeeEvent
import culture.thelove.tlc.domain.FeePeriod
import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.domain.SensitiveAction
import culture.thelove.tlc.domain.TokenKind
import culture.thelove.tlc.experience.AppSnapshot
import culture.thelove.tlc.experience.BundleCodec
import culture.thelove.tlc.experience.BundleResult
import culture.thelove.tlc.experience.Dues
import culture.thelove.tlc.experience.GrantEvent
import culture.thelove.tlc.experience.Preferences
import culture.thelove.tlc.experience.StateFile
import culture.thelove.tlc.experience.StateResult
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
public class AppState(filesDir: File) {

    public val journal: JournalState = JournalState(filesDir)
    private val stateFile = StateFile(filesDir)
    private val hasher = PlatformSha256()

    public var loading: Boolean by mutableStateOf(true)
        private set

    public var loadError: String? by mutableStateOf(null)
        private set

    public var snapshot: AppSnapshot? by mutableStateOf(null)
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
        loading = false
    }

    public suspend fun onboard(displayName: String, circleName: String, role: Role): String? {
        val result = Workspace.onboard(
            displayName = displayName,
            circleName = circleName,
            role = role,
            recordId = UUID.randomUUID().toString().replace("-", ""),
            now = System.currentTimeMillis(),
        )
        return commit(result)
    }

    public suspend fun updatePreferences(preferences: Preferences): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.updatePreferences(current, preferences))
    }

    public suspend fun addCoSteward(name: String): String? {
        val current = snapshot ?: return "The circle is not open yet."
        return commit(Workspace.addCoSteward(current, "pair-${UUID.randomUUID().toString().take(8)}", name))
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
