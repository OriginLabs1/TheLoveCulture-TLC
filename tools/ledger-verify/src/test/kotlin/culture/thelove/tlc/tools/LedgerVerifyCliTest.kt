package culture.thelove.tlc.tools

import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerCodec
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The verifier itself must be trustworthy: exit codes are the CI contract. */
class LedgerVerifyCliTest {

    @Test
    fun `demo exits zero when invariants hold`() {
        assertEquals(0, run(arrayOf("--demo")))
    }

    @Test
    fun `no arguments is a usage error`() {
        assertEquals(2, run(emptyArray()))
    }

    @Test
    fun `help exits zero`() {
        assertEquals(0, run(arrayOf("--help")))
    }

    @Test
    fun `malformed rows are rejected`() {
        assertFailsWith<IllegalArgumentException> { parse(listOf("1\tFEE_INTENT\tonly-three")) }
    }

    @Test
    fun `a tampered export fails verification`() {
        val header = "0".repeat(64)
        val bogus = "x".repeat(64)
        val entries = parse(listOf("1\tFEE_INTENT\tob:1\t100\t1\t-\t$header\t$bogus"))
        assertEquals(1, entries.size)
        assertFalse(Ledger.verify(entries, PlatformSha256()).ok)
    }

    @Test
    fun `anchor parsing is strict`() {
        assertFailsWith<IllegalArgumentException> { parseAnchor("1:abc") }
        assertTrue(parseAnchor("1:${"a".repeat(64)}:${"b".repeat(64)}:5").length == 1)
    }

    @Test
    fun `an app export verifies from a file`() {
        val file = java.nio.file.Files.createTempFile("tlc-export", ".log").toFile()
        try {
            // Build a genuinely valid single-entry export so the CLI has to accept it.
            val ledger = Ledger(PlatformSha256())
            ledger.append(
                culture.thelove.tlc.ledger.LedgerDraft(
                    culture.thelove.tlc.ledger.EntryKind.RECOGNITION,
                    "party:alice",
                    occurredAt = 1L,
                ),
            )
            file.writeText(LedgerCodec.encodeAll(ledger.entries()))
            assertEquals(0, run(arrayOf("--file", file.path)))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `an edited app export fails from a file`() {
        val hasher = PlatformSha256()
        val ledger = Ledger(hasher)
        ledger.append(
            culture.thelove.tlc.ledger.LedgerDraft(
                culture.thelove.tlc.ledger.EntryKind.RECOGNITION,
                "party:alice",
                occurredAt = 1L,
            ),
        )
        val file = java.nio.file.Files.createTempFile("tlc-export", ".log").toFile()
        try {
            file.writeText(LedgerCodec.encodeAll(ledger.entries()).replace("party:alice", "party:mallory"))
            assertEquals(1, run(arrayOf("--file", file.path)))
        } finally {
            file.delete()
        }
    }

    @Test
    fun `an app export with an unknown kind is a usage error`() {
        val file = java.nio.file.Files.createTempFile("tlc-export", ".log").toFile()
        try {
            file.writeText(LedgerCodec.HEADER + "\n1|TRANSFER|x|-|-|" + "0".repeat(64) + "|" + "a".repeat(64) + "|x\n")
            assertEquals(2, run(arrayOf("--file", file.path)))
        } finally {
            file.delete()
        }
    }
}
