package com.rafat.munasabati.compat

import android.content.Context
import android.icu.util.Calendar as IcuCalendar
import android.icu.util.IslamicCalendar
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
        val midnight = date.atStartOfDay(zone).toInstant().toEpochMilli()
        val hijri = IslamicCalendar().apply { timeInMillis = midnight }
        val hijriMonth = hijri.get(IcuCalendar.MONTH) + 1
        val hijriDay = hijri.get(IcuCalendar.DAY_OF_MONTH)

        // Calendar visibility is independent from the master reminder switch. Category
        // switches still control which Ahl al-Bayt groups the user wants to see.
        val occasions = AhlBaytCalendar.occasions.filter { occasion ->
            occasion.month == hijriMonth &&
                occasion.day == hijriDay &&
                AhlBaytCalendar.isCategoryEnabled(context, AhlBaytCalendar.categoryOf(occasion))
        }

        val start = date.atTime(LocalTime.of(8, 0)).atZone(zone).toInstant().toEpochMilli()
        val end = date.atTime(LocalTime.of(9, 0)).atZone(zone).toInstant().toEpochMilli()
        return occasions.mapIndexed { index, occasion ->
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
