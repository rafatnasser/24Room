package com.rafat.munasabati.compat

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.rafat.munasabati.data.EventRepository
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import java.util.TimeZone

data class SystemCalendar(val id:Long,val name:String,val account:String,val writable:Boolean)
data class CalendarSyncResult(val importedOrUpdated:Int,val pushed:Int)
data class CalendarDisconnectResult(val importedRemoved:Int,val localUnlinked:Int) {
    val total:Int get() = importedRemoved + localUnlinked
}

/**
 * Android calendar bridge.
 *
 * v5.2.7 rules:
 * 1) Read CalendarContract.Instances, not Events, so recurring events are expanded correctly.
 * 2) Never infer that an untagged local row is an external import.
 * 3) Only explicit imported fingerprints or registry-tracked rows may be auto-deleted.
 * 4) Local-origin fingerprints survive disconnects so previous local history is protected.
 */
class CalendarSyncManager(private val context:Context, private val repo:EventRepository) {
    private fun canRead() = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    private fun canWrite() = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    private val importRegistry by lazy { context.getSharedPreferences("munasabati_calendar_import_registry_v1", Context.MODE_PRIVATE) }
    private fun registryKey(calendarId:Long) = "calendar_$calendarId"
    private fun trackedTokens(calendarId:Long):MutableSet<String> = runCatching {
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toMutableSet() ?: mutableSetOf()
    }.getOrElse {
        importRegistry.edit().remove(registryKey(calendarId)).apply()
        mutableSetOf()
    }
    private fun tokenParts(token:String):List<String> = token.split('|')
    private fun tokenProviderId(token:String):Long? = tokenParts(token).getOrNull(0)?.toLongOrNull()
    private fun tokenAppId(token:String):String? = tokenParts(token).getOrNull(1)?.takeIf { it.isNotBlank() }
    private fun tokenStart(token:String):Long? = tokenParts(token).getOrNull(2)?.toLongOrNull()
    private fun trackedImportedIds(calendarId:Long):Set<String> = trackedTokens(calendarId).mapNotNull(::tokenAppId).toSet()
    private fun allTrackedImportedIds():Set<String> = importRegistry.all.keys
        .filter { it.startsWith("calendar_") }
        .flatMap { key -> runCatching { importRegistry.getStringSet(key, emptySet()) ?: emptySet() }.getOrDefault(emptySet()).mapNotNull(::tokenAppId) }
        .toSet()

    private fun rememberImportedBatch(calendarId:Long, entries:List<Triple<Long,String,Long>>) {
        if(entries.isEmpty()) return
        val values = trackedTokens(calendarId)
        val appIds = entries.map { it.second }.toSet()
        values.removeAll { tokenAppId(it) in appIds }
        entries.forEach { entry -> values += "${entry.first}|${entry.second}|${entry.third}" }
        importRegistry.edit().putStringSet(registryKey(calendarId), values).apply()
    }

    private fun removeTrackedAppIds(appIds:Set<String>) {
        if(appIds.isEmpty()) return
        val editor = importRegistry.edit()
        importRegistry.all.keys.filter { it.startsWith("calendar_") }.forEach { key ->
            val values = runCatching { importRegistry.getStringSet(key, emptySet())?.toMutableSet() ?: mutableSetOf() }.getOrDefault(mutableSetOf())
            values.removeAll { tokenAppId(it) in appIds }
            if(values.isEmpty()) editor.remove(key) else editor.putStringSet(key, values)
        }
        editor.apply()
    }

    private fun clearTrackedCalendar(calendarId:Long) { importRegistry.edit().remove(registryKey(calendarId)).apply() }
    private fun explicitImported(event:EventModel):Boolean = CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)
    private fun isTrackedImported(event:EventModel):Boolean = allTrackedImportedIds().contains(event.id)

    fun calendars():List<SystemCalendar> {
        if(!canRead()) return emptyList()
        val out = mutableListOf<SystemCalendar>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME, CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL),
            "${CalendarContract.Calendars.VISIBLE}=1", null, "${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE"
        )?.use { c ->
            while(c.moveToNext()) out += SystemCalendar(
                c.getLong(0), c.getString(1) ?: "تقويم", c.getString(2) ?: "",
                c.getInt(3) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
            )
        }
        return out
    }

    fun push(event:EventModel, calendarId:Long):Long? {
        if(!canWrite()) return null
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, event.title)
            put(CalendarContract.Events.DESCRIPTION, event.notes)
            put(CalendarContract.Events.EVENT_LOCATION, event.locationName)
            put(CalendarContract.Events.DTSTART, event.startEpochMillis)
            put(CalendarContract.Events.DTEND, event.endEpochMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        val uri = if(event.calendarEventId != null) {
            val target = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.calendarEventId)
            val updated = context.contentResolver.update(target, values, null, null)
            if(updated <= 0) context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) else target
        } else context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
        val id = uri?.lastPathSegment?.toLongOrNull()
        if(id != null) {
            val origin = if(isImportedEvent(event)) {
                event.calendarFingerprint
            } else {
                event.calendarFingerprint.takeIf { it.startsWith("local:") } ?: CalendarEventOrigin.localFingerprint(fingerprint(event))
            }
            repo.upsertEvent(event.copy(calendarId=calendarId, calendarEventId=id, calendarSync=true, calendarFingerprint=origin, updatedAt=System.currentTimeMillis()))
        }
        return id
    }

    private data class ProviderInstance(
        val providerId:Long,
        val title:String,
        val notes:String,
        val location:String,
        val start:Long,
        val end:Long
    )

    fun importCalendar(
        calendarId:Long,
        from:Long = System.currentTimeMillis() - 30L * 86_400_000L,
        to:Long = System.currentTimeMillis() + 730L * 86_400_000L
    ):Int {
        if(!canRead()) return 0
        val now = System.currentTimeMillis()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, from)
        ContentUris.appendId(builder, to)
        val instances = mutableListOf<ProviderInstance>()
        context.contentResolver.query(
            builder.build(),
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            ),
            "${CalendarContract.Instances.CALENDAR_ID}=?",
            arrayOf(calendarId.toString()),
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { c ->
            while(c.moveToNext()) {
                val providerId = c.getLong(0)
                val title = c.getString(1)?.trim().orEmpty()
                if(title.isBlank()) continue
                val start = c.getLong(4)
                val end = c.getLong(5).takeIf { it > start } ?: start + 3_600_000L
                instances += ProviderInstance(providerId, title, c.getString(2) ?: "", c.getString(3) ?: "", start, end)
            }
        }
        val providerInstances = instances.distinctBy { it.providerId to it.start }
        val providerCount = providerInstances.groupingBy { it.providerId }.eachCount()
        val seenPairs = providerInstances.map { it.providerId to it.start }.toSet()

        val existingSnapshot = repo.allEvents()
        val existingById = existingSnapshot.associateBy { it.id }
        val existingByExact = existingSnapshot
            .filter { it.calendarId == calendarId && it.calendarEventId != null }
            .associateBy { it.calendarEventId!! to it.startEpochMillis }
        val existingByProvider = existingSnapshot
            .filter { it.calendarId == calendarId && it.calendarEventId != null }
            .groupBy { it.calendarEventId!! }
        val localLinkedByProvider = existingSnapshot
            .filter { it.calendarId == calendarId && it.calendarEventId != null && !isImportedEvent(it) }
            .associateBy { it.calendarEventId!! }
        val registryByPair = trackedTokens(calendarId).mapNotNull { token ->
            val providerId = tokenProviderId(token)
            val appId = tokenAppId(token)
            val start = tokenStart(token)
            if(providerId != null && appId != null && start != null) (providerId to start) to appId else null
        }.toMap()

        val retainedImportedIds = mutableSetOf<String>()
        val registryBatch = mutableListOf<Triple<Long,String,Long>>()
        var count = 0

        providerInstances.forEach { instance ->
            val exact = existingByExact[instance.providerId to instance.start]
            val registryExisting = registryByPair[instance.providerId to instance.start]?.let(existingById::get)
            val singleImported = if(providerCount[instance.providerId] == 1) {
                existingByProvider[instance.providerId]?.filter(::isImportedEvent)?.singleOrNull()
            } else null
            val existing = localLinkedByProvider[instance.providerId] ?: exact ?: registryExisting ?: singleImported

            if(instance.end <= now) {
                if(existing != null && isImportedEvent(existing)) {
                    repo.deleteEvent(existing.id)
                    removeTrackedAppIds(setOf(existing.id))
                }
                return@forEach
            }

            val existingIsImported = existing?.let(::isImportedEvent) ?: true
            val base = existing ?: EventModel(
                title = instance.title,
                startEpochMillis = instance.start,
                endEpochMillis = instance.end,
                category = EventCategory.OTHER
            )
            val origin = if(existingIsImported) {
                CalendarEventOrigin.importedFingerprint(calendarId, instance.providerId, instance.start)
            } else {
                base.calendarFingerprint.takeIf { it.startsWith("local:") } ?: CalendarEventOrigin.localFingerprint(fingerprint(base))
            }
            val merged = base.copy(
                title = instance.title,
                notes = instance.notes,
                locationName = instance.location,
                startEpochMillis = instance.start,
                endEpochMillis = instance.end,
                calendarId = calendarId,
                calendarEventId = instance.providerId,
                calendarSync = true,
                calendarFingerprint = origin,
                updatedAt = System.currentTimeMillis()
            )
            repo.upsertEvent(merged)
            if(existingIsImported) {
                retainedImportedIds += merged.id
                registryBatch += Triple(instance.providerId, merged.id, instance.start)
            }
            count++
        }

        // Reconcile deleted/rescheduled external instances. Only proven imports are removed.
        val trackedIds = trackedImportedIds(calendarId)
        val staleImported = existingSnapshot.filter { event ->
            val tiedToCalendar = event.calendarId == calendarId || event.id in trackedIds
            val withinWindow = event.startEpochMillis in from..to
            tiedToCalendar && withinWindow && isImportedEvent(event) && event.id !in retainedImportedIds &&
                (event.calendarEventId == null || (event.calendarEventId to event.startEpochMillis) !in seenPairs)
        }
        staleImported.forEach { repo.deleteEvent(it.id) }
        removeTrackedAppIds(staleImported.map { it.id }.toSet())

        // If a locally-created event's external copy disappeared, keep the local event and only unlink it.
        existingSnapshot.filter { event ->
            event.calendarId == calendarId && event.calendarEventId != null && !isImportedEvent(event) &&
                event.startEpochMillis in from..to && (event.calendarEventId to event.startEpochMillis) !in seenPairs
        }.forEach { event ->
            val localOrigin = event.calendarFingerprint.takeIf { it.startsWith("local:") } ?: CalendarEventOrigin.localFingerprint(fingerprint(event))
            repo.upsertEvent(event.copy(calendarId=null, calendarEventId=null, calendarSync=false, calendarFingerprint=localOrigin, updatedAt=System.currentTimeMillis()))
        }

        rememberImportedBatch(calendarId, registryBatch)
        cleanupExpiredCalendarEvents(now)
        return count
    }

    fun syncCalendar(calendarId:Long):CalendarSyncResult {
        cleanupExpiredCalendarEvents()
        val imported = importCalendar(calendarId)
        var pushed = 0
        repo.allEvents().filter { it.calendarSync && it.calendarId == calendarId && !isImportedEvent(it) }.forEach {
            runCatching { push(it, calendarId) }.getOrNull()?.let { pushed++ }
        }
        cleanupExpiredCalendarEvents()
        return CalendarSyncResult(imported, pushed)
    }

    fun syncMarked():Int {
        var n = 0
        repo.allEvents().filter { it.calendarSync && it.calendarId != null && !isImportedEvent(it) }.forEach {
            runCatching { push(it, it.calendarId!!) }.getOrNull()?.let { n++ }
        }
        return n
    }

    fun linkedCount(calendarId:Long):Int {
        val tracked = trackedImportedIds(calendarId)
        return repo.allEvents().count { it.calendarId == calendarId || it.id in tracked }
    }

    fun disconnectCalendar(calendarId:Long):CalendarDisconnectResult {
        val tracked = trackedImportedIds(calendarId)
        val linked = repo.allEvents().filter { it.calendarId == calendarId || it.id in tracked }
        var removed = 0
        var unlinked = 0
        linked.forEach { event ->
            if(isImportedEvent(event) || event.id in tracked) {
                if(repo.deleteEvent(event.id) > 0) removed++
            } else {
                val localOrigin = event.calendarFingerprint.takeIf { it.startsWith("local:") } ?: CalendarEventOrigin.localFingerprint(fingerprint(event))
                repo.upsertEvent(event.copy(calendarId=null, calendarEventId=null, calendarSync=false, calendarFingerprint=localOrigin, updatedAt=System.currentTimeMillis()))
                unlinked++
            }
        }
        clearTrackedCalendar(calendarId)
        cleanupExpiredCalendarEvents()
        return CalendarDisconnectResult(removed, unlinked)
    }

    fun isImportedEvent(event:EventModel):Boolean = explicitImported(event) || isTrackedImported(event)

    fun cleanupExpiredCalendarEvents(now:Long = System.currentTimeMillis()):Int {
        val tracked = allTrackedImportedIds()
        val removedIds = mutableSetOf<String>()
        repo.allEvents().filter { event ->
            (explicitImported(event) || event.id in tracked) && event.endEpochMillis <= now
        }.forEach { event ->
            if(repo.deleteEvent(event.id) > 0) removedIds += event.id
        }
        removeTrackedAppIds(removedIds)
        return removedIds.size
    }

    fun deleteLinked(event:EventModel):Boolean {
        if(!event.calendarSync || event.calendarEventId == null) return true
        if(!canWrite()) return false
        return runCatching {
            context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.calendarEventId), null, null) >= 0
        }.getOrDefault(false)
    }

    private fun fingerprint(e:EventModel) = "${e.title}|${e.startEpochMillis}|${e.endEpochMillis}|${e.locationName}|${e.notes}".hashCode().toString()
}
