package culture.thelove.tlc.ledger

/**
 * Deterministic, delimiter-free canonical encoding.
 *
 * Every field is length-prefixed so that no value can smuggle a separator into another field
 * (`"a|b"` and `"a" "b"` must hash differently). Lengths are counted in UTF-8 bytes, never chars.
 * A null field is encoded as `-1:` and is therefore distinct from the empty string.
 */
public object Canonical {

    public fun of(vararg fields: String?): ByteArray {
        val out = ArrayList<Byte>(64)
        for (field in fields) {
            if (field == null) {
                out.addAll("-1:".encodeToByteArray().asList())
                continue
            }
            val bytes = field.encodeToByteArray()
            out.addAll(bytes.size.toString().encodeToByteArray().asList())
            out.add(':'.code.toByte())
            out.addAll(bytes.asList())
        }
        return out.toByteArray()
    }
}
