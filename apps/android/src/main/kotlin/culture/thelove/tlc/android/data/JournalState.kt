package culture.thelove.tlc.android.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import culture.thelove.tlc.ledger.Anchor
import culture.thelove.tlc.ledger.AppendResult
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerDraft
import culture.thelove.tlc.ledger.LedgerEntry
import culture.thelove.tlc.ledger.Merkle
import culture.thelove.tlc.ledger.PersistentLedger
import culture.thelove.tlc.ledger.VerificationReport
import culture.thelove.tlc.ledger.adapter.FileLogStorage
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

/**
 * The shell's view of the record. It owns *presentation* state only; every rule stays in
 * [PersistentLedger]. Disk work happens off the main thread, and the UI state is published on the
 * caller's (main) dispatcher so recomposition never observes a half-written view.
 */
public class JournalState(filesDir: File) {

    private val hasher = PlatformSha256()
    private val storage = FileLogStorage(filesDir)
    private var ledger: PersistentLedger? = null

    public var loading: Boolean by mutableStateOf(true)
        private set

    public var loadError: String? by mutableStateOf(null)
        private set

    public var entries: List<LedgerEntry> by mutableStateOf(emptyList())
        private set

    public var status: PersistentLedger.Status? by mutableStateOf(null)
        private set

    public var headHash: String by mutableStateOf(Ledger.GENESIS)
        private set

    public var merkleRoot: String by mutableStateOf(Ledger.GENESIS)
        private set

    public var report: VerificationReport? by mutableStateOf(null)
        private set

    public val healthy: Boolean get() = ledger?.healthy == true

    public val blockedReason: String? get() = ledger?.blockedReason()

    public suspend fun load() {
        val opened = withContext(Dispatchers.IO) { runCatching { PersistentLedger.open(hasher, storage) } }
        opened.onSuccess {
            ledger = it
            publish()
        }.onFailure {
            loadError = it.message ?: "The record could not be opened."
        }
        loading = false
    }

    public suspend fun addRecognition(who: String, what: String): SaveOutcome {
        val current = ledger ?: return SaveOutcome.Failed("The record is not open yet.")
        val name = who.trim()
        if (name.isBlank()) return SaveOutcome.Failed("Name the person, or how you know them.")
        if (name.length > MAX_WHO) return SaveOutcome.Failed("That name is too long for one entry.")
        val note = what.trim()
        if (note.length > MAX_WHAT) return SaveOutcome.Failed("That note is too long for one entry.")

        // The subject is the whole sentence a person will read later. Recognition carries no
        // amount: it is a signal, not a payment (INV-01/INV-03).
        val subject = if (note.isBlank()) name else "$name — $note"
        val draft = LedgerDraft(
            kind = EntryKind.RECOGNITION,
            subject = subject,
            occurredAt = System.currentTimeMillis(),
            idempotencyKey = "recognition:${UUID.randomUUID()}",
        )
        return when (val result = withContext(Dispatchers.IO) { current.append(draft) }) {
            is AppendResult.Appended -> {
                publish()
                SaveOutcome.Saved(result.entry)
            }
            is AppendResult.Duplicate -> {
                publish()
                SaveOutcome.Saved(result.existing)
            }
            is AppendResult.Invalid -> SaveOutcome.Failed(result.reason)
        }
    }

    public suspend fun exportText(): String = withContext(Dispatchers.IO) { ledger?.export().orEmpty() }

    public fun anchor(createdAt: Long = System.currentTimeMillis()): Anchor? = ledger?.anchor(createdAt)

    /** A proof for one entry against the current root; the screen renders it, it does not decide. */
    public fun proof(index: Int): List<Merkle.Step>? {
        val current = ledger ?: return null
        val leaves = current.entries().map { it.hash }
        if (index !in leaves.indices) return null
        return Merkle.proof(index, leaves, hasher)
    }

    public fun proofVerifies(leaf: String, steps: List<Merkle.Step>): Boolean =
        Merkle.verify(leaf, steps, merkleRoot, hasher)

    private fun publish() {
        val current = ledger ?: return
        entries = current.entries()
        status = current.status
        headHash = current.headHash()
        merkleRoot = current.merkleRoot()
        report = current.verification()
    }

    public companion object {
        public const val MAX_WHO: Int = 120
        public const val MAX_WHAT: Int = 280
    }
}

public sealed interface SaveOutcome {
    public data class Saved(public val entry: LedgerEntry) : SaveOutcome

    public data class Failed(public val reason: String) : SaveOutcome
}
