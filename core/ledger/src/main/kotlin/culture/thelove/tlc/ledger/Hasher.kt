package culture.thelove.tlc.ledger

/**
 * SK-09: hashing is a port. The core never links a cipher directly; each platform supplies an
 * audited primitive. A pure-Kotlin implementation exists only in tests, with RFC vectors.
 */
public fun interface Hasher {
    public fun sha256(bytes: ByteArray): ByteArray
}

public object Hex {
    private const val DIGITS = "0123456789abcdef"

    public fun encode(bytes: ByteArray): String {
        val out = StringBuilder(bytes.size * 2)
        for (b in bytes) {
            val v = b.toInt() and 0xff
            out.append(DIGITS[v ushr 4]).append(DIGITS[v and 0x0f])
        }
        return out.toString()
    }

    public fun decode(hex: String): ByteArray {
        require(hex.length % 2 == 0) { "Hex string must have an even length." }
        val out = ByteArray(hex.length / 2)
        for (i in out.indices) {
            val hi = DIGITS.indexOf(hex[i * 2].lowercaseChar())
            val lo = DIGITS.indexOf(hex[i * 2 + 1].lowercaseChar())
            require(hi >= 0 && lo >= 0) { "Invalid hex character at index ${i * 2}." }
            out[i] = ((hi shl 4) or lo).toByte()
        }
        return out
    }
}
