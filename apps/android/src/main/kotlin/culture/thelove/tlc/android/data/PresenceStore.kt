package culture.thelove.tlc.android.data

import android.content.Context

/**
 * Presence consent, stored on the device and defaulting to *nobody*.
 *
 * v0.0.7 is local-first: this record is the person's instruction. It is applied by the shared plane
 * when one is connected and it is honoured by every surface that draws presence. No signal is ever
 * inferred from activity: a hidden signal is returned as "not shared", never as "offline".
 */
public enum class PresenceAudience {
    NOBODY,
    SELECTED,
    CIRCLE,
    EVERYONE,
}

public enum class PresenceSignal {
    ONLINE_NOW,
    LAST_SEEN,
    TYPING,
    READ_RECEIPTS,
}

public data class PresenceSettings(
    public val onlineNow: PresenceAudience = PresenceAudience.NOBODY,
    public val lastSeen: PresenceAudience = PresenceAudience.NOBODY,
    public val typing: PresenceAudience = PresenceAudience.NOBODY,
    public val readReceipts: PresenceAudience = PresenceAudience.NOBODY,
    public val invisible: Boolean = false,
    public val exactLastSeen: Boolean = false,
    public val selectedPeople: Set<String> = emptySet(),
    public val hiddenFrom: Set<String> = emptySet(),
) {
    public fun audience(signal: PresenceSignal): PresenceAudience = when (signal) {
        PresenceSignal.ONLINE_NOW -> onlineNow
        PresenceSignal.LAST_SEEN -> lastSeen
        PresenceSignal.TYPING -> typing
        PresenceSignal.READ_RECEIPTS -> readReceipts
    }

    public fun withAudience(signal: PresenceSignal, audience: PresenceAudience): PresenceSettings = when (signal) {
        PresenceSignal.ONLINE_NOW -> copy(onlineNow = audience)
        PresenceSignal.LAST_SEEN -> copy(lastSeen = audience)
        PresenceSignal.TYPING -> copy(typing = audience)
        PresenceSignal.READ_RECEIPTS -> copy(readReceipts = audience)
    }

    public fun shares(signal: PresenceSignal, viewer: String?): Boolean {
        if (invisible) return false
        val handle = viewer?.lowercase()
        if (handle != null && handle in hiddenFrom) return false
        return when (audience(signal)) {
            PresenceAudience.NOBODY -> false
            PresenceAudience.EVERYONE -> true
            // A null viewer is someone outside the circle (see the live preview).
            PresenceAudience.CIRCLE -> handle != null
            PresenceAudience.SELECTED -> handle != null && handle in selectedPeople
        }
    }

    public companion object {
        /** Every signal starts private. This is the whole point. */
        public val PRIVATE: PresenceSettings = PresenceSettings()
    }
}

public class PresenceStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    public fun load(): PresenceSettings {
        fun audience(key: String, fallback: PresenceAudience): PresenceAudience =
            runCatching { PresenceAudience.valueOf(prefs.getString(key, null) ?: fallback.name) }
                .getOrDefault(fallback)
        return PresenceSettings(
            onlineNow = audience(KEY_ONLINE, PresenceAudience.NOBODY),
            lastSeen = audience(KEY_LAST_SEEN, PresenceAudience.NOBODY),
            typing = audience(KEY_TYPING, PresenceAudience.NOBODY),
            readReceipts = audience(KEY_READS, PresenceAudience.NOBODY),
            invisible = prefs.getBoolean(KEY_INVISIBLE, false),
            exactLastSeen = prefs.getBoolean(KEY_EXACT, false),
            selectedPeople = prefs.getStringSet(KEY_SELECTED, emptySet()).orEmpty()
                .map { it.lowercase() }.toSet(),
            hiddenFrom = prefs.getStringSet(KEY_HIDDEN, emptySet()).orEmpty()
                .map { it.lowercase() }.toSet(),
        )
    }

    public fun save(settings: PresenceSettings) {
        prefs.edit()
            .putString(KEY_ONLINE, settings.onlineNow.name)
            .putString(KEY_LAST_SEEN, settings.lastSeen.name)
            .putString(KEY_TYPING, settings.typing.name)
            .putString(KEY_READS, settings.readReceipts.name)
            .putBoolean(KEY_INVISIBLE, settings.invisible)
            .putBoolean(KEY_EXACT, settings.exactLastSeen)
            .putStringSet(KEY_SELECTED, settings.selectedPeople)
            .putStringSet(KEY_HIDDEN, settings.hiddenFrom)
            .apply()
    }

    public fun reset() {
        prefs.edit().clear().apply()
    }

    private companion object {
        private const val PREFS = "tlc.presence"
        private const val KEY_ONLINE = "online_now"
        private const val KEY_LAST_SEEN = "last_seen"
        private const val KEY_TYPING = "typing"
        private const val KEY_READS = "read_receipts"
        private const val KEY_INVISIBLE = "invisible"
        private const val KEY_EXACT = "exact_last_seen"
        private const val KEY_SELECTED = "selected_people"
        private const val KEY_HIDDEN = "hidden_from"
    }
}
