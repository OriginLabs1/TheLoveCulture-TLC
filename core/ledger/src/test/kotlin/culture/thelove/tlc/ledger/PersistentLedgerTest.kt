package culture.thelove.tlc.ledger

import culture.thelove.tlc.ledger.adapter.FileLogStorage
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * INV-08 / SK-04: the record must survive the process, and the app must know when it did not.
 * These tests use [InMemoryLogStorage] as a simulated disk, including simulated crashes and
 * simulated edits to the file.
 */
class PersistentLedgerTest {

    private val hasher = PlatformSha256()
    private val t0 = 1_760_000_000_000L

    private fun draft(n: Int, key: String = "k-$n"): LedgerDraft = LedgerDraft(
        kind = EntryKind.RECOGNITION,
        subject = "party:friend-$n",
        occurredAt = t0 + n,
        idempotencyKey = key,
    )

    private fun seeded(storage: InMemoryLogStorage = InMemoryLogStorage(), count: Int = 3): PersistentLedger {
        val ledger = PersistentLedger.open(hasher, storage)
        (1..count).forEach { assertIs<AppendResult.Appended>(ledger.append(draft(it))) }
        return ledger
    }

    @Test
    fun `entries survive a restart`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 3)

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Ready>(reopened.status)
        assertEquals(3, reopened.size())
        assertEquals(3, reopened.entries().size)
        assertTrue(reopened.verification().ok)
        assertTrue(reopened.healthy)
    }

    @Test
    fun `an empty record opens ready and accepts the first entry`() {
        val opened = PersistentLedger.open(hasher, InMemoryLogStorage())
        assertEquals(PersistentLedger.Status.Ready(0), opened.status)
        assertEquals(Ledger.GENESIS, opened.headHash())
        assertIs<AppendResult.Appended>(opened.append(draft(1)))
        assertEquals(1, opened.size())
    }

    @Test
    fun `a subject with separators and newlines survives the file`() {
        val storage = InMemoryLogStorage()
        val ledger = PersistentLedger.open(hasher, storage)
        val subject = "Ada \uD83D\uDC9B | the plumber\nwho came at 7pm \\ done"
        val result = assertIs<AppendResult.Appended>(
            ledger.append(LedgerDraft(EntryKind.RECOGNITION, subject, occurredAt = t0)),
        )

        val reopened = PersistentLedger.open(hasher, storage)
        assertEquals(subject, reopened.entries().single().subject)
        assertEquals(result.entry.hash, reopened.headHash())
        assertTrue(reopened.verification().ok)
    }

    @Test
    fun `editing the stored amount is detected at open`() {
        val storage = InMemoryLogStorage()
        val ledger = PersistentLedger.open(hasher, storage)
        ledger.append(LedgerDraft(EntryKind.FEE_INTENT, "ob:1", 100_000, t0, "k-1"))
        ledger.append(LedgerDraft(EntryKind.RECOGNITION, "party:bob", occurredAt = t0 + 1, idempotencyKey = "k-2"))

        val tampered = assertNotNull(storage.readLog()).replace("|100000|", "|999999|")
        storage.replaceLog(tampered)

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Damaged>(reopened.status)
        assertFalse(reopened.healthy)
        assertEquals(0, reopened.size())
    }

    @Test
    fun `deleting the last line is detected as truncation`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 3)
        val text = assertNotNull(storage.readLog())
        val lines = text.trimEnd('\n').split('\n')
        storage.replaceLog(lines.dropLast(1).joinToString("\n") + "\n")

        val reopened = PersistentLedger.open(hasher, storage)
        val status = assertIs<PersistentLedger.Status.Truncated>(reopened.status)
        assertEquals(3, status.expected)
        assertEquals(2, status.found)
        assertNotNull(reopened.blockedReason())
    }

    @Test
    fun `deleting the whole file while the seal remains is truncation`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 2)
        storage.replaceLog("")

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Truncated>(reopened.status)
        assertEquals(2, reopened.status.let { (it as PersistentLedger.Status.Truncated).expected })
    }

    @Test
    fun `rewriting an entry and recomputing its hash still breaks the seal`() {
        val storage = InMemoryLogStorage()
        val ledger = PersistentLedger.open(hasher, storage)
        ledger.append(LedgerDraft(EntryKind.RECOGNITION, "party:alice", occurredAt = t0, idempotencyKey = "k-1"))
        ledger.append(LedgerDraft(EntryKind.RECOGNITION, "party:bob", occurredAt = t0 + 1, idempotencyKey = "k-2"))

        // A forger who understands the chain: change the last subject and recompute that entry's
        // hash. The chain itself would still look valid — the seal is what catches the edit.
        val entries = ledger.entries().toMutableList()
        val forgedDraft = LedgerDraft(EntryKind.RECOGNITION, "party:mallory", occurredAt = t0 + 1, idempotencyKey = "k-2")
        val forged = entries[1].copy(
            subject = forgedDraft.subject,
            hash = LedgerEntry.computeHash(
                hasher, entries[1].seq, entries[1].kind, forgedDraft.subject,
                entries[1].amountKobo, entries[1].occurredAt, entries[1].idempotencyKey, entries[0].hash,
            ),
        )
        entries[1] = forged
        storage.replaceLog(LedgerCodec.encodeAll(entries))

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Damaged>(reopened.status)
        assertTrue(
            (reopened.status as PersistentLedger.Status.Damaged).reason.contains("seal"),
            reopened.status.toString(),
        )
    }

    @Test
    fun `a duplicated idempotency key across restarts returns the original`() {
        val storage = InMemoryLogStorage()
        val first = seeded(storage, 2)
        val key = first.entries().first().idempotencyKey

        val reopened = PersistentLedger.open(hasher, storage)
        val before = reopened.size()
        val result = reopened.append(
            LedgerDraft(EntryKind.RECOGNITION, "party:someone-else", occurredAt = t0 + 50, idempotencyKey = key),
        )
        val duplicate = assertIs<AppendResult.Duplicate>(result)
        assertEquals(first.entries().first().hash, duplicate.existing.hash)
        assertEquals(before, reopened.size())
        assertEquals(before, assertNotNull(storage.readLog()).trimEnd('\n').lines().size - 1)
    }

    @Test
    fun `a damaged record refuses new entries`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 2)
        storage.replaceLog(assertNotNull(storage.readLog()).replace("RECOGNITION", "PROOF_RECORDED"))

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Damaged>(reopened.status)
        val result = reopened.append(draft(99))
        assertIs<AppendResult.Invalid>(result)
        assertEquals(0, reopened.size())
    }

    @Test
    fun `an interrupted write is recovered by dropping only the fragment`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 3)
        val text = assertNotNull(storage.readLog())
        // Simulate a crash in the middle of appending a fourth entry.
        storage.replaceLog(text + text.trimEnd('\n').split('\n').last().take(20))

        val reopened = PersistentLedger.open(hasher, storage)
        val status = assertIs<PersistentLedger.Status.Recovered>(reopened.status)
        assertEquals(3, status.entries)
        assertTrue(status.droppedBytes > 0)
        assertEquals(3, reopened.size())
        assertTrue(reopened.verification().ok)

        // Recovery must not make the record read-only.
        assertIs<AppendResult.Appended>(reopened.append(draft(4)))
        assertEquals(4, PersistentLedger.open(hasher, storage).size())
    }

    @Test
    fun `a complete last line without its newline is repaired, not dropped`() {
        val storage = InMemoryLogStorage()
        seeded(storage, 3)
        storage.replaceLog(assertNotNull(storage.readLog()).trimEnd('\n'))

        val reopened = PersistentLedger.open(hasher, storage)
        assertIs<PersistentLedger.Status.Recovered>(reopened.status)
        assertEquals(3, reopened.size())
        assertTrue(assertNotNull(storage.readLog()).endsWith('\n'))
    }

    @Test
    fun `a record copied without a seal still verifies`() {
        val storage = InMemoryLogStorage()
        val ledger = seeded(storage, 3)
        val copy = InMemoryLogStorage(initialLog = ledger.export())

        val reopened = PersistentLedger.open(hasher, copy)
        assertIs<PersistentLedger.Status.Ready>(reopened.status)
        assertEquals(3, reopened.size())
        assertTrue(reopened.verification().ok)
    }

    @Test
    fun `a failed write is not durable and does not appear after restart`() {
        val storage = InMemoryLogStorage()
        val ledger = seeded(storage, 2)
        storage.failNextWrite = true

        val result = ledger.append(draft(3))
        assertIs<AppendResult.Invalid>(result)
        assertFalse(ledger.healthy)
        assertEquals(2, ledger.size())
        // Appends stay refused for the session rather than silently diverging from the disk.
        assertIs<AppendResult.Invalid>(ledger.append(draft(4)))

        val reopened = PersistentLedger.open(hasher, storage)
        assertEquals(2, reopened.size())
        assertTrue(reopened.verification().ok)
    }

    @Test
    fun `export is the format the command-line verifier reads`() {
        val ledger = seeded(InMemoryLogStorage(), 3)
        val text = ledger.export()
        assertTrue(text.startsWith(LedgerCodec.HEADER + "\n"))
        assertEquals(3, text.trimEnd('\n').lines().size - 1)
        val decoded = assertIs<LedgerCodec.Decoded.Ok>(LedgerCodec.decode(text))
        assertTrue(Ledger.verify(decoded.entries, hasher).ok)
    }

    @Test
    fun `an anchor commits to the length the record had`() {
        val ledger = seeded(InMemoryLogStorage(), 3)
        val anchor = ledger.anchor(t0 + 100)
        assertEquals(3, anchor.length)
        assertEquals(ledger.headHash(), anchor.headHash)
        assertEquals(ledger.merkleRoot(), anchor.merkleRoot)
        assertNull(PersistentLedger.open(hasher, InMemoryLogStorage(initialLog = ledger.export())).blockedReason())
    }

    @Test
    fun `a verified backup replaces the whole record and survives a reopen`() {
        val dir = Files.createTempDirectory("tlc-restore").toFile()
        val hasher = PlatformSha256()
        val storage = FileLogStorage(dir)
        val original = PersistentLedger.open(hasher, storage)
        original.append(LedgerDraft(EntryKind.RECOGNITION, "first", occurredAt = 1, idempotencyKey = "a"))
        original.append(LedgerDraft(EntryKind.RECOGNITION, "second", occurredAt = 2, idempotencyKey = "b"))
        val backup = original.entries()

        val replacement = Ledger(hasher)
        replacement.append(LedgerDraft(EntryKind.RECOGNITION, "replacement", occurredAt = 3, idempotencyKey = "c"))

        val reopened = PersistentLedger.open(hasher, storage)
        val result = reopened.restore(replacement.entries())
        assertTrue(result is PersistentLedger.RestoreResult.Restored)
        assertEquals(1, reopened.size())
        assertEquals("replacement", reopened.entries().single().subject)

        val after = PersistentLedger.open(hasher, storage)
        assertTrue(after.healthy)
        assertEquals(1, after.size())
        assertEquals(replacement.entries().single().hash, after.headHash())

        // The old backup still verifies, so restoring it is not a loss.
        assertTrue(after.restore(backup) is PersistentLedger.RestoreResult.Restored)
        assertEquals(2, after.size())
    }

    @Test
    fun `an invalid backup cannot overwrite a good record`() {
        val dir = Files.createTempDirectory("tlc-restore-bad").toFile()
        val hasher = PlatformSha256()
        val storage = FileLogStorage(dir)
        val ledger = PersistentLedger.open(hasher, storage)
        ledger.append(LedgerDraft(EntryKind.RECOGNITION, "keep me", occurredAt = 1, idempotencyKey = "a"))
        val good = ledger.entries().single()

        val result = ledger.restore(listOf(good.copy(subject = "tampered")))
        assertTrue(result is PersistentLedger.RestoreResult.Invalid)
        assertEquals("keep me", ledger.entries().single().subject)

        val after = PersistentLedger.open(hasher, storage)
        assertEquals("keep me", after.entries().single().subject)
    }
}
