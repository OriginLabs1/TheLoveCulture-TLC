package culture.thelove.tlc.ledger

/**
 * The on-disk/on-wire form of the ledger: one header line, then one line per entry.
 *
 * Why a text format rather than a database or a binary blob:
 *  - a person can open the file and read their own record without this app;
 *  - a reviewer can diff it, and the `ledger-verify` CLI can check it;
 *  - a partial write is visible (the last line is incomplete) instead of silently corrupt.
 *
 * Layout (fields separated by `|`, subject last so it may contain `|`):
 *
 * ```
 * #tlc-ledger-v1
 * seq|kind|occurredAt|amountKobo|idempotencyKey|prevHash|hash|subject
 * ```
 *
 * `-` means "absent" for the nullable fields. `%`, `|`, newline and carriage return are
 * percent-encoded in the free-text fields (`%25`, `%7C`, `%0A`, `%0D`), so no value can introduce
 * a separator; everything else is stored verbatim, including non-ASCII.
 */
public object LedgerCodec {

    public const val HEADER: String = "#tlc-ledger-v1"

    private const val FIELD_SEPARATOR = '|'
    private const val ABSENT = "-"
    private const val FIELD_COUNT = 8

    public fun encode(entry: LedgerEntry): String = buildString(160) {
        append(entry.seq)
        append(FIELD_SEPARATOR)
        append(entry.kind.name)
        append(FIELD_SEPARATOR)
        append(entry.occurredAt)
        append(FIELD_SEPARATOR)
        append(entry.amountKobo?.toString() ?: ABSENT)
        append(FIELD_SEPARATOR)
        append(entry.idempotencyKey?.let(::escape) ?: ABSENT)
        append(FIELD_SEPARATOR)
        append(entry.prevHash)
        append(FIELD_SEPARATOR)
        append(entry.hash)
        append(FIELD_SEPARATOR)
        append(escape(entry.subject))
    }

    /** The whole log as it is written to storage. Ends with a newline, or is just the header. */
    public fun encodeAll(entries: List<LedgerEntry>): String = buildString {
        append(HEADER).append('\n')
        entries.forEach { append(encode(it)).append('\n') }
    }

    public sealed interface Decoded {
        public data class Ok(public val entries: List<LedgerEntry>, public val trailingNewline: Boolean) : Decoded

        /** The file cannot be trusted at all (wrong header, unreadable line). */
        public data class Unreadable(public val lineNumber: Int, public val reason: String) : Decoded
    }

    public fun decode(text: String): Decoded {
        val firstLine = text.substringBefore('\n')
        if (firstLine != HEADER) {
            return Decoded.Unreadable(1, "missing $HEADER header")
        }
        val trailingNewline = text.endsWith('\n')
        val lines = text.split('\n')
        // lines[0] is the header; a trailing newline produces one empty final element.
        val content = lines.drop(1).dropLastWhile { it.isEmpty() }
        val entries = ArrayList<LedgerEntry>(content.size)
        content.forEachIndexed { index, line ->
            val parsed = decodeLine(line)
            when (parsed) {
                is LineResult.Bad -> return Decoded.Unreadable(index + 2, parsed.reason)
                is LineResult.Good -> entries += parsed.entry
            }
        }
        return Decoded.Ok(entries, trailingNewline)
    }

    private sealed interface LineResult {
        data class Good(val entry: LedgerEntry) : LineResult

        data class Bad(val reason: String) : LineResult
    }

    private fun decodeLine(line: String): LineResult {
        val parts = line.split(FIELD_SEPARATOR, limit = FIELD_COUNT)
        if (parts.size != FIELD_COUNT) {
            return LineResult.Bad("expected $FIELD_COUNT fields, found ${parts.size}")
        }
        val seq = parts[0].toLongOrNull() ?: return LineResult.Bad("bad sequence number")
        val kind = EntryKind.entries.firstOrNull { it.name == parts[1] }
            ?: return LineResult.Bad("unknown entry kind '${parts[1]}'")
        val occurredAt = parts[2].toLongOrNull() ?: return LineResult.Bad("bad timestamp")
        val amount = if (parts[3] == ABSENT) null else parts[3].toLongOrNull()
            ?: return LineResult.Bad("bad amount")
        val key = if (parts[4] == ABSENT) null else unescape(parts[4])
        val prevHash = parts[5]
        val hash = parts[6]
        val subject = unescape(parts[7])
        return try {
            LineResult.Good(
                LedgerEntry(
                    seq = seq,
                    kind = kind,
                    subject = subject,
                    amountKobo = amount,
                    occurredAt = occurredAt,
                    idempotencyKey = key,
                    prevHash = prevHash,
                    hash = hash,
                ),
            )
        } catch (e: IllegalArgumentException) {
            LineResult.Bad(e.message ?: "invalid entry")
        }
    }

    private fun escape(value: String): String {
        if (value.none { it == '%' || it == '|' || it == '\n' || it == '\r' }) return value
        return buildString(value.length + 8) {
            value.forEach { c ->
                when (c) {
                    '%' -> append("%25")
                    '|' -> append("%7C")
                    '\n' -> append("%0A")
                    '\r' -> append("%0D")
                    else -> append(c)
                }
            }
        }
    }

    private fun unescape(value: String): String {
        if (value.indexOf('%') < 0) return value
        return buildString(value.length) {
            var i = 0
            while (i < value.length) {
                val c = value[i]
                if (c == '%' && i + 2 < value.length) {
                    when (value.substring(i + 1, i + 3).uppercase()) {
                        "25" -> { append('%'); i += 3 }
                        "7C" -> { append('|'); i += 3 }
                        "0A" -> { append('\n'); i += 3 }
                        "0D" -> { append('\r'); i += 3 }
                        else -> { append(c); i++ }
                    }
                } else {
                    append(c)
                    i++
                }
            }
        }
    }
}
