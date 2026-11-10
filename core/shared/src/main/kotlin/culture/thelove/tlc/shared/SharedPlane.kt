package culture.thelove.tlc.shared

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.PublicKey
import java.security.SecureRandom
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The cryptographic half of the shared plane.
 *
 * Two P-256 key pairs live on each device: one signs, one receives encrypted envelopes. Messages
 * use an ephemeral-static ECDH (ECIES) construction: the sender generates a fresh P-256 key for
 * every message, derives an AES-256-GCM key with HKDF-SHA256, then sends the ephemeral public key,
 * the nonce and the ciphertext. The relay only ever stores those three fields plus routing
 * metadata, so it cannot read a message.
 *
 * This is deliberately built from audited JCA primitives rather than a hand-written ratchet. It is
 * honest E2EE for one-to-one text and is labelled as such in the app. It does not claim forward
 * secrecy across a long session; the next milestone may replace this port with MLS.
 */
public object SharedCrypto {

    private const val CURVE = "secp256r1"
    private const val KEY_ALGORITHM = "EC"
    private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val ECDH_ALGORITHM = "ECDH"
    private const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_TAG_BITS = 128
    private const val NONCE_BYTES = 12
    private const val SHARED_SECRET_BYTES = 32
    private const val HKDF_INFO = "tlc-shared-plane-v1"

    private val random = SecureRandom()

    public fun generateKeyPair(): KeyPair {
        val generator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        generator.initialize(ECGenParameterSpec(CURVE))
        return generator.generateKeyPair()
    }

    public fun encodePublic(key: PublicKey): String =
        Base64.getEncoder().encodeToString(key.encoded)

    public fun encodePrivate(key: PrivateKey): String =
        Base64.getEncoder().encodeToString(key.encoded)

    public fun decodePublic(value: String): PublicKey =
        KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePublic(X509EncodedKeySpec(Base64.getDecoder().decode(value)))

    public fun decodePrivate(value: String): PrivateKey =
        KeyFactory.getInstance(KEY_ALGORITHM)
            .generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(value)))

    public fun sign(privateKey: PrivateKey, data: String): String {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(privateKey)
        signer.update(data.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(signer.sign())
    }

    public fun verify(publicKey: String, data: String, signature: String): Boolean =
        runCatching {
            val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
            verifier.initVerify(decodePublic(publicKey))
            verifier.update(data.toByteArray(Charsets.UTF_8))
            verifier.verify(Base64.getDecoder().decode(signature))
        }.getOrDefault(false)

    /**
     * Seal a message for one recipient. The result is everything the recipient needs except their
     * own private key. [aad] binds the routing metadata so a relay cannot move an envelope to a
     * different conversation without the open failing.
     */
    public fun seal(recipientPublicKey: String, plaintext: String, aad: String): SealedMessage {
        val ephemeral = generateKeyPair()
        val recipient = decodePublic(recipientPublicKey)
        val secret = deriveSecret(ephemeral.private, recipient, aad)
        val nonce = ByteArray(NONCE_BYTES).also { random.nextBytes(it) }
        val cipher = Cipher.getInstance(AES_TRANSFORMATION)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(secret, "AES"),
            GCMParameterSpec(GCM_TAG_BITS, nonce),
        )
        cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return SealedMessage(
            ephemeralPublicKey = encodePublic(ephemeral.public),
            nonce = Base64.getEncoder().encodeToString(nonce),
            ciphertext = Base64.getEncoder().encodeToString(ciphertext),
        )
    }

    /** Open an envelope with the recipient's static private key. Null means it did not verify. */
    public fun open(privateKey: PrivateKey, sealed: SealedMessage, aad: String): String? =
        runCatching {
            val ephemeral = decodePublic(sealed.ephemeralPublicKey)
            val secret = deriveSecret(privateKey, ephemeral, aad)
            val nonce = Base64.getDecoder().decode(sealed.nonce)
            if (nonce.size != NONCE_BYTES) return null
            val cipher = Cipher.getInstance(AES_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(secret, "AES"),
                GCMParameterSpec(GCM_TAG_BITS, nonce),
            )
            cipher.updateAAD(aad.toByteArray(Charsets.UTF_8))
            String(cipher.doFinal(Base64.getDecoder().decode(sealed.ciphertext)), Charsets.UTF_8)
        }.getOrNull()

    /** SHA-256 fingerprint, grouped for a human to compare. Never a secret. */
    public fun fingerprint(publicKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(Base64.getDecoder().decode(publicKey))
        return digest.joinToString(":") { "%02X".format(it) }
    }

    private fun deriveSecret(privateKey: PrivateKey, publicKey: PublicKey, aad: String): ByteArray {
        val agreement = KeyAgreement.getInstance(ECDH_ALGORITHM)
        agreement.init(privateKey)
        agreement.doPhase(publicKey, true)
        val shared = agreement.generateSecret()
        return hkdf(shared, aad.toByteArray(Charsets.UTF_8), SHARED_SECRET_BYTES)
    }

    /** RFC 5869 HKDF-SHA256, salt = aad, info = a fixed protocol label. */
    private fun hkdf(input: ByteArray, salt: ByteArray, length: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(if (salt.isEmpty()) ByteArray(32) else salt, "HmacSHA256"))
        val prk = mac.doFinal(input)
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        mac.update(HKDF_INFO.toByteArray(Charsets.UTF_8))
        mac.update(0x01)
        val block = mac.doFinal()
        return block.copyOf(length)
    }
}

/** The wire shape of one encrypted message. Contains no plaintext and no sender identity. */
public data class SealedMessage(
    public val ephemeralPublicKey: String,
    public val nonce: String,
    public val ciphertext: String,
)

/**
 * The four presence signals and the four audiences. The relay applies this exact policy; the app
 * keeps the same shape so the preview cannot disagree with the live projection.
 */
public enum class PresenceAudience {
    NOBODY,
    SELECTED,
    CIRCLE,
    EVERYONE,
}

public data class PresenceConsent(
    public val onlineNow: PresenceAudience = PresenceAudience.NOBODY,
    public val lastSeen: PresenceAudience = PresenceAudience.NOBODY,
    public val typing: PresenceAudience = PresenceAudience.NOBODY,
    public val readReceipts: PresenceAudience = PresenceAudience.NOBODY,
    public val invisible: Boolean = false,
    public val exactLastSeen: Boolean = false,
    public val selected: Set<String> = emptySet(),
    public val hidden: Set<String> = emptySet(),
) {
    public companion object {
        public val PRIVATE: PresenceConsent = PresenceConsent()
    }
}

/**
 * What one viewer is allowed to know about one owner. A hidden signal is always
 * [PresenceAudience.NOBODY] — the relay never substitutes "offline" for "not shared".
 */
public data class PresenceProjection(
    public val onlineNow: Boolean?,
    public val lastSeen: Long?,
    public val lastSeenCoarse: Boolean,
    public val typing: Boolean?,
    public val readReceipts: Boolean?,
)

/**
 * The single presence rule used by the relay and mirrored by the app's preview.
 *
 * [viewer] is the viewer's handle, [owner] is the person whose presence is being projected,
 * [circle] contains the handles with an accepted connection, and [lastSeen] is the owner's last
 * accepted heartbeat. A viewer outside the circle sees nothing even when the audience is CIRCLE.
 */
public fun projectPresence(
    consent: PresenceConsent,
    owner: String,
    viewer: String,
    circle: Set<String>,
    online: Boolean,
    lastSeen: Long,
    now: Long,
): PresenceProjection {
    val normalizedViewer = viewer.lowercase()
    fun visible(audience: PresenceAudience): Boolean {
        if (consent.invisible) return false
        if (normalizedViewer in consent.hidden) return false
        return when (audience) {
            PresenceAudience.NOBODY -> false
            PresenceAudience.EVERYONE -> true
            PresenceAudience.CIRCLE -> normalizedViewer in circle
            PresenceAudience.SELECTED -> normalizedViewer in consent.selected
        }
    }

    val onlineVisible = visible(consent.onlineNow)
    val lastVisible = visible(consent.lastSeen)
    val exact = consent.exactLastSeen
    return PresenceProjection(
        onlineNow = if (onlineVisible) online else null,
        lastSeen = if (lastVisible) {
            if (exact) lastSeen else coarse(lastSeen, now)
        } else {
            null
        },
        lastSeenCoarse = lastVisible && !exact,
        typing = if (visible(consent.typing)) false else null,
        readReceipts = if (visible(consent.readReceipts)) false else null,
    )
}

/** Coarse windows: the other side never gets a timestamp when exact last seen is off. */
public fun coarse(lastSeen: Long, now: Long): Long {
    val elapsed = (now - lastSeen).coerceAtLeast(0)
    val hour = 60 * 60 * 1000L
    val day = 24 * hour
    return when {
        elapsed < hour -> now / hour * hour
        elapsed < day -> now / day * day
        elapsed < 7 * day -> now / (7 * day) * (7 * day)
        else -> now / (30 * day) * (30 * day)
    }
}

/** A connection is live only when both people accepted; a request alone shares nothing. */
public enum class ConnectionState {
    NONE,
    REQUESTED,
    PENDING,
    ACCEPTED,
    DECLINED,
    BLOCKED,
}
