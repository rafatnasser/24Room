package com.rafat.munasabati.smart

import com.rafat.munasabati.model.EventModel
import java.time.*
import kotlin.math.max

/** Pure policy for proactive annual-event suggestions. */
object SmartSuggestionEngine {
    const val SEVEN_DAYS_MINUTES = 7 * 24 * 60

    data class AnnualSuggestion(
        val event: EventModel,
        val nextOccurrenceMillis: Long,
        val daysUntil: Long
    )

    fun nextAnnualOccurrenceMillis(
        eventStartMillis: Long,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val original = Instant.ofEpochMilli(eventStartMillis).atZone(zone)
        val now = Instant.ofEpochMilli(nowMillis).atZone(zone)
        fun candidate(year: Int): ZonedDateTime {
            val ym = YearMonth.of(year, original.monthValue)
            val day = original.dayOfMonth.coerceAtMost(ym.lengthOfMonth())
            return LocalDate.of(year, original.monthValue, day)
                .atTime(original.toLocalTime())
                .atZone(zone)
        }
        var c = candidate(now.year)
        if (c.toInstant().toEpochMilli() <= nowMillis) c = candidate(now.year + 1)
        return c.toInstant().toEpochMilli()
    }

    fun suggestionFor(
        event: EventModel,
        nowMillis: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault()
    ): AnnualSuggestion? {
        if (!event.recurrence.equals("yearly", ignoreCase = true)) return null
        if (event.reminderMinutes >= SEVEN_DAYS_MINUTES || event.remindersCsv.split(',').any { it.trim() == SEVEN_DAYS_MINUTES.toString() }) return null
        val next = nextAnnualOccurrenceMillis(event.startEpochMillis, nowMillis, zone)
        val nowDate = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
        val nextDate = Instant.ofEpochMilli(next).atZone(zone).toLocalDate()
        val days = max(0, Duration.between(nowDate.atStartOfDay(zone), nextDate.atStartOfDay(zone)).toDays())
        if (days !in 0..7) return null
        return AnnualSuggestion(event, next, days)
    }

    fun mergeSevenDayReminder(csv: String): String {
        val values = csv.split(',').mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
        values += SEVEN_DAYS_MINUTES
        return values.sorted().joinToString(",")
    }
}
