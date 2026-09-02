package com.rafat.munasabati.compat

import android.content.Context
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import com.rafat.munasabati.model.EventStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * Read-only projection used by the v5 calendar UI.
 *
 * Ahl al-Bayt occasions are intentionally NOT persisted in the Room events table.
 * They are projected into the calendar at render time so they can never be confused
 * with local user events or external Android calendar imports.
 */
object UnifiedCalendarProjection {
    private const val AHL_PREFIX = "ahlbayt:"

    fun isAhlBaytEvent(event: EventModel): Boolean = event.id.startsWith(AHL_PREFIX)

    fun ahlBaytEvents(context: Context, date: LocalDate): List<EventModel> {
        val zone = ZoneId.systemDefault()
        val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1L
        return runCatching { AhlBaytCalendar.occasionsForDate(context, date) }
            .getOrDefault(emptyList())
            .mapIndexed { index, occasion ->
                EventModel(
                    id = "$AHL_PREFIX$date:${occasion.month}:${occasion.day}:$index:${occasion.title.hashCode()}",
                    title = occasion.title,
                    startEpochMillis = start,
                    endEpochMillis = end,
                    category = EventCategory.RELIGIOUS,
                    importance = 2,
                    notes = occasion.type,
                    status = EventStatus.UPCOMING,
                    reminderMinutes = 0,
                    recurrence = "yearly",
                    calendarFingerprint = "ahlbayt:${occasion.month}:${occasion.day}:${occasion.title.hashCode()}"
                )
            }
    }

    fun eventsForDate(context: Context, date: LocalDate, storedEvents: List<EventModel>): List<EventModel> {
        val zone = ZoneId.systemDefault()
        val local = storedEvents.filter { event ->
            event.status != EventStatus.CANCELLED &&
                Instant.ofEpochMilli(event.startEpochMillis).atZone(zone).toLocalDate() == date
        }
        return (local + ahlBaytEvents(context, date)).sortedBy { it.startEpochMillis }
    }

    fun eventsForMonth(context: Context, month: YearMonth, storedEvents: List<EventModel>): List<EventModel> {
        val zone = ZoneId.systemDefault()
        val local = storedEvents.filter { event ->
            val date = Instant.ofEpochMilli(event.startEpochMillis).atZone(zone).toLocalDate()
            event.status != EventStatus.CANCELLED && YearMonth.from(date) == month
        }
        val religious = (1..month.lengthOfMonth()).flatMap { day ->
            ahlBaytEvents(context, month.atDay(day))
        }
        return (local + religious).sortedBy { it.startEpochMillis }
    }
}
