package com.rafat.munasabati.migration

import android.content.Context
import com.rafat.munasabati.data.EventRepository
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import kotlin.math.abs

/**
 * One-time, loss-averse recovery for local events that originally came from the preserved v4 store.
 *
 * v5.2.4/v5.2.5 contained calendar-cleanup heuristics that could delete an untagged local historical
 * row when it happened to resemble an Android calendar event. The original v4 preference family and
 * its migration snapshot were intentionally preserved, so we can safely recover missing LOCAL history
 * without copying Android/Google/Outlook imports into Previous Events.
 */
object LegacyPastRecovery {
    private const val META_KEY = "v527_local_past_recovery_done"

    fun recoverMissingLocalPastEvents(context: Context, repo: EventRepository, now: Long = System.currentTimeMillis()): Int {
        if (repo.getMeta(META_KEY) == "done") return 0

        val raw = readLegacyEventsJson(context, repo)
        if (raw.isNullOrBlank()) {
            repo.putMeta(META_KEY, "done")
            repo.putMeta("v527_local_past_recovered_count", "0")
            return 0
        }

        val array = runCatching { JSONArray(raw) }.getOrElse { JSONArray() }
        val existing = repo.allEvents().toMutableList()
        var recovered = 0

        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val title = obj.optString("title", "").trim()
            if (title.isBlank()) continue
            val start = obj.optLong("eventTime", Long.MIN_VALUE).takeIf { it != Long.MIN_VALUE }
                ?: firstLong(obj, "startEpochMillis", "start_ms", "time", "dateMillis", "timestamp")
                ?: continue
            if (start >= now) continue

            // v4 active events are the user's own app events. External calendar imports were introduced later.
            // Recovered history is deliberately detached from every external calendar identity.
            val id = obj.opt("id")?.takeUnless { it == JSONObject.NULL }?.toString()?.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()

            val duplicate = existing.any { current ->
                current.id == id || (
                    normalize(current.title) == normalize(title) &&
                        abs(current.startEpochMillis - start) <= 60_000L
                    )
            }
            if (duplicate) continue

            val updated = obj.optLong("updatedAt", now)
            val event = EventModel(
                id = id,
                title = title,
                startEpochMillis = start,
                endEpochMillis = firstLong(obj, "endEpochMillis", "end_ms")?.takeIf { it > start } ?: start + 3_600_000L,
                category = EventCategory.from(obj.optString("category", null)),
                importance = when {
                    obj.optBoolean("strongAlert", false) -> 3
                    obj.optBoolean("pinned", false) || obj.optBoolean("favorite", false) -> 2
                    else -> 1
                },
                locationName = obj.optString("locationName", ""),
                notes = obj.optString("details", obj.optString("notes", "")),
                reminderMinutes = obj.optInt("reminderMinutes", 60),
                recurrence = obj.optString("recurrence", "none"),
                remindersCsv = obj.optString("remindersCsv", ""),
                attachmentUri = obj.optString("attachmentUri", "").takeIf { it.isNotBlank() },
                attachmentName = obj.optString("attachmentName", ""),
                attachmentType = obj.optString("attachmentType", ""),
                locationUrl = obj.optString("locationUrl", ""),
                legacyColor = obj.optString("color", ""),
                favorite = obj.optBoolean("favorite", false),
                pinned = obj.optBoolean("pinned", false),
                strongAlert = obj.optBoolean("strongAlert", false),
                calendarId = null,
                calendarEventId = null,
                calendarSync = false,
                calendarFingerprint = "local:legacy-recovered:${id.hashCode()}",
                legacyJson = obj.toString(),
                createdAt = updated,
                updatedAt = updated
            )
            repo.upsertEvent(event)
            existing += event
            recovered++
        }

        repo.putMeta(META_KEY, "done")
        repo.putMeta("v527_local_past_recovered_count", recovered.toString())
        return recovered
    }

    private fun readLegacyEventsJson(context: Context, repo: EventRepository): String? {
        val direct = runCatching {
            context.getSharedPreferences("munasabati_events", Context.MODE_PRIVATE).getString("events", null)
        }.getOrNull()
        if (!direct.isNullOrBlank() && direct != "[]") return direct

        val snapshotPath = repo.getMeta("legacy_v4_backup")?.takeIf { it.isNotBlank() } ?: return direct
        return runCatching {
            val snapshot = JSONObject(File(snapshotPath).readText())
            snapshot.optJSONObject("munasabati_events")?.optString("events", null)
        }.getOrNull() ?: direct
    }

    private fun firstLong(obj: JSONObject, vararg keys: String): Long? = keys.firstNotNullOfOrNull { key ->
        if (obj.has(key) && !obj.isNull(key)) obj.opt(key)?.toString()?.toLongOrNull() else null
    }

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}
