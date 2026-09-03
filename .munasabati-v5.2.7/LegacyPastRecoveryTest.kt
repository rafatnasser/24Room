package com.rafat.munasabati.migration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.rafat.munasabati.MunasabatiApp
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class LegacyPastRecoveryTest {
    @Test
    fun missingLegacyPastEvent_isRecoveredAsLocalOnly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as MunasabatiApp
        val repo = app.repository
        val prefs = context.getSharedPreferences("munasabati_events", Context.MODE_PRIVATE)
        val previousRaw = prefs.getString("events", null)
        val id = "v527-recovery-${UUID.randomUUID()}"
        val start = System.currentTimeMillis() - 86_400_000L

        val legacy = JSONObject().apply {
            put("id", id)
            put("title", "Recovered local previous event v527")
            put("eventTime", start)
            put("category", "PERSONAL")
            put("details", "legacy local history")
            put("calendarSync", true)
            put("calendarId", 12345)
            put("calendarEventId", 67890)
        }
        prefs.edit().putString("events", JSONArray().put(legacy).toString()).commit()
        repo.deleteEvent(id)
        repo.putMeta("v527_local_past_recovery_done", "")

        try {
            val recovered = LegacyPastRecovery.recoverMissingLocalPastEvents(context, repo)
            assertEquals(1, recovered)
            val event = repo.allEvents().first { it.id == id }
            assertTrue(event.startEpochMillis < System.currentTimeMillis())
            assertTrue(!event.calendarSync)
            assertEquals(null, event.calendarId)
            assertEquals(null, event.calendarEventId)
            assertTrue(event.calendarFingerprint.startsWith("local:"))
        } finally {
            repo.deleteEvent(id)
            if (previousRaw == null) prefs.edit().remove("events").commit()
            else prefs.edit().putString("events", previousRaw).commit()
            repo.putMeta("v527_local_past_recovery_done", "done")
        }
    }
}
