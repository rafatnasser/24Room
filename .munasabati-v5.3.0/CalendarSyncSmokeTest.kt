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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.util.TimeZone
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class CalendarSyncSmokeTest {
    private lateinit var context: Context
    private lateinit var app: MunasabatiApp
    private var calendarId: Long? = null
    private val providerEventIds = mutableListOf<Long>()
    private val localIds = mutableListOf<String>()

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        app = context.applicationContext as MunasabatiApp
    }

    @After
    fun cleanup() {
        localIds.forEach { runCatching { app.repository.deleteEvent(it) } }
        providerEventIds.forEach { id ->
            runCatching { context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id), null, null) }
        }
        calendarId?.let { id ->
            val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v530-test")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
                .build()
            runCatching { context.contentResolver.delete(ContentUris.withAppendedId(uri, id), null, null) }
        }
    }

    private fun createCalendar(): Long {
        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v530-test")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            .build()
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, "munasabati-v530-test")
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CalendarContract.ACCOUNT_TYPE_LOCAL)
            put(CalendarContract.Calendars.NAME, "Munasabati v5.3 Test")
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, "Munasabati v5.3 Test")
            put(CalendarContract.Calendars.CALENDAR_COLOR, 0x336699)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, "munasabati-v530-test")
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
        }
        val created = requireNotNull(context.contentResolver.insert(uri, values))
        return created.lastPathSegment!!.toLong().also { calendarId = it }
    }

    private fun createProviderEvent(calendarId: Long, title: String, start: Long, end: Long): Long {
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, title)
            put(CalendarContract.Events.DTSTART, start)
            put(CalendarContract.Events.DTEND, end)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val created = requireNotNull(context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values))
        return created.lastPathSegment!!.toLong().also { providerEventIds += it }
    }

    @Test
    fun sync_isLive_nonPersistent_and_neverDeletesLocalHistory() {
        val calId = createCalendar()
        val now = System.currentTimeMillis()

        // This pair intentionally resembles the legacy heuristic that previously deleted
        // a local past event when an external event had the same title and start time.
        val pastStart = now - 3_600_000L * 3
        val pastEnd = now - 3_600_000L * 2
        createProviderEvent(calId, "Protected local history", pastStart, pastEnd)
        val localPast = EventModel(
            id = "v530-local-${UUID.randomUUID()}",
            title = "Protected local history",
            startEpochMillis = pastStart,
            endEpochMillis = pastEnd,
            category = EventCategory.OTHER
        )
        localIds += localPast.id
        app.repository.upsertEvent(localPast)

        val futureStart = now + 3_600_000L
        createProviderEvent(calId, "Live external event", futureStart, futureStart + 3_600_000L)

        val manager = CalendarSyncManager(context, app.repository)
        manager.cleanupExpiredCalendarEvents()
        assertNotNull("Local history must never be deleted by calendar cleanup", app.repository.eventById(localPast.id))

        val result = manager.syncCalendar(calId)
        assertTrue(result.importedOrUpdated >= 1)
        assertTrue(manager.isCalendarConnected(calId))
        assertTrue(manager.liveConnectedEvents().any { it.title == "Live external event" })

        // External rows are rendered live from CalendarProvider, never persisted into Room.
        assertFalse(app.repository.allEvents().any {
            it.calendarId == calId && CalendarEventOrigin.isImportedFingerprint(it.calendarFingerprint)
        })

        manager.disconnectCalendar(calId)
        assertFalse(manager.isCalendarConnected(calId))
        assertFalse(manager.liveConnectedEvents().any { it.title == "Live external event" })
        assertNotNull(app.repository.eventById(localPast.id))
    }

    @Test
    fun ahlBayt_calendarEvents_show_even_when_masterReminder_isOff() {
        AhlBaytCalendar.setEnabled(context, false)
        AhlBaytCategory.values().forEach { AhlBaytCalendar.setCategoryEnabled(context, it, true) }

        var date = LocalDate.now()
        var found: List<EventModel> = emptyList()
        repeat(400) {
            found = UnifiedCalendarSource.ahlBaytEventsForDate(context, date)
            if (found.isNotEmpty()) return@repeat
            date = date.plusDays(1)
        }
        assertTrue("At least one Ahl al-Bayt occasion must be visible in the calendar year", found.isNotEmpty())
        assertTrue(found.all { it.id.startsWith("ahlbayt:") && it.category == EventCategory.RELIGIOUS })
    }

    @Test
    fun previousEventRecovery_restoresMissingLegacyRow_withoutOverwritingCurrentRows() {
        val prefs = context.getSharedPreferences("munasabati_events", Context.MODE_PRIVATE)
        val oldPayload = prefs.getString("events", null)
        val recoveredId = "v530-recover-${UUID.randomUUID()}"
        val currentId = "v530-current-${UUID.randomUUID()}"
        localIds += recoveredId
        localIds += currentId

        val now = System.currentTimeMillis()
        val legacy = JSONObject().apply {
            put("id", recoveredId)
            put("title", "Recovered previous event")
            put("category", "عائلية")
            put("recurrence", "none")
            put("eventTime", now - 7L * 86_400_000L)
            put("remindersCsv", "60")
            put("strongAlert", false)
            put("favorite", false)
        }
        prefs.edit().putString("events", JSONArray().put(legacy).toString()).commit()

        val current = EventModel(
            id = currentId,
            title = "Current row wins",
            startEpochMillis = now - 2L * 86_400_000L,
            endEpochMillis = now - 2L * 86_400_000L + 3_600_000L,
            category = EventCategory.WORK
        )
        app.repository.upsertEvent(current)
        app.repository.putMeta("legacy_v4_migration", "done")
        app.repository.putMeta("v530_previous_event_recovery", "")

        val recovered = PreviousEventRecovery.recoverMissingLegacyEventsOnce(context, app.repository, now)
        assertTrue(recovered >= 1)
        assertNotNull(app.repository.eventById(recoveredId))
        assertTrue(app.repository.eventById(currentId)?.title == "Current row wins")

        if (oldPayload == null) prefs.edit().remove("events").commit()
        else prefs.edit().putString("events", oldPayload).commit()
    }
}
