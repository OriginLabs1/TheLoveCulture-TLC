package culture.thelove.tlc.experience

import culture.thelove.tlc.domain.Role
import culture.thelove.tlc.ledger.EntryKind
import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerCodec
import culture.thelove.tlc.ledger.LedgerDraft
import culture.thelove.tlc.ledger.adapter.PlatformSha256
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BundleCodecTest {

    private val hasher = PlatformSha256()

    private fun state(): AppSnapshot = AppSnapshot(
        profile = Profile("Ada", "Circle", Role.SINGLE, "0123456789abcdef0123456789abcdef", 1),
        preferences = Preferences(onboarded = true),
        stewards = listOf(Steward("0123456789abcdef0123456789abcdef", "Ada", StewardRole.PRIMARY)),
    )

    private fun record(): String {
        val ledger = Ledger(hasher)
        ledger.append(LedgerDraft(EntryKind.RECOGNITION, "Ada showed up", occurredAt = 10, idempotencyKey = "r1"))
        ledger.append(LedgerDraft(EntryKind.CONSENT_GRANTED, "Circle formed", occurredAt = 20, idempotencyKey = "c1"))
        return LedgerCodec.encodeAll(ledger.entries())
    }

    @Test
    fun `bundle round trips state and a verified record`() {
        val text = BundleCodec.encode(state(), record(), 1000)
        val decoded = BundleCodec.decode(text, hasher)
        assertIs<BundleResult.Ok>(decoded)
        assertEquals(state().profile, decoded.snapshot.profile)
        assertTrue(decoded.record.contains("#tlc-ledger-v1"))
        assertEquals(2, LedgerCodec.decode(decoded.record).let { (it as LedgerCodec.Decoded.Ok).entries.size })
    }

    @Test
    fun `a tampered record makes the whole bundle invalid`() {
        val text = BundleCodec.encode(state(), record(), 1000)
            .replace("Ada showed up", "Ada never showed up")
        val decoded = BundleCodec.decode(text, hasher)
        assertIs<BundleResult.Invalid>(decoded)
        assertTrue(decoded.reason.contains("does not verify"))
    }

    @Test
    fun `an unsupported bundle format is refused`() {
        val text = BundleCodec.encode(state(), record(), 1000).replace("tlc-bundle-v1", "tlc-bundle-v2")
        val decoded = BundleCodec.decode(text, hasher)
        assertIs<BundleResult.Invalid>(decoded)
        assertTrue(decoded.reason.contains("Unsupported backup format"))
    }

    @Test
    fun `oversized bundle is refused before parsing`() {
        val decoded = BundleCodec.decode("x".repeat(BundleCodec.MAX_CHARS + 1), hasher)
        assertIs<BundleResult.Invalid>(decoded)
        assertTrue(decoded.reason.contains("unreasonably large"))
    }
}
