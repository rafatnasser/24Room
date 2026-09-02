from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.4')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'

# Version bump.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*83\b', 'versionCode = 84', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.3"', 'versionName = "5.2.4"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Install retention helper + tests.
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
compat_dir.mkdir(parents=True, exist_ok=True)
(compat_dir / 'CalendarRetentionPolicy.kt').write_text((assets / 'CalendarRetentionPolicy.kt').read_text(encoding='utf-8'), encoding='utf-8')
test_dir = root / 'app/src/test/java/com/rafat/munasabati/compat'
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / 'CalendarRetentionPolicyTest.kt').write_text((assets / 'CalendarRetentionPolicyTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Dashboard/search/calendar: imported origin is permanent, expired imported events never belong to history.
s = ui.read_text(encoding='utf-8')
old = '''private fun v522IsImportedCalendarEvent(event: EventModel): Boolean =
    event.calendarSync && event.calendarId != null && event.calendarEventId != null &&
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)'''
new = '''private fun v522IsImportedCalendarEvent(event: EventModel): Boolean =
    CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)'''
if old not in s:
    raise SystemExit('imported event helper anchor missing')
s = s.replace(old, new, 1)

# Clean calendar events before dashboard reads them.
old = '''            val repo = v52Repo(context)
            allEvents = repo.allEvents()
            calendarSources = runCatching {'''
new = '''            val repo = v52Repo(context)
            runCatching { CalendarSyncManager(context, repo).cleanupExpiredCalendarEvents() }
            allEvents = repo.allEvents()
            calendarSources = runCatching {'''
if old not in s:
    raise SystemExit('dashboard repository load anchor missing')
s = s.replace(old, new, 1)

# Imported calendar section contains only currently active/future imported events.
old = '    val calendarEvents = active.filter { v522IsImportedCalendarEvent(it) }.sortedBy { kotlin.math.abs(it.startEpochMillis - System.currentTimeMillis()) }.take(8)'
new = '    val calendarEvents = active.filter { v522IsImportedCalendarEvent(it) && it.endEpochMillis > System.currentTimeMillis() }.sortedBy { it.startEpochMillis }.take(8)'
if old not in s:
    raise SystemExit('calendar events filter anchor missing')
s = s.replace(old, new, 1)

# Search must not return an expired imported event; also prune old imports when opening search.
old = '    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val r = v52Repo(context); events = r.allEvents(); people = r.people().associate { it.id to it.name } } }'
new = '''    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { CalendarSyncManager(context, r).cleanupExpiredCalendarEvents() }
        events = r.allEvents().filter { !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis()) }
        people = r.people().associate { it.id to it.name }
    } }'''
if old not in s:
    raise SystemExit('search load anchor missing')
s = s.replace(old, new, 1)

# Calendar screen must not display expired imported events.
old = '    LaunchedEffect(Unit) { allEvents = withContext(Dispatchers.IO) { v52Repo(context).allEvents() } }'
new = '''    LaunchedEffect(Unit) { allEvents = withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { CalendarSyncManager(context, r).cleanupExpiredCalendarEvents() }
        r.allEvents().filter { !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis()) }
    } }'''
if old not in s:
    raise SystemExit('calendar load anchor missing')
s = s.replace(old, new, 1)

ui.write_text(s, encoding='utf-8')

# Calendar sync manager: remove expired imports, remove old disconnected leftovers, never re-import ended items.
s = sync_file.read_text(encoding='utf-8')
old = '''    fun isImportedEvent(event:EventModel):Boolean=
        event.calendarSync&&event.calendarId!=null&&event.calendarEventId!=null&&CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)'''
new = '''    fun isImportedEvent(event:EventModel):Boolean=
        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)'''
if old not in s:
    raise SystemExit('CalendarSync isImportedEvent anchor missing')
s = s.replace(old, new, 1)

# Prevent ended provider events from being inserted or retained during import.
old = '''            val start=c.getLong(4)
            val end=c.getLong(5).takeIf{it>start}?:start+3_600_000
            val existing=repo.allEvents().firstOrNull{it.calendarEventId==providerId&&it.calendarId==calendarId}
            val existingIsImported=existing?.let(::isImportedEvent) ?: true'''
new = '''            val start=c.getLong(4)
            val end=c.getLong(5).takeIf{it>start}?:start+3_600_000
            val existing=repo.allEvents().firstOrNull{it.calendarEventId==providerId&&it.calendarId==calendarId}
            if(!CalendarRetentionPolicy.shouldKeepImported(end,System.currentTimeMillis())){
                if(existing!=null && isImportedEvent(existing)) repo.deleteEvent(existing.id)
                continue
            }
            val existingIsImported=existing?.let(::isImportedEvent) ?: true'''
if old not in s:
    raise SystemExit('import expiration anchor missing')
s = s.replace(old, new, 1)

# Disconnect all records tied to a calendar id, even if an older build left calendarSync false.
old = '        val linked=repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId}'
new = '        val linked=repo.allEvents().filter{it.calendarId==calendarId}'
if old not in s:
    raise SystemExit('disconnect linked anchor missing')
s = s.replace(old, new, 1)

# Add cleanup API before deleteLinked.
anchor = '''    fun deleteLinked(event:EventModel):Boolean{'''
cleanup = r'''    fun cleanupExpiredCalendarEvents(now:Long=System.currentTimeMillis()):Int{
        var removed=0
        repo.allEvents().filter{isImportedEvent(it)&&!CalendarRetentionPolicy.shouldKeepImported(it.endEpochMillis,now)}.forEach{event->
            if(repo.deleteEvent(event.id)>0) removed++
        }
        removed += cleanupLegacyDisconnectedCalendarEvents(now)
        return removed
    }

    private fun cleanupLegacyDisconnectedCalendarEvents(now:Long):Int{
        if(!canRead()) return 0
        val earliest=now-730L*86_400_000L
        val providerSignatures=mutableSetOf<String>()
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events.TITLE,CalendarContract.Events.DESCRIPTION,CalendarContract.Events.EVENT_LOCATION,CalendarContract.Events.DTSTART,CalendarContract.Events.DTEND),
            "${CalendarContract.Events.DELETED}=0 AND ${CalendarContract.Events.DTSTART} BETWEEN ? AND ?",
            arrayOf(earliest.toString(),now.toString()),
            null
        )?.use{c->while(c.moveToNext()){
            val title=c.getString(0)?.trim().orEmpty()
            if(title.isBlank()) continue
            val start=c.getLong(3)
            val end=c.getLong(4).takeIf{it>start}?:start+3_600_000L
            if(end>now) continue
            providerSignatures += CalendarRetentionPolicy.legacySignature(title,start,end,c.getString(2)?:"",c.getString(1)?:"")
        }}
        if(providerSignatures.isEmpty()) return 0
        var removed=0
        repo.allEvents().filter{event->
            !event.calendarSync && event.calendarId==null && event.calendarEventId==null &&
            event.calendarFingerprint.isBlank() && event.endEpochMillis<=now && event.category==EventCategory.OTHER &&
            providerSignatures.contains(CalendarRetentionPolicy.legacySignature(event.title,event.startEpochMillis,event.endEpochMillis,event.locationName,event.notes))
        }.forEach{event-> if(repo.deleteEvent(event.id)>0) removed++ }
        return removed
    }

    fun deleteLinked(event:EventModel):Boolean{'''
if anchor not in s:
    raise SystemExit('deleteLinked anchor missing')
s = s.replace(anchor, cleanup, 1)

# Each explicit sync starts and finishes with pruning so expired imports cannot survive.
old = '''    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        val imported=importCalendar(calendarId)
        var pushed=0'''
new = '''    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        cleanupExpiredCalendarEvents()
        val imported=importCalendar(calendarId)
        var pushed=0'''
if old not in s:
    raise SystemExit('syncCalendar opening anchor missing')
s = s.replace(old, new, 1)
old = '''        return CalendarSyncResult(imported,pushed)
    }

    fun syncMarked():Int{'''
new = '''        cleanupExpiredCalendarEvents()
        return CalendarSyncResult(imported,pushed)
    }

    fun syncMarked():Int{'''
if old not in s:
    raise SystemExit('syncCalendar closing anchor missing')
s = s.replace(old, new, 1)

sync_file.write_text(s, encoding='utf-8')
print('Munasabati v5.2.4 calendar retention fixes applied')
