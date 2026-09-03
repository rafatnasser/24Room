package com.rafat.munasabati.compat

import android.content.Context
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

object AhlBaytCalendarEventBridge {
    private const val ID_PREFIX = "ahlbayt:"

    fun eventsForDate(context:Context,date:LocalDate):List<EventModel>{
        val zone=ZoneId.systemDefault()
        val start=date.atStartOfDay(zone).toInstant().toEpochMilli()
        val end=date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return AhlBaytCalendar.occasionsForDate(context,date).mapIndexed{index,occasion->
            EventModel(
                id="$ID_PREFIX$date:${occasion.month}:${occasion.day}:${occasion.title.hashCode()}:$index",
                title=occasion.title,
                startEpochMillis=start,
                endEpochMillis=end,
                category=EventCategory.RELIGIOUS
            )
        }
    }

    fun eventsForMonth(context:Context,month:YearMonth):List<EventModel> =
        (1..month.lengthOfMonth()).flatMap{eventsForDate(context,month.atDay(it))}

    fun isAhlBaytEvent(event:EventModel):Boolean=event.id.startsWith(ID_PREFIX)
}
