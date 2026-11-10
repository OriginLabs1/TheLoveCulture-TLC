package culture.thelove.tlc.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedCryptoTest {

    @Test
    fun `a sealed message opens only with the recipient key`() {
        val ada = SharedCrypto.generateKeyPair()
        val grace = SharedCrypto.generateKeyPair()
        val aad = "conv-ada-grace:42"
        val sealed = SharedCrypto.seal(SharedCrypto.encodePublic(ada.public), "hello, live world", aad)

        assertEquals("hello, live world", SharedCrypto.open(ada.private, sealed, aad))
        assertNull(SharedCrypto.open(grace.private, sealed, aad))
    }

    @Test
    fun `a moved or edited envelope fails closed`() {
        val ada = SharedCrypto.generateKeyPair()
        val sealed = SharedCrypto.seal(SharedCrypto.encodePublic(ada.public), "only once", "conv:1")

        assertNull(SharedCrypto.open(ada.private, sealed, "conv:2"))
        val tampered = sealed.ciphertext.let { if (it.first() == 'A') "B${it.drop(1)}" else "A${it.drop(1)}" }
        assertNull(SharedCrypto.open(ada.private, sealed.copy(ciphertext = tampered), "conv:1"))
        assertNull(SharedCrypto.open(ada.private, sealed.copy(nonce = "AAAA"), "conv:1"))
    }

    @Test
    fun `two seals of the same text differ`() {
        val ada = SharedCrypto.generateKeyPair()
        val publicKey = SharedCrypto.encodePublic(ada.public)
        val first = SharedCrypto.seal(publicKey, "same", "aad")
        val second = SharedCrypto.seal(publicKey, "same", "aad")
        assertNotEquals(first.ciphertext, second.ciphertext)
        assertNotEquals(first.ephemeralPublicKey, second.ephemeralPublicKey)
    }

    @Test
    fun `a signature proves the sender and rejects tampering`() {
        val ada = SharedCrypto.generateKeyPair()
        val publicKey = SharedCrypto.encodePublic(ada.public)
        val signature = SharedCrypto.sign(ada.private, "message-body")
        assertTrue(SharedCrypto.verify(publicKey, "message-body", signature))
        assertFalse(SharedCrypto.verify(publicKey, "message-bodX", signature))
    }

    @Test
    fun `a fingerprint is stable and human readable`() {
        val ada = SharedCrypto.generateKeyPair()
        val publicKey = SharedCrypto.encodePublic(ada.public)
        val fingerprint = SharedCrypto.fingerprint(publicKey)
        assertEquals(32, fingerprint.split(":").size)
        assertEquals(fingerprint, SharedCrypto.fingerprint(publicKey))
        assertNotNull(SharedCrypto.decodePublic(publicKey))
    }
}

class PresenceProjectionTest {

    private val now = 1_700_000_000_000L
    private val owner = "ada"
    private val viewer = "grace"

    @Test
    fun `every signal starts at nobody`() {
        val projection = projectPresence(
            consent = PresenceConsent.PRIVATE,
            owner = owner,
            viewer = viewer,
            circle = setOf(viewer),
            online = true,
            lastSeen = now,
            now = now,
        )
        assertNull(projection.onlineNow)
        assertNull(projection.lastSeen)
        assertNull(projection.typing)
        assertNull(projection.readReceipts)
    }

    @Test
    fun `invisible overrides every audience`() {
        val consent = PresenceConsent(
            onlineNow = PresenceAudience.EVERYONE,
            lastSeen = PresenceAudience.EVERYONE,
            typing = PresenceAudience.EVERYONE,
            readReceipts = PresenceAudience.EVERYONE,
            invisible = true,
        )
        val projection = projectPresence(consent, owner, viewer, setOf(viewer), true, now, now)
        assertNull(projection.onlineNow)
        assertNull(projection.lastSeen)
        assertNull(projection.typing)
        assertNull(projection.readReceipts)
    }

    @Test
    fun `selected people are the only people selected can see`() {
        val consent = PresenceConsent(
            onlineNow = PresenceAudience.SELECTED,
            selected = setOf("grace"),
        )
        assertTrue(
            projectPresence(consent, owner, "grace", emptySet(), true, now, now).onlineNow == true,
        )
        assertNull(
            projectPresence(consent, owner, "mallory", setOf("mallory"), true, now, now).onlineNow,
        )
    }

    @Test
    fun `circle means accepted connections only`() {
        val consent = PresenceConsent(onlineNow = PresenceAudience.CIRCLE)
        assertTrue(
            projectPresence(consent, owner, "grace", setOf("grace"), true, now, now).onlineNow == true,
        )
        assertNull(
            projectPresence(consent, owner, "grace", emptySet(), true, now, now).onlineNow,
        )
    }

    @Test
    fun `hidden beats another audience`() {
        val consent = PresenceConsent(
            onlineNow = PresenceAudience.EVERYONE,
            hidden = setOf("grace"),
        )
        assertNull(
            projectPresence(consent, owner, "grace", setOf("grace"), true, now, now).onlineNow,
        )
    }

    @Test
    fun `exact last seen is opt in and never inferred`() {
        val coarseConsent = PresenceConsent(lastSeen = PresenceAudience.EVERYONE)
        val coarse = projectPresence(coarseConsent, owner, viewer, emptySet(), false, now - 1000, now)
        assertEquals(true, coarse.lastSeenCoarse)
        assertNotEquals(now - 1000, coarse.lastSeen)

        val exactConsent = coarseConsent.copy(exactLastSeen = true)
        val exact = projectPresence(exactConsent, owner, viewer, emptySet(), false, now - 1000, now)
        assertEquals(false, exact.lastSeenCoarse)
        assertEquals(now - 1000, exact.lastSeen)
    }
}
