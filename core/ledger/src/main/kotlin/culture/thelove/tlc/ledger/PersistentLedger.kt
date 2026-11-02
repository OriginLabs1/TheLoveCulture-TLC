package culture.thelove.tlc.ledger

/**
 * INV-08: the record survives the process, and survives it *provably*.
 *
 * [Ledger] is the chain; this class is the chain plus the question "is what I loaded the same
 * thing I wrote?". It refuses to append to a record it cannot verify, because appending to a
 * damaged history would launder the damage into a longer, valid-looking one.
 *
 * Three kinds of damage are distinguished, because they call for different words on screen:
 *
 *  - [Status.Truncated] — the tail is missing relative to the local seal. The remaining prefix
 *    is internally consistent (a prefix always is), so only the seal reveals it.
 *  - [Status.Damaged] — a line does not parse, a hash does not recompute, or the chain breaks.
 *    This is the "someone edited the file" case.
 *  - [Status.Recovered] — a crash left an unterminated last line. The fragment is dropped only
 *    when it cannot be a complete entry, and the seal still has to agree.
 *
 * The seal is local evidence, not a public anchor: it detects a tail lost on this device. A
 * published [Anchor] is what detects a tail lost *and* the seal rewritten.
 */
public class PersistentLedger private constructor(
    private val hasher: Hasher,
    private val storage: LogStorage,
    private var ledger: Ledger,
    private var statusValue: Status,
) {

    private var durable: Int = ledger.size()
    private var faulted: String? = null

    public val status: Status get() = statusValue

    public sealed interface Status {
        /** Opened cleanly. `entries` may be zero. */
        public data class Ready(public val entries: Int) : Status

        /** Opened after dropping an unterminated fragment from an interrupted write. */
        public data class Recovered(public val entries: Int, public val droppedBytes: Int) : Status

        /** The tail is missing: entries present are fewer than the seal says. */
        public data class Truncated(public val expected: Int, public val found: Int) : Status

        /** The record cannot be trusted. No appends are possible. */
        public data class Damaged(public val reason: String) : Status
    }

    public val healthy: Boolean
        get() = status !is Status.Damaged && status !is Status.Truncated && faulted == null

    public fun entries(): List<LedgerEntry> = ledger.entries().take(durable)

    public fun size(): Int = durable

    public fun headHash(): String = entries().lastOrNull()?.hash ?: Ledger.GENESIS

    public fun merkleRoot(): String = Merkle.root(entries().map { it.hash }, hasher)

    public fun verification(): VerificationReport = Ledger.verify(entries(), hasher)

    public fun anchor(createdAt: Long): Anchor = Anchor(
        length = size(),
        headHash = headHash(),
        merkleRoot = merkleRoot(),
        createdAt = createdAt,
    )

    /** The exact text an export or a `ledger-verify --file` run should see. */
    public fun export(): String = LedgerCodec.encodeAll(entries())

    /** Why appends are refused, or `null` when they are allowed. */
    public fun blockedReason(): String? = faulted ?: when (val s = status) {
        is Status.Ready, is Status.Recovered -> null
        is Status.Truncated ->
            "The record is missing its last ${s.expected - s.found} entries. " +
                "TLC will not write over a gap; restore a backup or export what remains."
        is Status.Damaged -> "The record failed verification (${s.reason}). TLC will not add to it."
    }

    /**
     * Replace the whole record with a backup that the caller has already decoded.
     *
     * This is the only sanctioned way to shorten history, and it is still closed: the entries are
     * re-verified here, the text written is exactly the text a verifier would read, and the seal is
     * rewritten from the verified result. An invalid backup cannot overwrite a good record.
     */
    public fun restore(entries: List<LedgerEntry>): RestoreResult {
        if (entries.isEmpty()) {
            val text = LedgerCodec.encodeAll(emptyList())
            return try {
                storage.replaceLog(text)
                storage.writeSeal("0:${Ledger.GENESIS}:${Merkle.root(emptyList(), hasher)}")
                ledger = Ledger(hasher)
                durable = 0
                faulted = null
                statusValue = Status.Ready(0)
                RestoreResult.Restored
            } catch (e: Exception) {
                RestoreResult.Invalid(e.message ?: "The record could not be replaced.")
            }
        }
        val report = Ledger.verify(entries, hasher)
        if (!report.ok) return RestoreResult.Invalid("The imported record does not verify.")
        val rebuilt = Ledger(hasher)
        runCatching { rebuilt.restore(entries) }.onFailure {
            return RestoreResult.Invalid(it.message ?: "The imported record is not a valid chain.")
        }
        val text = LedgerCodec.encodeAll(entries)
        return try {
            storage.replaceLog(text)
            storage.writeSeal("${entries.size}:${rebuilt.headHash()}:${rebuilt.merkleRoot()}")
            ledger = rebuilt
            durable = entries.size
            faulted = null
            statusValue = Status.Ready(entries.size)
            RestoreResult.Restored
        } catch (e: Exception) {
            RestoreResult.Invalid(e.message ?: "The record could not be replaced.")
        }
    }

    public sealed interface RestoreResult {
        public data object Restored : RestoreResult

        public data class Invalid(public val reason: String) : RestoreResult
    }

    public fun append(draft: LedgerDraft): AppendResult {
        blockedReason()?.let { return AppendResult.Invalid(it) }

        val before = ledger.size()
        val result = ledger.append(draft)
        if (result !is AppendResult.Appended) return result

        // Write first, commit second. If the write fails, the in-memory entry is not durable and
        // stays invisible: the next open of the same storage will not contain it.
        try {
            val line = LedgerCodec.encode(result.entry)
            if (durable == 0) {
                // The header belongs to the first entry, so an empty store stays empty until the
                // first real fact — and a fresh file is never a header without a record.
                storage.replaceLog(LedgerCodec.HEADER + "\n" + line + "\n")
            } else {
                storage.appendLine(line)
            }
        } catch (e: Exception) {
            faulted = "This device could not write to the record (${e.message ?: "I/O error"}). " +
                "The entry was not saved and the journal is now read-only for this session."
            return AppendResult.Invalid(faulted!!)
        }
        durable = ledger.size()
        try {
            storage.writeSeal(sealText())
        } catch (_: Exception) {
            // The entry is durable; the seal is only a hint. A stale seal is detected on open as
            // "log longer than seal", which is the normal crash-after-append state.
        }
        check(ledger.size() == before + 1)
        return result
    }

    private fun sealText(): String = "$durable:${headHash()}:${merkleRoot()}"

    public companion object {

        public fun open(hasher: Hasher, storage: LogStorage): PersistentLedger {
            val raw = storage.readLog()
            val seal = parseSeal(storage.readSeal())

            if (raw.isNullOrEmpty()) {
                val status = if (seal != null && seal.length > 0) {
                    Status.Truncated(expected = seal.length, found = 0)
                } else {
                    Status.Ready(0)
                }
                return PersistentLedger(hasher, storage, Ledger(hasher), status)
            }

            if (!raw.startsWith(LedgerCodec.HEADER)) {
                return damaged(hasher, storage, "the file does not begin with ${LedgerCodec.HEADER}")
            }

            var text = raw
            var droppedBytes = 0
            if (!text.endsWith('\n')) {
                // An unterminated line is either a crash mid-write or a hand edit. Accept it only
                // if it is a complete, valid entry; otherwise drop the fragment and keep the prefix.
                val completed = runCatching { LedgerCodec.decode(text + "\n") }.getOrNull()
                val repair = completed is LedgerCodec.Decoded.Ok &&
                    Ledger.verify(completed.entries, hasher).ok
                if (repair) {
                    text += "\n"
                    try {
                        storage.replaceLog(text)
                    } catch (_: Exception) {
                        // Read-only media: the record is still valid, still usable for reading.
                    }
                } else {
                    val cut = text.lastIndexOf('\n')
                    droppedBytes = text.length - (cut + 1)
                    text = text.substring(0, cut + 1)
                    try {
                        storage.replaceLog(text)
                    } catch (_: Exception) {
                        // Read-only media: reads keep working; the next append reports the failure.
                    }
                }
            }

            val decoded = LedgerCodec.decode(text)
            if (decoded !is LedgerCodec.Decoded.Ok) {
                val bad = decoded as LedgerCodec.Decoded.Unreadable
                return damaged(hasher, storage, "line ${bad.lineNumber}: ${bad.reason}")
            }

            val report = Ledger.verify(decoded.entries, hasher)
            if (!report.ok) {
                return damaged(hasher, storage, describe(report))
            }

            val ledger = Ledger(hasher)
            try {
                ledger.restore(decoded.entries)
            } catch (e: IllegalArgumentException) {
                return damaged(hasher, storage, e.message ?: "the record could not be loaded")
            }

            // The seal is checked after the chain verifies: it is evidence about length and head,
            // not a substitute for recomputing the hashes.
            if (seal != null && seal.length > 0) {
                if (decoded.entries.size < seal.length) {
                    return PersistentLedger(
                        hasher, storage, ledger,
                        Status.Truncated(expected = seal.length, found = decoded.entries.size),
                    )
                }
                if (decoded.entries[seal.length - 1].hash != seal.head) {
                    return damaged(
                        hasher, storage,
                        "entry ${seal.length} no longer matches the seal: the record was rewritten",
                    )
                }
                if (decoded.entries.size == seal.length &&
                    Merkle.root(decoded.entries.map { it.hash }, hasher) != seal.root
                ) {
                    return damaged(hasher, storage, "the anchor root no longer matches the seal")
                }
            }

            val status = when {
                droppedBytes > 0 -> Status.Recovered(decoded.entries.size, droppedBytes)
                raw != text -> Status.Recovered(decoded.entries.size, 0)
                else -> Status.Ready(decoded.entries.size)
            }
            return PersistentLedger(hasher, storage, ledger, status)
        }

        private fun damaged(hasher: Hasher, storage: LogStorage, reason: String): PersistentLedger =
            PersistentLedger(hasher, storage, Ledger(hasher), Status.Damaged(reason))

        private fun describe(report: VerificationReport): String {
            val first = report.failures.firstOrNull() ?: return "unknown verification failure"
            val where = if (first.index >= 0) "entry ${first.index + 1}" else "the record"
            return "$where: ${first::class.simpleName}"
        }

        private data class Seal(val length: Int, val head: String, val root: String)

        private fun parseSeal(raw: String?): Seal? {
            if (raw.isNullOrBlank()) return null
            val parts = raw.split(':')
            if (parts.size != 3) return null
            val length = parts[0].toIntOrNull() ?: return null
            if (length < 0) return null
            if (parts[1].length != 64 || parts[2].length != 64) return null
            return Seal(length, parts[1], parts[2])
        }
    }
}
