package culture.thelove.tlc.android.data

import android.os.Build
import culture.thelove.tlc.ledger.LedgerEntry
import org.json.JSONArray
import org.json.JSONObject

/**
 * The Android side of the Pear/Holepunch handoff.
 *
 * This exports the *shape* of the record — sequence, kind, hash and time — plus the head hash.
 * It deliberately does not export subjects, amounts, notes, photos, receipts, contact details or
 * vault references. The companion node in `peer/` validates this file, appends it to a Hypercore
 * and replicates it to peers that know the same circle code.
 */
public object PeerAdapter {

    public const val FORMAT: String = "tlc.peer.handoff.v1"

    public fun handoff(
        handle: String?,
        displayName: String,
        entries: List<LedgerEntry>,
        headHash: String,
    ): String {
        val rows = JSONArray()
        entries.forEach { entry ->
            rows.put(
                JSONObject()
                    .put("seq", entry.seq)
                    .put("kind", entry.kind.name)
                    .put("hash", entry.hash)
                    .put("occurredAt", entry.occurredAt),
            )
        }
        val device = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .trim()
        return JSONObject()
            .put("format", FORMAT)
            .put("handle", handle.orEmpty())
            .put("display", displayName)
            .put("device", device)
            .put("exportedAt", System.currentTimeMillis())
            .put("headHash", headHash)
            .put("entryCount", entries.size)
            .put("entries", rows)
            .put("note", "TLC record shape only; no subjects, photos or vault references.")
            .toString(2)
    }
}
