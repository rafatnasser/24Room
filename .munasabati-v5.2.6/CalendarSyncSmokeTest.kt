package com.rafat.munasabati.compat

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rafat.munasabati.MunasabatiApp
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.TimeZone
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class CalendarSyncSmokeTest {
    private lateinit var context: Context
    private var calendarId: Long? = null
    private var providerEventId: Long? = null

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun cleanup() {
        providerEventId?.let { runCatching { context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, it), null, null) } }
        calendarId?.let { id ->
            val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-test")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()
            runCatching { context.contentResolver.delete(ContentUris.withAppendedId(uri, id), null, null) }
        }
    }

    @Test
    fun syncCalendar_withRealAndroidProvider_doesNotCrash() {
        val resolver = context.contentResolver
        val calendarUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-test")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val calValues = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-test")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "Munasabati Test")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Munasabati Test")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0x336699)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "munasabati-test")
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val cal = requireNotNull(resolver.insert(calendarUri, calValues))
        calendarId = cal.lastPathSegment!!.toLong()

        val start = System.currentTimeMillis() + 3_600_000L
        val eventValues = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId!!)
            put(CalendarContract.Events.TITLE, "Munasabati Sync Smoke")
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, start + 3_600_000L)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val ev = requireNotNull(resolver.insert(CalendarContract.Events.CONTENT_URI, eventValues))
        providerEventId = ev.lastPathSegment!!.toLong()

        val app = context.applicationContext as MunasabatiApp
        val executor = Executors.newSingleThreadExecutor()
        val future = executor.submit<CalendarSyncResult> {
            CalendarSyncManager(context, app.repository).syncCalendar(calendarId!!)
        }
        val result = future.get(20, TimeUnit.SECONDS)
        executor.shutdownNow()

        assertTrue(result.importedOrUpdated >= 1)
        assertTrue(app.repository.allEvents().any { it.title == "Munasabati Sync Smoke" && it.calendarId == calendarId })
    }
}
