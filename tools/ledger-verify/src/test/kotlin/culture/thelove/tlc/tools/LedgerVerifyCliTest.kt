package culture.thelove.tlc.tools

import culture.thelove.tlc.ledger.Ledger
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
}
