package com.rafat.munasabati.compat

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.rafat.munasabati.MunasabatiApp
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class V530CalendarIntegrationTest {
    private lateinit var context:Context
    private lateinit var app:MunasabatiApp
    private val calendars=mutableListOf<Long>()

    @Before fun setup(){
        context=ApplicationProvider.getApplicationContext()
        app=context.applicationContext as MunasabatiApp
        val ui=InstrumentationRegistry.getInstrumentation().uiAutomation
        ui.grantRuntimePermission(context.packageName,Manifest.permission.READ_CALENDAR)
        ui.grantRuntimePermission(context.packageName,Manifest.permission.WRITE_CALENDAR)
    }

    @After fun cleanup(){
        calendars.forEach{id->
            runCatching{CalendarSyncManager(context,app.repository).disconnectCalendar(id)}
            val uri=CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,"true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME,"munasabati-v530-test")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()
            runCatching{context.contentResolver.delete(ContentUris.withAppendedId(uri,id),null,null)}
        }
    }

    @Test fun expiredCleanup_neverDeletesUnprovenLocalHistory(){
        val now=System.currentTimeMillis()
        val id="v530-local-history-${System.nanoTime()}"
        val local=EventModel(
            id=id,
            title="Local previous event must survive",
            startEpochMillis=now-3*86_400_000L,
            endEpochMillis=now-3*86_400_000L+3_600_000L,
            category=EventCategory.OTHER
        )
        app.repository.upsertEvent(local)
        CalendarSyncManager(context,app.repository).cleanupExpiredCalendarEvents(now)
        assertNotNull(app.repository.allEvents().firstOrNull{it.id==id})
        app.repository.deleteEvent(id)
    }

    @Test fun ahlBaytOccasions_areBridgedIntoCalendarEvents(){
        AhlBaytCalendar.setEnabled(context,true)
        AhlBaytCategory.values().forEach{AhlBaytCalendar.setCategoryEnabled(context,it,true)}
        val date=(0L..400L).asSequence().map{LocalDate.now().plusDays(it)}
            .first{AhlBaytCalendar.occasionsForDate(context,it).isNotEmpty()}
        val events=AhlBaytCalendarEventBridge.eventsForDate(context,date)
        assertTrue(events.isNotEmpty())
        assertTrue(events.all{it.category==EventCategory.RELIGIOUS})
        assertTrue(events.all{AhlBaytCalendarEventBridge.isAhlBaytEvent(it)})
        assertTrue(AhlBaytCalendarEventBridge.eventsForMonth(context,java.time.YearMonth.from(date)).any{it.startEpochMillis==events.first().startEpochMillis})
    }

    @Test fun recurringCalendarEvent_importsDistinctInstances(){
        val calendarId=createCalendar()
        val start=System.currentTimeMillis()+3_600_000L
        val values=ContentValues().apply{
            put(CalendarContract.Events.CALENDAR_ID,calendarId)
            put(CalendarContract.Events.TITLE,"V530 Recurring Sync")
            put(CalendarContract.Events.DTSTART,start)
            put(CalendarContract.Events.DURATION,"PT1H")
            put(CalendarContract.Events.RRULE,"FREQ=DAILY;COUNT=2")
            put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().id)
        }
        val uri=requireNotNull(context.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values))
        val providerId=uri.lastPathSegment!!.toLong()

        val manager=CalendarSyncManager(context,app.repository)
        val result=manager.syncCalendar(calendarId)
        val imported=app.repository.allEvents().filter{it.calendarId==calendarId&&it.calendarEventId==providerId&&manager.isImportedEvent(it)}
        assertTrue(result.importedOrUpdated>=2)
        assertTrue(imported.size>=2)
        assertEquals(imported.size,imported.map{it.startEpochMillis}.distinct().size)
    }

    private fun createCalendar():Long{
        val uri=CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER,"true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME,"munasabati-v530-test")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values=ContentValues().apply{
            put(CalendarContract.Calendars.ACCOUNT_NAME,"munasabati-v530-test")
            put(CalendarContract.Calendars.ACCOUNT_TYPE,CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME,"Munasabati V530 Test")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,"Munasabati V530 Test")
            put(CalendarContract.Calendars.CALENDAR_COLOR,0x336699)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT,"munasabati-v530-test")
            put(CalendarContract.Calendars.VISIBLE,1)
            put(CalendarContract.Calendars.SYNC_EVENTS,1)
        }
        val id=requireNotNull(context.contentResolver.insert(uri,values)).lastPathSegment!!.toLong()
        calendars+=id
        return id
    }
}
