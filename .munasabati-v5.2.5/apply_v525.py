from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.5')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'

# Version bump.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*84\b', 'versionCode = 85', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.4"', 'versionName = "5.2.5"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Replace retention policy/tests with tolerant v5.2.5 versions.
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
test_dir = root / 'app/src/test/java/com/rafat/munasabati/compat'
(compat_dir / 'CalendarRetentionPolicy.kt').write_text((assets / 'CalendarRetentionPolicy.kt').read_text(encoding='utf-8'), encoding='utf-8')
(test_dir / 'CalendarRetentionPolicyTest.kt').write_text((assets / 'CalendarRetentionPolicyTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Dashboard: wake up exactly when the next imported event expires.
s = ui.read_text(encoding='utf-8')
if 'import kotlinx.coroutines.delay\n' not in s:
    anchor = 'import kotlinx.coroutines.Dispatchers\n'
    if anchor not in s:
        raise SystemExit('Dispatchers import anchor missing')
    s = s.replace(anchor, anchor + 'import kotlinx.coroutines.delay\n', 1)

anchor = '    val now = ZonedDateTime.now()\n'
expiry = '''    LaunchedEffect(allEvents) {
        val nowMillis = System.currentTimeMillis()
        val nextExpiry = allEvents
            .filter { v522IsImportedCalendarEvent(it) && it.endEpochMillis > nowMillis }
            .minOfOrNull { it.endEpochMillis }
        if (nextExpiry != null) {
            delay((nextExpiry - nowMillis).coerceAtLeast(250L) + 250L)
            refreshKey++
        }
    }

    val now = ZonedDateTime.now()
'''
if anchor not in s:
    raise SystemExit('dashboard now anchor missing')
s = s.replace(anchor, expiry, 1)
ui.write_text(s, encoding='utf-8')

# Sync manager: permanent registry for imported rows + tolerant legacy cleanup.
s = sync_file.read_text(encoding='utf-8')

anchor = '''    private fun canRead()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun canWrite()=ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED
'''
registry = '''    private fun canRead()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun canWrite()=ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED

    private val importRegistry by lazy { context.getSharedPreferences("munasabati_calendar_import_registry_v1", Context.MODE_PRIVATE) }
    private fun registryKey(calendarId:Long)="calendar_$calendarId"
    private fun trackedTokens(calendarId:Long):MutableSet<String> =
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toMutableSet() ?: mutableSetOf()
    private fun tokenAppId(token:String):String? = token.split('|').getOrNull(1)?.takeIf{it.isNotBlank()}
    private fun trackedImportedIds(calendarId:Long):Set<String> = trackedTokens(calendarId).mapNotNull(::tokenAppId).toSet()
    private fun allTrackedImportedIds():Set<String> = importRegistry.all.keys
        .filter{it.startsWith("calendar_")}
        .flatMap{key -> (importRegistry.getStringSet(key, emptySet()) ?: emptySet()).mapNotNull(::tokenAppId)}
        .toSet()
    private fun rememberImported(calendarId:Long,providerId:Long,appId:String,start:Long){
        val values=trackedTokens(calendarId)
        values.removeAll{ tokenAppId(it)==appId }
        values += "$providerId|$appId|$start"
        importRegistry.edit().putStringSet(registryKey(calendarId),values).apply()
    }
    private fun clearTrackedCalendar(calendarId:Long){ importRegistry.edit().remove(registryKey(calendarId)).apply() }
    private fun isTrackedImported(event:EventModel):Boolean = allTrackedImportedIds().contains(event.id.toString())
'''
if anchor not in s:
    raise SystemExit('permission helper anchor missing')
s = s.replace(anchor, registry, 1)

# Persist imported app-row identity every time a provider event is imported/updated.
old = '''            repo.upsertEvent(base.copy(
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
            ))
            count++'''
new = '''            val importedEvent=base.copy(
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
            if(CalendarEventOrigin.isImportedFingerprint(origin)) rememberImported(calendarId,providerId,importedEvent.id.toString(),start)
            count++'''
if old not in s:
    raise SystemExit('import upsert anchor missing')
s = s.replace(old, new, 1)

# Imported identity is recognized by either explicit fingerprint or permanent registry.
old = '''    fun isImportedEvent(event:EventModel):Boolean=
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)'''
new = '''    fun isImportedEvent(event:EventModel):Boolean=
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint) || isTrackedImported(event)'''
if old not in s:
    raise SystemExit('isImportedEvent anchor missing')
s = s.replace(old, new, 1)

# Linked count includes registry-tracked imported rows too.
old = '    fun linkedCount(calendarId:Long):Int=repo.allEvents().count{it.calendarSync&&it.calendarId==calendarId}'
new = '''    fun linkedCount(calendarId:Long):Int{
        val tracked=trackedImportedIds(calendarId)
        return repo.allEvents().count{it.calendarId==calendarId || tracked.contains(it.id.toString())}
    }'''
if old not in s:
    raise SystemExit('linkedCount anchor missing')
s = s.replace(old, new, 1)

# Disconnect removes all known imports for that calendar, including old orphan rows matched back to provider.
pat = re.compile(r'''    fun disconnectCalendar\(calendarId:Long\):CalendarDisconnectResult\{.*?\n    \}\n\n    fun isImportedEvent''', re.S)
replacement = '''    fun disconnectCalendar(calendarId:Long):CalendarDisconnectResult{
        var removed=cleanupLegacyCalendarMatches(calendarId,System.currentTimeMillis(),endedOnly=false)
        val tracked=trackedImportedIds(calendarId)
        val linked=repo.allEvents().filter{it.calendarId==calendarId || tracked.contains(it.id.toString())}
        var unlinked=0
        linked.forEach{event->
            if(isImportedEvent(event) || tracked.contains(event.id.toString())){
                if(repo.deleteEvent(event.id)>0) removed++
            }else{
                repo.upsertEvent(event.copy(calendarId=null,calendarEventId=null,calendarSync=false,calendarFingerprint="",updatedAt=System.currentTimeMillis()))
                unlinked++
            }
        }
        clearTrackedCalendar(calendarId)
        cleanupExpiredCalendarEvents()
        return CalendarDisconnectResult(removed,unlinked)
    }

    fun isImportedEvent'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'disconnect function patch failed: {n}')

# Replace v5.2.4 exact legacy cleanup with tolerant title/start matching.
pat = re.compile(r'''    private fun cleanupLegacyDisconnectedCalendarEvents\(now:Long\):Int\{.*?\n    \}\n\n    fun deleteLinked''', re.S)
replacement = r'''    private fun cleanupLegacyDisconnectedCalendarEvents(now:Long):Int =
        cleanupLegacyCalendarMatches(null,now,endedOnly=true)

    private fun cleanupLegacyCalendarMatches(calendarId:Long?,now:Long,endedOnly:Boolean):Int{
        if(!canRead()) return 0
        val earliest=now-730L*86_400_000L
        val latest=if(endedOnly) now else now+730L*86_400_000L
        val providerEvents=mutableListOf<Pair<String,Long>>()
        val selection=if(calendarId==null)
            "${CalendarContract.Events.DELETED}=0 AND ${CalendarContract.Events.DTSTART} BETWEEN ? AND ?"
        else
            "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.DELETED}=0 AND ${CalendarContract.Events.DTSTART} BETWEEN ? AND ?"
        val args=if(calendarId==null)
            arrayOf(earliest.toString(),latest.toString())
        else
            arrayOf(calendarId.toString(),earliest.toString(),latest.toString())
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events.TITLE,CalendarContract.Events.DTSTART,CalendarContract.Events.DTEND),
            selection,args,null
        )?.use{c->while(c.moveToNext()){
            val title=c.getString(0)?.trim().orEmpty()
            if(title.isBlank()) continue
            val start=c.getLong(1)
            val end=c.getLong(2).takeIf{it>start}?:start+3_600_000L
            if(endedOnly && end>now) continue
            providerEvents += title to start
        }}
        if(providerEvents.isEmpty()) return 0
        var removed=0
        repo.allEvents().filter{event->
            !event.calendarSync && event.calendarId==null && event.calendarEventId==null &&
            event.calendarFingerprint.isBlank() && event.category==EventCategory.OTHER &&
            (!endedOnly || event.endEpochMillis<=now) &&
            providerEvents.any{provider -> CalendarRetentionPolicy.likelyLegacyCalendarMatch(event.title,event.startEpochMillis,provider.first,provider.second)}
        }.forEach{event-> if(repo.deleteEvent(event.id)>0) removed++ }
        return removed
    }

    fun deleteLinked'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'legacy cleanup patch failed: {n}')

# Registry-tracked expired imports are purged even if old fields were damaged.
old = '''        repo.allEvents().filter{isImportedEvent(it)&&!CalendarRetentionPolicy.shouldKeepImported(it.endEpochMillis,now)}.forEach{event->
            if(repo.deleteEvent(event.id)>0) removed++
        }'''
new = '''        val tracked=allTrackedImportedIds()
        repo.allEvents().filter{(isImportedEvent(it)||tracked.contains(it.id.toString()))&&!CalendarRetentionPolicy.shouldKeepImported(it.endEpochMillis,now)}.forEach{event->
            if(repo.deleteEvent(event.id)>0) removed++
        }'''
if old not in s:
    raise SystemExit('expired cleanup anchor missing')
s = s.replace(old, new, 1)

sync_file.write_text(s, encoding='utf-8')
print('Munasabati v5.2.5 calendar identity cleanup applied')
