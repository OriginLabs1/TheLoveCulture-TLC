package culture.thelove.tlc.android.data

import android.content.Context
import culture.thelove.tlc.domain.Handle
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The local door: a distinct username and a word/phrase passphrase.
 *
 * This is deliberately *not* biometric authentication. There is no fingerprint prompt, no face
 * prompt, no passkey and no device-credential shortcut anywhere in the flow. The passphrase is the
 * only way in, and it is never stored: the device keeps a salted PBKDF2 verifier and compares it in
 * constant time. The passphrase never leaves the device, never enters the record and never appears
 * in a log.
 *
 * The v0.0.7 shell is local-first. The verifier protects the door on this device. When the shared
 * plane arrives, the same passphrase feeds an OPAQUE/Argon2id verifier at the relay through
 * `IdentityPort`; the UI and this store do not change shape.
 */
public class IdentityStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    public data class Identity(
        public val username: String,
        public val displayName: String,
        public val createdAt: Long,
        public val wordCount: Int,
    )

    public fun identity(): Identity? {
        val username = prefs.getString(KEY_USERNAME, null) ?: return null
        val salt = prefs.getString(KEY_SALT, null) ?: return null
        val verifier = prefs.getString(KEY_VERIFIER, null) ?: return null
        if (salt.isBlank() || verifier.isBlank()) return null
        return Identity(
            username = username,
            displayName = prefs.getString(KEY_DISPLAY, username).orEmpty(),
            createdAt = prefs.getLong(KEY_CREATED, 0L),
            wordCount = prefs.getInt(KEY_WORDS, 0),
        )
    }

    public fun create(username: String, displayName: String, passphrase: String): IdentityError? {
        val clean = normalizeUsername(username) ?: return IdentityError.INVALID_USERNAME
        if (Handle.isReserved(clean)) return IdentityError.RESERVED_USERNAME
        passphraseProblem(passphrase)?.let { return it }

        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val verifier = derive(passphrase, salt)
        val written = prefs.edit()
            .putString(KEY_USERNAME, clean)
            .putString(KEY_DISPLAY, displayName.trim().ifBlank { clean })
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_VERIFIER, Base64.getEncoder().encodeToString(verifier))
            .putLong(KEY_CREATED, System.currentTimeMillis())
            .putInt(KEY_WORDS, wordCount(passphrase))
            .commit()
        return if (written) null else IdentityError.STORAGE
    }

    /** Verify a username and passphrase without revealing which half was wrong. */
    public fun verify(username: String, passphrase: String): Boolean {
        val identity = identity() ?: return false
        if (normalizeUsername(username) != identity.username) return false
        val salt = decode(prefs.getString(KEY_SALT, null)) ?: return false
        val expected = decode(prefs.getString(KEY_VERIFIER, null)) ?: return false
        val candidate = derive(passphrase, salt)
        return MessageDigest.isEqual(expected, candidate)
    }

    public fun changePassphrase(username: String, current: String, next: String): IdentityError? {
        if (!verify(username, current)) return IdentityError.WRONG_PASSPHRASE
        passphraseProblem(next)?.let { return it }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val verifier = derive(next, salt)
        val written = prefs.edit()
            .putString(KEY_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(KEY_VERIFIER, Base64.getEncoder().encodeToString(verifier))
            .putInt(KEY_WORDS, wordCount(next))
            .commit()
        return if (written) null else IdentityError.STORAGE
    }

    /** Remove the local door. The private record on the device is not touched. */
    public fun clear() {
        prefs.edit().clear().commit()
    }

    private fun derive(passphrase: String, salt: ByteArray): ByteArray {
        val chars = passphrase.toCharArray()
        return try {
            val spec = PBEKeySpec(chars, salt, ITERATIONS, KEY_BITS)
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            chars.fill('\u0000')
        }
    }

    private fun decode(value: String?): ByteArray? =
        runCatching { Base64.getDecoder().decode(value ?: return null) }.getOrNull()

    public companion object {
        private const val PREFS = "tlc.identity"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY = "display_name"
        private const val KEY_SALT = "salt"
        private const val KEY_VERIFIER = "verifier"
        private const val KEY_CREATED = "created_at"
        private const val KEY_WORDS = "word_count"
        private const val SALT_BYTES = 32
        private const val ITERATIONS = 210_000
        private const val KEY_BITS = 256
        /** Lower-case, trim, allow a leading @ and use the same grammar as every other handle. */
        public fun normalizeUsername(raw: String): String? = Handle.parse(raw)?.value

        /** Number of whitespace-separated words; used for the honest strength guide. */
        public fun wordCount(passphrase: String): Int =
            passphrase.trim().split(Regex("\\s+")).count { it.isNotBlank() }

        /**
         * A word/phrase is strong enough when it carries at least four words and twelve characters,
         * or a single long line of twenty characters or more. No composition rules, no shaming.
         */
        public fun passphraseProblem(passphrase: String): IdentityError? {
            val clean = passphrase.trim()
            if (clean.length < 12) return IdentityError.WEAK_PASSPHRASE
            val words = wordCount(clean)
            if (words < 4 && clean.length < 20) return IdentityError.WEAK_PASSPHRASE
            if (clean.lowercase().replace(" ", "").length < 8) return IdentityError.WEAK_PASSPHRASE
            return null
        }

        /** 0..4, for the strength meter. Honest labels, never a fake score out of 100. */
        public fun strength(passphrase: String): Int {
            val clean = passphrase.trim()
            if (clean.isEmpty()) return 0
            val words = wordCount(clean)
            return when {
                clean.length >= 24 && words >= 5 -> 4
                words >= 4 && clean.length >= 12 -> 3
                clean.length >= 16 -> 2
                else -> 1
            }
        }
    }
}

/** Failures the UI maps to strings; no user-facing copy lives in the data layer. */
public enum class IdentityError {
    INVALID_USERNAME,
    RESERVED_USERNAME,
    WEAK_PASSPHRASE,
    WRONG_PASSPHRASE,
    NO_IDENTITY,
    STORAGE,
}
