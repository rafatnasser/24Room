package com.rafat.munasabati.compat

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rafat.munasabati.MunasabatiApp
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone

@RunWith(AndroidJUnit4::class)
class CalendarSyncRegressionTest {
    private lateinit var context: Context
    private var calendarId: Long? = null
    private var providerEventId: Long? = null
    private val createdAppIds = mutableListOf<String>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cleanup() {
        val app = context.applicationContext as MunasabatiApp
        createdAppIds.forEach { runCatching { app.repository.deleteEvent(it) } }
        providerEventId?.let { runCatching { context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it), null, null) } }
        calendarId?.let { id ->
            val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v527-test")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()
            runCatching { context.contentResolver.delete(ContentUris.withAppendedId(uri, id), null, null) }
        }
    }

    private fun createCalendar(): Long {
        val resolver = context.contentResolver
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v527-test")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v527-test")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "Munasabati v527 Test")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Munasabati v527 Test")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0x447799)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "munasabati-v527-test")
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        return requireNotNull(resolver.insert(uri, values)).lastPathSegment!!.toLong().also { calendarId = it }
    }

    @Test
    fun recurringProviderEvent_importsExpandedInstances() {
        val calId = createCalendar()
        val start = System.currentTimeMillis() + 3_600_000L
        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calId)
            put(CalendarContract.Events.TITLE, "Munasabati recurring sync v527")
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DURATION, "PT30M")
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.RRULE, "FREQ=DAILY;COUNT=3")
        }
        val ev = requireNotNull(context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, eventValues))
        providerEventId = ev.lastPathSegment!!.toLong()

        val app = context.applicationContext as MunasabatiApp
        val manager = CalendarSyncManager(context, app.repository)
        manager.syncCalendar(calId)
        val imported = app.repository.allEvents().filter {
            it.calendarId == calId && it.calendarEventId == providerEventId && CalendarEventOrigin.isImportedFingerprint(it.calendarFingerprint)
        }
        createdAppIds += imported.map { it.id }

        assertTrue("Recurring event must expand into at least 3 calendar instances", imported.map { it.startEpochMillis }.distinct().size >= 3)
    }

    @Test
    fun cleanupExpiredCalendarEvents_neverDeletesAmbiguousLocalHistory() {
        val app = context.applicationContext as MunasabatiApp
        val now = System.currentTimeMillis()
        val local = EventModel(
            title = "Local previous event v527",
            startEpochMillis = now - 3_600_000L,
            endEpochMillis = now - 1_800_000L,
            category = EventCategory.OTHER,
            calendarId = null,
            calendarEventId = null,
            calendarSync = false,
            calendarFingerprint = ""
        )
        app.repository.upsertEvent(local)
        createdAppIds += local.id

        CalendarSyncManager(context, app.repository).cleanupExpiredCalendarEvents(now)
        assertTrue("Local previous event must survive calendar cleanup", app.repository.allEvents().any { it.id == local.id })

        val imported = EventModel(
            title = "Expired imported event v527",
            startEpochMillis = now - 7_200_000L,
            endEpochMillis = now - 3_600_000L,
            category = EventCategory.OTHER,
            calendarId = 999L,
            calendarEventId = 111L,
            calendarSync = true,
            calendarFingerprint = CalendarEventOrigin.importedFingerprint(999L, 111L, now - 7_200_000L)
        )
        app.repository.upsertEvent(imported)
        createdAppIds += imported.id
        CalendarSyncManager(context, app.repository).cleanupExpiredCalendarEvents(now)
        assertFalse("Explicit expired import must be removed", app.repository.allEvents().any { it.id == imported.id })
    }
}
