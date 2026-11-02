package culture.thelove.tlc.ledger

import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * INV-01/INV-02 — the ledger's vocabulary is a security control. If someone adds
 * `BALANCE`, `ESCROW` or `TRANSFER` to [EntryKind], this test fails and forces the review.
 */
class LedgerContractTest {

    private val hasher = PlatformSha256()

    @Test
    fun `allowed entry kinds are exactly the factual claims TLC may make`() {
        assertEquals(
            listOf(
                "FEE_INTENT",
                "PROOF_RECORDED",
                "ATTESTATION",
                "RECEIPT",
                "WAIVER",
                "GRANT_PLEDGE",
                "GRANT_EVIDENCE",
                "MILESTONE_VERIFIED",
                "CONSENT_GRANTED",
                "CONSENT_REVOKED",
                "RECOGNITION",
            ),
            EntryKind.entries.map { it.name },
        )
    }

    @Test
    fun `no entry kind implies custody or movement of value`() {
        val forbidden = listOf("BALANCE", "ESCROW", "POOL", "TRANSFER", "SETTLE", "WALLET", "PAYOUT", "HOLD")
        EntryKind.entries.forEach { kind ->
            forbidden.forEach { word ->
                assertFalse(kind.name.contains(word), "$kind contains forbidden concept $word")
            }
        }
    }

    @Test
    fun `canonical encoding is unambiguous`() {
        // "a" + "bc" must not hash the same as "ab" + "c".
        assertFalse(Canonical.of("a", "bc").contentEquals(Canonical.of("ab", "c")))
        assertFalse(Canonical.of("a", null).contentEquals(Canonical.of("a", "")))
        assertTrue(Canonical.of("a", "b").contentEquals(Canonical.of("a", "b")))
    }

    @Test
    fun `hashing is stable across runs and platforms`() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            Hex.encode(hasher.sha256("abc".encodeToByteArray())),
        )
        assertEquals(
            LedgerEntry.computeHash(hasher, 1, EntryKind.FEE_INTENT, "ob:1", 100, 1, "k", Ledger.GENESIS),
            LedgerEntry.computeHash(hasher, 1, EntryKind.FEE_INTENT, "ob:1", 100, 1, "k", Ledger.GENESIS),
        )
    }

    @Test
    fun `hex round trips`() {
        val bytes = ByteArray(32) { (it * 7).toByte() }
        assertTrue(Hex.decode(Hex.encode(bytes)).contentEquals(bytes))
    }
}
