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
 * v5.3 calendar architecture.
 *
 * External Google/Outlook/device events are provider-backed live data. They are not
 * persisted into Munasabati's Room event table. This creates a hard data boundary:
 * local Munasabati events can become Previous Events; external calendar events never can.
 */
class CalendarSyncManager(private val context:Context,private val repo:EventRepository){
    private fun canRead()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun canWrite()=ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED

    // v5.2.x import registry is read only for migration/proven imported-row cleanup.
    private val importRegistry by lazy { context.getSharedPreferences("munasabati_calendar_import_registry_v1", Context.MODE_PRIVATE) }
    private fun registryKey(calendarId:Long)="calendar_$calendarId"
    private fun trackedTokens(calendarId:Long):Set<String> = runCatching {
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toSet() ?: emptySet()
    }.getOrDefault(emptySet())
    private fun tokenAppId(token:String):String? = token.split('|').getOrNull(1)?.takeIf{it.isNotBlank()}
    private fun trackedImportedIds(calendarId:Long):Set<String> = trackedTokens(calendarId).mapNotNull(::tokenAppId).toSet()
    private fun allTrackedImportedIds():Set<String> = importRegistry.all.keys
        .filter{it.startsWith("calendar_")}
        .flatMap{key -> runCatching { importRegistry.getStringSet(key, emptySet()) ?: emptySet() }.getOrDefault(emptySet()) }
        .mapNotNull(::tokenAppId)
        .toSet()

    // v5.3 stores only which provider calendars are connected, not copies of their events.
    private val connectionPrefs by lazy { context.getSharedPreferences("munasabati_calendar_connections_v2", Context.MODE_PRIVATE) }
    private fun storedConnectedIds():Set<Long> = runCatching {
        connectionPrefs.getStringSet("calendar_ids", emptySet())?.mapNotNull{it.toLongOrNull()}?.toSet() ?: emptySet()
    }.getOrDefault(emptySet())
    private fun saveConnectedIds(ids:Set<Long>){
        connectionPrefs.edit().putStringSet("calendar_ids",ids.map{it.toString()}.toSet()).apply()
    }

    private fun migrateLegacyConnections(){
        val registryIds=importRegistry.all.keys
            .filter{it.startsWith("calendar_")}
            .mapNotNull{it.removePrefix("calendar_").toLongOrNull()}
            .toSet()
        val rowIds=repo.allEvents().mapNotNull{event->
            val provenImported=CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || allTrackedImportedIds().contains(event.id)
            event.calendarId?.takeIf{provenImported}
        }.toSet()
        val merged=storedConnectedIds()+registryIds+rowIds
        if(merged!=storedConnectedIds()) saveConnectedIds(merged)
    }

    fun connectedCalendarIds():Set<Long>{
        migrateLegacyConnections()
        return storedConnectedIds()
    }
    fun isCalendarConnected(calendarId:Long):Boolean=connectedCalendarIds().contains(calendarId)
    private fun markConnected(calendarId:Long){ saveConnectedIds(storedConnectedIds()+calendarId) }
    private fun markDisconnected(calendarId:Long){ saveConnectedIds(storedConnectedIds()-calendarId) }

    fun calendars():List<SystemCalendar>{
        if(!canRead()) return emptyList()
        val out=mutableListOf<SystemCalendar>()
        runCatching {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                arrayOf(CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,CalendarContract.Calendars.ACCOUNT_NAME,CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL),
                "${CalendarContract.Calendars.VISIBLE}=1",null,"${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE"
            )?.use{c->while(c.moveToNext())out+=SystemCalendar(c.getLong(0),c.getString(1)?:"تقويم",c.getString(2)?:"",c.getInt(3)>=CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)}
        }
        return out
    }

    private fun valuesFor(event:EventModel,calendarId:Long,withMarker:Boolean):ContentValues=ContentValues().apply{
        put(CalendarContract.Events.CALENDAR_ID,calendarId)
        put(CalendarContract.Events.TITLE,event.title)
        put(CalendarContract.Events.DESCRIPTION,event.notes)
        put(CalendarContract.Events.EVENT_LOCATION,event.locationName)
        put(CalendarContract.Events.DTSTART,event.startEpochMillis)
        put(CalendarContract.Events.DTEND,event.endEpochMillis)
        put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().id)
        if(withMarker){
            put(CalendarContract.Events.CUSTOM_APP_PACKAGE,context.packageName)
            put(CalendarContract.Events.CUSTOM_APP_URI,"munasabati://event/${event.id}")
        }
    }

    private fun writeProviderEvent(event:EventModel,calendarId:Long):Long?{
        fun write(values:ContentValues):Long?{
            event.calendarEventId?.let{existingId->
                val target=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,existingId)
                if(context.contentResolver.update(target,values,null,null)>0) return existingId
            }
            return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)?.lastPathSegment?.toLongOrNull()
        }
        return runCatching{write(valuesFor(event,calendarId,true))}.getOrElse{
            // Some vendor CalendarProviders reject custom marker columns on writes.
            runCatching{write(valuesFor(event,calendarId,false))}.getOrNull()
        }
    }

    /** Push a Munasabati-local event to a writable provider calendar. */
    fun push(event:EventModel,calendarId:Long):Long?{
        if(!canWrite() || isImportedEvent(event)) return null
        val providerId=writeProviderEvent(event,calendarId)?:return null
        repo.upsertEvent(event.copy(
            calendarId=calendarId,
            calendarEventId=providerId,
            calendarSync=true,
            calendarFingerprint=event.calendarFingerprint.takeIf{it.startsWith("local:")}
                ?:CalendarEventOrigin.localFingerprint(fingerprint(event)),
            updatedAt=System.currentTimeMillis()
        ))
        return providerId
    }

    private fun instanceUri(from:Long,to:Long)=CalendarContract.Instances.CONTENT_URI.buildUpon().also{
        ContentUris.appendId(it,from)
        ContentUris.appendId(it,to)
    }.build()

    /** Read a durable Munasabati marker from a provider event when supported. */
    private fun markerLocalId(providerId:Long):String?=runCatching{
        context.contentResolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,providerId),
            arrayOf(CalendarContract.Events.CUSTOM_APP_PACKAGE,CalendarContract.Events.CUSTOM_APP_URI),
            null,null,null
        )?.use{c->
            if(!c.moveToFirst()) return@use null
            val pkg=c.getString(0).orEmpty()
            val uri=c.getString(1).orEmpty()
            if(pkg==context.packageName && uri.startsWith("munasabati://event/"))
                uri.removePrefix("munasabati://event/").takeIf{it.isNotBlank()}
            else null
        }
    }.getOrNull()

    /**
     * Live Instances query. Recurring provider events are expanded into their real
     * occurrences; anything already ended is excluded immediately.
     */
    fun liveEventsForCalendar(
        calendarId:Long,
        from:Long=System.currentTimeMillis()-7L*86_400_000L,
        to:Long=System.currentTimeMillis()+730L*86_400_000L
    ):List<EventModel>{
        if(!canRead()) return emptyList()
        val now=System.currentTimeMillis()
        val snapshot=repo.allEvents()
        val byId=snapshot.associateBy{it.id}
        val linkedProviderIds=snapshot.filter{it.calendarId==calendarId && !isImportedEvent(it)}.mapNotNull{it.calendarEventId}.toSet()
        val out=mutableListOf<EventModel>()

        context.contentResolver.query(
            instanceUri(from,to),
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Instances.TITLE,
                CalendarContract.Instances.DESCRIPTION,
                CalendarContract.Instances.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Instances.STATUS
            ),
            "${CalendarContract.Instances.CALENDAR_ID}=?",
            arrayOf(calendarId.toString()),
            CalendarContract.Instances.BEGIN
        )?.use{c->while(c.moveToNext()){
            val providerId=c.getLong(0)
            val title=c.getString(1)?.trim().orEmpty()
            if(title.isBlank()) continue
            val start=c.getLong(4)
            val end=c.getLong(5).takeIf{it>start}?:start+3_600_000L
            val status=c.getInt(6)
            if(status==CalendarContract.Events.STATUS_CANCELED || end<=now) continue

            // A provider row created from Munasabati must remain one local event, not
            // return later as a second external copy after reconnecting.
            val markerId=markerLocalId(providerId)
            val markerLocal=markerId?.let{byId[it]}
            if(providerId in linkedProviderIds || markerLocal!=null){
                if(markerLocal!=null && (markerLocal.calendarId!=calendarId || markerLocal.calendarEventId!=providerId || !markerLocal.calendarSync)){
                    repo.upsertEvent(markerLocal.copy(
                        calendarId=calendarId,
                        calendarEventId=providerId,
                        calendarSync=true,
                        calendarFingerprint=markerLocal.calendarFingerprint.takeIf{it.startsWith("local:")}
                            ?:CalendarEventOrigin.localFingerprint(fingerprint(markerLocal)),
                        updatedAt=System.currentTimeMillis()
                    ))
                }
                continue
            }

            out+=EventModel(
                id="external:$calendarId:$providerId:$start",
                title=title,
                notes=c.getString(2)?:"",
                locationName=c.getString(3)?:"",
                startEpochMillis=start,
                endEpochMillis=end,
                category=EventCategory.OTHER,
                calendarId=calendarId,
                calendarEventId=providerId,
                calendarSync=true,
                calendarFingerprint=CalendarEventOrigin.importedFingerprint(calendarId,providerId,start)
            )
        }}
        return out.distinctBy{it.id}.sortedBy{it.startEpochMillis}
    }

    fun liveConnectedEvents(
        from:Long=System.currentTimeMillis()-7L*86_400_000L,
        to:Long=System.currentTimeMillis()+730L*86_400_000L
    ):List<EventModel> = connectedCalendarIds()
        .flatMap{id->runCatching{liveEventsForCalendar(id,from,to)}.getOrDefault(emptyList())}
        .distinctBy{it.id}
        .sortedBy{it.startEpochMillis}

    /**
     * Compatibility name kept for the existing UI. In v5.3 this connects/refreshes a
     * live source and returns the number visible; it no longer imports copies into Room.
     */
    fun importCalendar(calendarId:Long,from:Long=System.currentTimeMillis()-7L*86_400_000L,to:Long=System.currentTimeMillis()+730L*86_400_000L):Int{
        markConnected(calendarId)
        purgePersistedImportedRows(calendarId)
        return liveEventsForCalendar(calendarId,from,to).size
    }

    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        markConnected(calendarId)
        purgePersistedImportedRows(calendarId)
        val visible=runCatching{liveEventsForCalendar(calendarId)}.getOrDefault(emptyList()).size
        var pushed=0
        repo.allEvents().filter{it.calendarSync && it.calendarId==calendarId && !isImportedEvent(it)}.forEach{
            runCatching{push(it,calendarId)}.getOrNull()?.let{pushed++}
        }
        return CalendarSyncResult(visible,pushed)
    }

    fun syncMarked():Int{
        var pushed=0
        repo.allEvents().filter{it.calendarSync && it.calendarId!=null && !isImportedEvent(it)}.forEach{
            runCatching{push(it,it.calendarId!!)}.getOrNull()?.let{pushed++}
        }
        return pushed
    }

    /** Only Munasabati-local rows are counted as linked rows. */
    fun linkedCount(calendarId:Long):Int=repo.allEvents().count{it.calendarId==calendarId && !isImportedEvent(it)}

    /**
     * Purge only rows that carry explicit imported identity or an old registry identity.
     * There is deliberately no title/date/category heuristic in v5.3.
     */
    private fun purgePersistedImportedRows(calendarId:Long?=null):Int{
        migrateLegacyConnections()
        val tracked=if(calendarId==null) allTrackedImportedIds() else trackedImportedIds(calendarId)
        var removed=0
        repo.allEvents().filter{event->
            val belongs=calendarId==null || event.calendarId==calendarId || tracked.contains(event.id)
            belongs && (CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || tracked.contains(event.id))
        }.forEach{event->if(repo.deleteEvent(event.id)>0)removed++}
        if(calendarId==null) importRegistry.edit().clear().apply()
        else importRegistry.edit().remove(registryKey(calendarId)).apply()
        return removed
    }

    /** Called by screens when entering v5.3 live mode. */
    fun prepareLiveMode():Int{
        migrateLegacyConnections()
        return purgePersistedImportedRows(null)
    }

    /** Compatibility API: no local-history heuristic cleanup; only proven old imports. */
    fun cleanupExpiredCalendarEvents(now:Long=System.currentTimeMillis()):Int=prepareLiveMode()

    fun disconnectCalendar(calendarId:Long):CalendarDisconnectResult{
        migrateLegacyConnections()
        var removed=purgePersistedImportedRows(calendarId)
        var unlinked=0
        repo.allEvents().filter{it.calendarId==calendarId && !isImportedEvent(it)}.forEach{event->
            repo.upsertEvent(event.copy(
                calendarId=null,
                calendarEventId=null,
                calendarSync=false,
                calendarFingerprint=event.calendarFingerprint.takeIf{it.startsWith("local:")}
                    ?:CalendarEventOrigin.localFingerprint(fingerprint(event)),
                updatedAt=System.currentTimeMillis()
            ))
            unlinked++
        }
        markDisconnected(calendarId)
        return CalendarDisconnectResult(removed,unlinked)
    }

    fun isImportedEvent(event:EventModel):Boolean=
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || allTrackedImportedIds().contains(event.id)

    /** Source-provider deletion is intentionally not used by disconnect/remove-from-app. */
    fun deleteLinked(event:EventModel):Boolean{
        if(!event.calendarSync || event.calendarEventId==null) return true
        if(!canWrite()) return false
        return runCatching{
            context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,event.calendarEventId),null,null)>=0
        }.getOrDefault(false)
    }

    private fun fingerprint(e:EventModel)="${e.title}|${e.startEpochMillis}|${e.endEpochMillis}|${e.locationName}|${e.notes}".hashCode().toString()
}
