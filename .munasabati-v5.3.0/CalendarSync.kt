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

class CalendarSyncManager(private val context:Context,private val repo:EventRepository){
    private fun canRead()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun canWrite()=ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED

    private val importRegistry by lazy { context.getSharedPreferences("munasabati_calendar_import_registry_v1", Context.MODE_PRIVATE) }
    private fun registryKey(calendarId:Long)="calendar_$calendarId"
    private fun trackedTokens(calendarId:Long):MutableSet<String> = runCatching {
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toMutableSet() ?: mutableSetOf()
    }.getOrElse {
        importRegistry.edit().remove(registryKey(calendarId)).apply()
        mutableSetOf()
    }
    private fun tokenParts(token:String):Triple<Long,String,Long>?{
        val p=token.split('|')
        if(p.size<3) return null
        val providerId=p[0].toLongOrNull()?:return null
        val appId=p[1].takeIf{it.isNotBlank()}?:return null
        val start=p[2].toLongOrNull()?:return null
        return Triple(providerId,appId,start)
    }
    private fun trackedImportedIds(calendarId:Long):Set<String> = trackedTokens(calendarId).mapNotNull{tokenParts(it)?.second}.toSet()
    private fun allTrackedImportedIds():Set<String> = importRegistry.all.keys
        .filter{it.startsWith("calendar_")}
        .flatMap{key -> runCatching { importRegistry.getStringSet(key, emptySet()) ?: emptySet() }.getOrDefault(emptySet()) }
        .mapNotNull{tokenParts(it)?.second}
        .toSet()
    private fun trackedByInstance(calendarId:Long):Map<Pair<Long,Long>,String> = trackedTokens(calendarId).mapNotNull{tokenParts(it)}
        .associate{ (providerId,appId,start) -> (providerId to start) to appId }
    private fun writeTrackedCalendar(calendarId:Long,entries:Collection<Triple<Long,String,Long>>){
        if(entries.isEmpty()) importRegistry.edit().remove(registryKey(calendarId)).apply()
        else importRegistry.edit().putStringSet(registryKey(calendarId),entries.map{"${it.first}|${it.second}|${it.third}"}.toSet()).apply()
    }
    private fun clearTrackedCalendar(calendarId:Long){ importRegistry.edit().remove(registryKey(calendarId)).apply() }
    private fun isTrackedImported(event:EventModel):Boolean = allTrackedImportedIds().contains(event.id)

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

    private fun valuesFor(event:EventModel,calendarId:Long,withMarker:Boolean):ContentValues = ContentValues().apply{
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

    private fun writeProviderEvent(event:EventModel,calendarId:Long,withMarker:Boolean):Long?{
        fun write(values:ContentValues):Long?{
            val existingId=event.calendarEventId
            if(existingId!=null){
                val target=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,existingId)
                val updated=context.contentResolver.update(target,values,null,null)
                if(updated>0) return existingId
            }
            return context.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)?.lastPathSegment?.toLongOrNull()
        }
        return runCatching { write(valuesFor(event,calendarId,withMarker)) }.getOrElse {
            if(withMarker) runCatching { write(valuesFor(event,calendarId,false)) }.getOrNull() else null
        }
    }

    fun push(event:EventModel,calendarId:Long):Long?{
        if(!canWrite()) return null
        val imported=isImportedEvent(event)
        val id=writeProviderEvent(event,calendarId,!imported) ?: return null
        val origin=if(imported) event.calendarFingerprint else CalendarEventOrigin.localFingerprint(fingerprint(event))
        repo.upsertEvent(event.copy(calendarId=calendarId,calendarEventId=id,calendarSync=true,calendarFingerprint=origin,updatedAt=System.currentTimeMillis()))
        return id
    }

    private fun instanceUri(from:Long,to:Long)=CalendarContract.Instances.CONTENT_URI.buildUpon().also{
        ContentUris.appendId(it,from)
        ContentUris.appendId(it,to)
    }.build()

    private fun markerLocalId(providerId:Long):String? = runCatching {
        context.contentResolver.query(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,providerId),
            arrayOf(CalendarContract.Events.CUSTOM_APP_PACKAGE,CalendarContract.Events.CUSTOM_APP_URI),
            null,null,null
        )?.use{c->
            if(!c.moveToFirst()) return@use null
            val pkg=c.getString(0).orEmpty()
            val uri=c.getString(1).orEmpty()
            if(pkg==context.packageName && uri.startsWith("munasabati://event/")) uri.removePrefix("munasabati://event/").takeIf{it.isNotBlank()} else null
        }
    }.getOrNull()

    fun importCalendar(calendarId:Long,from:Long=System.currentTimeMillis()-86_400_000L,to:Long=System.currentTimeMillis()+730L*86_400_000L):Int{
        if(!canRead()) return 0
        val now=System.currentTimeMillis()
        val snapshot=repo.allEvents()
        val byId=snapshot.associateBy{it.id}
        val trackedByInstance=trackedByInstance(calendarId)
        val observedOrigins=mutableSetOf<String>()
        val registryEntries=mutableListOf<Triple<Long,String,Long>>()
        var count=0

        context.contentResolver.query(
            instanceUri(from,to),
            arrayOf(
                CalendarContract.Instances.EVENT_ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DESCRIPTION,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END,
                CalendarContract.Events.STATUS
            ),
            "calendar_id=?",
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

            val localLinked=snapshot.firstOrNull{it.calendarId==calendarId && it.calendarEventId==providerId && !isImportedEvent(it)}
                ?: markerLocalId(providerId)?.let{byId[it]}
            if(localLinked!=null){
                if(localLinked.calendarId!=calendarId || localLinked.calendarEventId!=providerId || !localLinked.calendarSync){
                    repo.upsertEvent(localLinked.copy(calendarId=calendarId,calendarEventId=providerId,calendarSync=true,
                        calendarFingerprint=localLinked.calendarFingerprint.takeIf{it.startsWith("local:")}?:CalendarEventOrigin.localFingerprint(fingerprint(localLinked)),
                        updatedAt=System.currentTimeMillis()))
                }
                continue
            }

            val origin=CalendarEventOrigin.importedFingerprint(calendarId,providerId,start)
            observedOrigins+=origin
            val trackedId=trackedByInstance[providerId to start]
            val existing=snapshot.firstOrNull{it.calendarFingerprint==origin}
                ?: trackedId?.let{byId[it]}
                ?: snapshot.firstOrNull{it.calendarId==calendarId && it.calendarEventId==providerId && it.startEpochMillis==start && isImportedEvent(it)}
            val base=existing?:EventModel(title=title,startEpochMillis=start,endEpochMillis=end,category=EventCategory.OTHER)
            val importedEvent=base.copy(
                title=title,
                notes=c.getString(2)?:"",
                locationName=c.getString(3)?:"",
                startEpochMillis=start,
                endEpochMillis=end,
                calendarId=calendarId,
                calendarEventId=providerId,
                calendarSync=true,
                calendarFingerprint=origin,
                updatedAt=System.currentTimeMillis()
            )
            repo.upsertEvent(importedEvent)
            registryEntries+=Triple(providerId,importedEvent.id,start)
            count++
        }}

        val observedIds=registryEntries.map{it.second}.toSet()
        val trackedIds=trackedImportedIds(calendarId)
        repo.allEvents().filter{event->
            event.endEpochMillis>now && event.startEpochMillis<=to &&
                (event.calendarId==calendarId || trackedIds.contains(event.id)) &&
                isImportedEvent(event) && !observedIds.contains(event.id) &&
                (event.calendarFingerprint.startsWith("imported:$calendarId:") || trackedIds.contains(event.id))
        }.forEach{repo.deleteEvent(it.id)}
        writeTrackedCalendar(calendarId,registryEntries)
        return count
    }

    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        cleanupExpiredCalendarEvents()
        val imported=importCalendar(calendarId)
        var pushed=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId&&!isImportedEvent(it)}.forEach{
            runCatching { push(it,calendarId) }.getOrNull()?.let { pushed++ }
        }
        cleanupExpiredCalendarEvents()
        return CalendarSyncResult(imported,pushed)
    }

    fun syncMarked():Int{
        var n=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId!=null&&!isImportedEvent(it)}.forEach{
            runCatching { push(it,it.calendarId!!) }.getOrNull()?.let{n++}
        }
        return n
    }

    fun linkedCount(calendarId:Long):Int{
        val tracked=trackedImportedIds(calendarId)
        return repo.allEvents().count{it.calendarId==calendarId || tracked.contains(it.id)}
    }

    fun disconnectCalendar(calendarId:Long):CalendarDisconnectResult{
        val tracked=trackedImportedIds(calendarId)
        val linked=repo.allEvents().filter{it.calendarId==calendarId || tracked.contains(it.id)}
        var removed=0
        var unlinked=0
        linked.forEach{event->
            if(CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || tracked.contains(event.id)){
                if(repo.deleteEvent(event.id)>0) removed++
            }else{
                val localOrigin=event.calendarFingerprint.takeIf{it.startsWith("local:")}
                    ?:CalendarEventOrigin.localFingerprint(fingerprint(event))
                repo.upsertEvent(event.copy(calendarId=null,calendarEventId=null,calendarSync=false,calendarFingerprint=localOrigin,updatedAt=System.currentTimeMillis()))
                unlinked++
            }
        }
        clearTrackedCalendar(calendarId)
        cleanupExpiredCalendarEvents()
        return CalendarDisconnectResult(removed,unlinked)
    }

    fun isImportedEvent(event:EventModel):Boolean=
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || isTrackedImported(event)

    fun cleanupExpiredCalendarEvents(now:Long=System.currentTimeMillis()):Int{
        val tracked=allTrackedImportedIds()
        var removed=0
        repo.allEvents().filter{event->
            (CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || tracked.contains(event.id)) && event.endEpochMillis<=now
        }.forEach{event-> if(repo.deleteEvent(event.id)>0) removed++ }
        pruneRegistry()
        return removed
    }

    private fun pruneRegistry(){
        val existing=repo.allEvents().map{it.id}.toSet()
        importRegistry.all.keys.filter{it.startsWith("calendar_")}.forEach{key->
            val current=runCatching{importRegistry.getStringSet(key,emptySet())?:emptySet()}.getOrDefault(emptySet())
            val kept=current.filter{tokenParts(it)?.second in existing}.toSet()
            if(kept.isEmpty()) importRegistry.edit().remove(key).apply()
            else if(kept!=current) importRegistry.edit().putStringSet(key,kept).apply()
        }
    }

    fun deleteLinked(event:EventModel):Boolean{
        if(!event.calendarSync||event.calendarEventId==null)return true
        if(!canWrite())return false
        return runCatching{context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,event.calendarEventId),null,null)>=0}.getOrDefault(false)
    }

    private fun fingerprint(e:EventModel)="${e.title}|${e.startEpochMillis}|${e.endEpochMillis}|${e.locationName}|${e.notes}".hashCode().toString()
}
