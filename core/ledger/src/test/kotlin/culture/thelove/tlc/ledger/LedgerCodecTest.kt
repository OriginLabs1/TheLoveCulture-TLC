package culture.thelove.tlc.ledger

import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The file format is the durability contract. If a subject cannot survive a round trip, a person's
 * record is silently altered; if a broken line cannot be rejected, the format is not checkable.
 */
class LedgerCodecTest {

    private val hasher = PlatformSha256()
    private val t0 = 1_760_000_000_000L

    private fun entry(
        seq: Long = 1,
        kind: EntryKind = EntryKind.RECOGNITION,
        subject: String = "party:alice",
        amount: Long? = null,
        key: String? = null,
        prev: String = Ledger.GENESIS,
    ): LedgerEntry = LedgerEntry(
        seq = seq,
        kind = kind,
        subject = subject,
        amountKobo = amount,
        occurredAt = t0 + seq,
        idempotencyKey = key,
        prevHash = prev,
        hash = LedgerEntry.computeHash(hasher, seq, kind, subject, amount, t0 + seq, key, prev),
    )

    @Test
    fun `every field round trips`() {
        val original = entry(amount = 250_000, key = "k-1")
        val decoded = LedgerCodec.decode(LedgerCodec.encodeAll(listOf(original)))
        assertIs<LedgerCodec.Decoded.Ok>(decoded)
        assertEquals(listOf(original), decoded.entries)
        assertTrue(decoded.trailingNewline)
    }

    @Test
    fun `absent optional fields stay absent`() {
        val original = entry()
        val line = LedgerCodec.encode(original)
        assertTrue(line.contains("|-|-|"), line)
        val decoded = LedgerCodec.decode(LedgerCodec.encodeAll(listOf(original)))
        assertIs<LedgerCodec.Decoded.Ok>(decoded)
        assertEquals(original, decoded.entries.single())
    }

    @Test
    fun `a subject may contain separators, newlines, backslashes and emoji`() {
        val subject = "Ada | \uD83D\uDC9B\nline two\r\nC:\\path\\to\\record \\| done"
        val original = entry(subject = subject, key = "key|with|pipes")
        val encoded = LedgerCodec.encodeAll(listOf(original))
        // The on-disk file must still be one line per entry.
        assertEquals(2, encoded.trimEnd('\n').lines().size)
        val decoded = LedgerCodec.decode(encoded)
        assertIs<LedgerCodec.Decoded.Ok>(decoded)
        assertEquals(subject, decoded.entries.single().subject)
        assertEquals("key|with|pipes", decoded.entries.single().idempotencyKey)
        assertEquals(original.hash, decoded.entries.single().hash)
    }

    @Test
    fun `the header is required exactly`() {
        assertIs<LedgerCodec.Decoded.Unreadable>(LedgerCodec.decode("seq|kind\n"))
        assertIs<LedgerCodec.Decoded.Unreadable>(LedgerCodec.decode("#tlc-ledger-v1extra\n"))
        assertIs<LedgerCodec.Decoded.Ok>(LedgerCodec.decode(LedgerCodec.HEADER + "\n"))
    }

    @Test
    fun `unknown kinds and short lines are rejected with a line number`() {
        val bad = LedgerCodec.decode(
            buildString {
                append(LedgerCodec.HEADER).append('\n')
                append("1|BALANCE|ob:1|-|-|").append(Ledger.GENESIS).append('|').append("a".repeat(64)).append('|').append("ob:1").append('\n')
            },
        )
        assertIs<LedgerCodec.Decoded.Unreadable>(bad)
        assertEquals(2, bad.lineNumber)
        assertTrue(bad.reason.contains("BALANCE"))

        val short = LedgerCodec.decode(LedgerCodec.HEADER + "\n1|RECOGNITION\n")
        assertIs<LedgerCodec.Decoded.Unreadable>(short)
        assertEquals(2, short.lineNumber)
    }

    @Test
    fun `a bad number is rejected rather than coerced`() {
        val bad = LedgerCodec.decode(
            LedgerCodec.HEADER + "\nnot-a-number|RECOGNITION|party| - | - |" +
                Ledger.GENESIS + "|" + "b".repeat(64) + "|party\n",
        )
        assertIs<LedgerCodec.Decoded.Unreadable>(bad)
    }

    @Test
    fun `an empty ledger encodes to just the header`() {
        val text = LedgerCodec.encodeAll(emptyList())
        assertEquals("#tlc-ledger-v1\n", text)
        val decoded = LedgerCodec.decode(text)
        assertIs<LedgerCodec.Decoded.Ok>(decoded)
        assertTrue(decoded.entries.isEmpty())
        assertFalse(decoded.entries.any { it.seq < 1 })
    }
}
