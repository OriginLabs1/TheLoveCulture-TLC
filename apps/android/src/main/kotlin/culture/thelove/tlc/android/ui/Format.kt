package culture.thelove.tlc.android.ui

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.abs

/** Presentation-only formatting. The record stores epoch millis; the screen makes them human. */

private val dateTime: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

public fun formatTimestamp(epochMillis: Long): String =
    dateTime.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** "just now", "7 min ago", "yesterday", then a date. Coarse on purpose: a record is not a feed. */
public fun relativeTime(epochMillis: Long, now: Long = System.currentTimeMillis()): String {
    val delta = abs(now - epochMillis)
    val minutes = delta / 60_000
    val hours = delta / 3_600_000
    val days = delta / 86_400_000
    val future = epochMillis > now
    return when {
        minutes < 1 -> if (future) "in a moment" else "just now"
        minutes < 60 -> if (future) "in $minutes min" else "$minutes min ago"
        hours < 24 -> if (future) "in $hours h" else if (hours == 1L) "an hour ago" else "$hours hours ago"
        days == 1L -> if (future) "tomorrow" else "yesterday"
        days < 7 -> if (future) "in $days days" else "$days days ago"
        else -> dateTime.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
    }
}

public fun shortHash(hash: String): String =
    if (hash.length <= 16) hash else "${hash.take(10)}…${hash.takeLast(6)}"

public fun hashPrefix(hash: String): String = hash.take(16)

public fun dayStamp(epochMillis: Long = System.currentTimeMillis()): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT))
