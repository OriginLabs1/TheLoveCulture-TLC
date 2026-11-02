package culture.thelove.tlc.experience

import culture.thelove.tlc.ledger.Hasher
import culture.thelove.tlc.ledger.Ledger
import culture.thelove.tlc.ledger.LedgerCodec
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The portable backup: app state plus the record, in one file.
 *
 * Import is deliberately strict. A bundle is accepted only when its state is valid and its record
 * re-verifies from the hashes alone. A backup that cannot prove itself is not a backup (INV-08).
 */
public sealed interface BundleResult {
    public data class Ok(
        public val snapshot: AppSnapshot,
        public val record: String,
        public val exportedAt: Long,
    ) : BundleResult

    public data class Invalid(public val reason: String) : BundleResult
}

public object BundleCodec {

    public const val FORMAT: String = "tlc-bundle-v1"
    public const val MAX_CHARS: Int = 8_000_000

    public fun encode(snapshot: AppSnapshot, recordText: String, exportedAt: Long): String {
        val state = Json.parseToJsonElement(StateCodec.encode(snapshot))
        return buildJsonObject {
            put("format", FORMAT)
            put("exported_at", exportedAt)
            put("state", state)
            put("record", recordText)
        }.toString()
    }

    public fun decode(text: String, hasher: Hasher): BundleResult {
        if (text.length > MAX_CHARS) return BundleResult.Invalid("The backup is unreasonably large.")
        return try {
            val root = Json.parseToJsonElement(text) as? JsonObject
                ?: return BundleResult.Invalid("The backup is not a JSON object.")
            val format = root["format"]?.jsonPrimitive?.contentOrNull
            if (format != FORMAT) return BundleResult.Invalid("Unsupported backup format: $format")

            val state = root["state"] as? JsonObject
                ?: return BundleResult.Invalid("The backup contains no app state.")
            val snapshot = when (val decoded = StateCodec.decodeObject(state)) {
                is StateResult.Ok -> decoded.snapshot
                is StateResult.Invalid -> return BundleResult.Invalid(decoded.reason)
            }

            val recordText = root["record"]?.jsonPrimitive?.contentOrNull
                ?: return BundleResult.Invalid("The backup contains no record.")
            val entries = when (val decoded = LedgerCodec.decode(recordText)) {
                is LedgerCodec.Decoded.Ok -> decoded.entries
                is LedgerCodec.Decoded.Unreadable ->
                    return BundleResult.Invalid("The record is malformed: ${decoded.reason}")
            }
            if (entries.isNotEmpty() && !Ledger.verify(entries, hasher).ok) {
                return BundleResult.Invalid("The record in this backup does not verify.")
            }
            val exportedAt = root["exported_at"]?.jsonPrimitive?.longOrNull ?: 0L
            BundleResult.Ok(
                snapshot = snapshot,
                // Canonicalise: what is imported is exactly what a verifier would read.
                record = LedgerCodec.encodeAll(entries),
                exportedAt = exportedAt,
            )
        } catch (e: Exception) {
            BundleResult.Invalid(e.message ?: "The backup could not be read.")
        }
    }
}
