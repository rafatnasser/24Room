package com.rafat.munasabati.compat

import android.content.Context
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Non-persistent calendar sources that are merged into the app calendar at render time.
 * Ahl al-Bayt occasions are intentionally never written into the user's local event table.
 */
object UnifiedCalendarSource {
    fun ahlBaytEventsForDate(context: Context, date: LocalDate): List<EventModel> {
        val zone = ZoneId.systemDefault()
        val start = date.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()
        return AhlBaytCalendar.occasionsForDate(context, date).mapIndexed { index, occasion ->
            EventModel(
                id = "ahlbayt:${date}:${occasion.month}:${occasion.day}:$index:${occasion.title.hashCode()}",
                title = occasion.title,
                startEpochMillis = start,
                endEpochMillis = end,
                category = EventCategory.RELIGIOUS,
                reminderMinutes = 0,
                notes = "تقويم أهل البيت عليهم السلام • ${occasion.type}",
                recurrence = "yearly"
            )
        }
    }
}
