from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.3')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
legacy = root / 'app/src/main/java/com/rafat/munasabati/ui/LegacyScreens.kt'

s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*82\b', 'versionCode = 83', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.2"', 'versionName = "5.2.3"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Install explicit origin helper and tests.
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
compat_dir.mkdir(parents=True, exist_ok=True)
(compat_dir / 'CalendarEventOrigin.kt').write_text((assets / 'CalendarEventOrigin.kt').read_text(encoding='utf-8'), encoding='utf-8')
test_dir = root / 'app/src/test/java/com/rafat/munasabati/compat'
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / 'CalendarEventOriginTest.kt').write_text((assets / 'CalendarEventOriginTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Fix dashboard filtering and expose safe removal for leftovers in Previous events.
s = ui.read_text(encoding='utf-8')
if 'import com.rafat.munasabati.compat.CalendarEventOrigin\n' not in s:
    anchor = 'import com.rafat.munasabati.compat.CalendarSyncManager\n'
    if anchor not in s:
        raise SystemExit('CalendarSyncManager import anchor missing')
    s = s.replace(anchor, anchor + 'import com.rafat.munasabati.compat.CalendarEventOrigin\n', 1)

s, n = re.subn(
    r'private fun v522IsImportedCalendarEvent\(event: EventModel\): Boolean =\n\s*event\.calendarSync.*?containsMatchIn\(event\.calendarFingerprint\)',
    'private fun v522IsImportedCalendarEvent(event: EventModel): Boolean =\n    event.calendarSync && event.calendarId != null && event.calendarEventId != null &&\n        CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)',
    s,
    count=1,
    flags=re.S,
)
if n != 1:
    raise SystemExit(f'imported helper patch failed: {n}')

old = '    val previousAll = active.filter { !v522IsImportedCalendarEvent(it) && it.endEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
new = '    val previousAll = active.filter { !v522IsImportedCalendarEvent(it) && it.startEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
if old not in s:
    raise SystemExit('previous filter anchor missing')
s = s.replace(old, new, 1)

old = '    val calendarEvents = active.filter { v522IsImportedCalendarEvent(it) && it.startEpochMillis >= todayStart }.sortedBy { it.startEpochMillis }.take(6)'
new = '    val calendarEvents = active.filter { v522IsImportedCalendarEvent(it) }.sortedBy { kotlin.math.abs(it.startEpochMillis - System.currentTimeMillis()) }.take(8)'
if old not in s:
    raise SystemExit('calendar events filter anchor missing')
s = s.replace(old, new, 1)

old = '            items(calendarEvents, key = { "calendar-${it.id}" }) { event ->\n                V52EventCard(event, english, source = v52CalendarSource(event, calendarSources, english))\n            }'
new = '''            items(calendarEvents, key = { "calendar-${it.id}" }) { event ->
                V52EventCard(
                    event,
                    english,
                    source = v52CalendarSource(event, calendarSources, english),
                    onRemoveFromApp = {
                        runCatching { ReminderScheduler(context).cancel(event.id) }
                        v52Repo(context).deleteEvent(event.id)
                        refreshKey++
                    }
                )
            }'''
if old not in s:
    raise SystemExit('calendar item anchor missing')
s = s.replace(old, new, 1)

old = '        else items(previousEvents, key = { "previous-${it.id}" }) { V52EventCard(it, english) }'
new = '''        else items(previousEvents, key = { "previous-${it.id}" }) { event ->
            V52EventCard(event, english, onRemoveFromApp = {
                runCatching { ReminderScheduler(context).cancel(event.id) }
                v52Repo(context).deleteEvent(event.id)
                refreshKey++
            })
        }'''
if old not in s:
    raise SystemExit('previous item anchor missing')
s = s.replace(old, new, 1)

s, n = re.subn(
    r'private fun V52EventCard\(event: EventModel, english: Boolean, modifier: Modifier = Modifier, source: String\? = null\)',
    'private fun V52EventCard(event: EventModel, english: Boolean, modifier: Modifier = Modifier, source: String? = null, onRemoveFromApp: (() -> Unit)? = null)',
    s,
    count=1,
)
if n != 1:
    raise SystemExit('event card signature patch failed')

anchor = '''                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = {}, label = { Text(if (event.recurrence.equals("yearly", true)) v52Tr(english, "سنوية", "Yearly") else v52Tr(english, "مرة واحدة", "One-time")) }, leadingIcon = { Icon(Icons.Default.Repeat, null, modifier = Modifier.size(16.dp)) })
                        AssistChip(onClick = {}, label = { Text(v52Tr(english, "قبل ${event.reminderMinutes} د", "${event.reminderMinutes}m before")) }, leadingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp)) })
                    }'''
replacement = anchor + '''
                    if (onRemoveFromApp != null) {
                        OutlinedButton(onClick = onRemoveFromApp, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.DeleteOutline, null, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(v52Tr(english, "إزالة من مناسبـاتي", "Remove from Munasabati"))
                        }
                    }'''
if anchor not in s:
    raise SystemExit('event card expanded anchor missing')
s = s.replace(anchor, replacement, 1)
ui.write_text(s, encoding='utf-8')

# Replace calendar sync manager with explicit origin semantics.
sync_source = r'''package com.rafat.munasabati.compat

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.rafat.munasabati.data.EventRepository
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import java.util.*

data class SystemCalendar(val id:Long,val name:String,val account:String,val writable:Boolean)
data class CalendarSyncResult(val importedOrUpdated:Int,val pushed:Int)
data class CalendarDisconnectResult(val importedRemoved:Int,val localUnlinked:Int) {
    val total:Int get() = importedRemoved + localUnlinked
}

class CalendarSyncManager(private val context:Context,private val repo:EventRepository){
    private fun canRead()=ContextCompat.checkSelfPermission(context,Manifest.permission.READ_CALENDAR)==PackageManager.PERMISSION_GRANTED
    private fun canWrite()=ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_CALENDAR)==PackageManager.PERMISSION_GRANTED

    fun calendars():List<SystemCalendar>{
        if(!canRead()) return emptyList()
        val out=mutableListOf<SystemCalendar>()
        context.contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            arrayOf(CalendarContract.Calendars._ID,CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,CalendarContract.Calendars.ACCOUNT_NAME,CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL),
            "${CalendarContract.Calendars.VISIBLE}=1",null,"${CalendarContract.Calendars.CALENDAR_DISPLAY_NAME} COLLATE NOCASE"
        )?.use{c->while(c.moveToNext())out+=SystemCalendar(c.getLong(0),c.getString(1)?:"تقويم",c.getString(2)?:"",c.getInt(3)>=CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR)}
        return out
    }

    fun push(event:EventModel,calendarId:Long):Long?{
        if(!canWrite()) return null
        val values=ContentValues().apply{
            put(CalendarContract.Events.CALENDAR_ID,calendarId)
            put(CalendarContract.Events.TITLE,event.title)
            put(CalendarContract.Events.DESCRIPTION,event.notes)
            put(CalendarContract.Events.EVENT_LOCATION,event.locationName)
            put(CalendarContract.Events.DTSTART,event.startEpochMillis)
            put(CalendarContract.Events.DTEND,event.endEpochMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE,TimeZone.getDefault().id)
        }
        val uri=if(event.calendarEventId!=null){
            val target=ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,event.calendarEventId)
            context.contentResolver.update(target,values,null,null)
            target
        }else context.contentResolver.insert(CalendarContract.Events.CONTENT_URI,values)
        val id=uri?.lastPathSegment?.toLongOrNull()
        if(id!=null){
            val origin=if(isImportedEvent(event)) event.calendarFingerprint else CalendarEventOrigin.localFingerprint(fingerprint(event))
            repo.upsertEvent(event.copy(calendarId=calendarId,calendarEventId=id,calendarSync=true,calendarFingerprint=origin,updatedAt=System.currentTimeMillis()))
        }
        return id
    }

    fun importCalendar(calendarId:Long,from:Long=System.currentTimeMillis()-365L*86_400_000,to:Long=System.currentTimeMillis()+730L*86_400_000):Int{
        if(!canRead()) return 0
        var count=0
        context.contentResolver.query(
            CalendarContract.Events.CONTENT_URI,
            arrayOf(CalendarContract.Events._ID,CalendarContract.Events.TITLE,CalendarContract.Events.DESCRIPTION,CalendarContract.Events.EVENT_LOCATION,CalendarContract.Events.DTSTART,CalendarContract.Events.DTEND),
            "${CalendarContract.Events.CALENDAR_ID}=? AND ${CalendarContract.Events.DELETED}=0 AND ${CalendarContract.Events.DTSTART} BETWEEN ? AND ?",
            arrayOf(calendarId.toString(),from.toString(),to.toString()),
            CalendarContract.Events.DTSTART
        )?.use{c->while(c.moveToNext()){
            val providerId=c.getLong(0)
            val title=c.getString(1)?.trim().orEmpty()
            if(title.isBlank()) continue
            val start=c.getLong(4)
            val end=c.getLong(5).takeIf{it>start}?:start+3_600_000
            val existing=repo.allEvents().firstOrNull{it.calendarEventId==providerId&&it.calendarId==calendarId}
            val existingIsImported=existing?.let(::isImportedEvent) ?: true
            val base=existing?:EventModel(title=title,startEpochMillis=start,endEpochMillis=end,category=EventCategory.OTHER)
            val origin=if(existingIsImported) CalendarEventOrigin.importedFingerprint(calendarId,providerId,start)
                else CalendarEventOrigin.localFingerprint(fingerprint(base))
            repo.upsertEvent(base.copy(
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
            count++
        }}
        return count
    }

    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        val imported=importCalendar(calendarId)
        var pushed=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId&&!isImportedEvent(it)}.forEach{
            if(push(it,calendarId)!=null) pushed++
        }
        return CalendarSyncResult(imported,pushed)
    }

    fun syncMarked():Int{
        var n=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId!=null&&!isImportedEvent(it)}.forEach{
            if(push(it,it.calendarId!!)!=null)n++
        }
        return n
    }

    fun linkedCount(calendarId:Long):Int=repo.allEvents().count{it.calendarSync&&it.calendarId==calendarId}

    fun disconnectCalendar(calendarId:Long):CalendarDisconnectResult{
        val linked=repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId}
        var removed=0
        var unlinked=0
        linked.forEach{event->
            if(isImportedEvent(event)){
                if(repo.deleteEvent(event.id)>0) removed++
            }else{
                repo.upsertEvent(event.copy(calendarId=null,calendarEventId=null,calendarSync=false,calendarFingerprint="",updatedAt=System.currentTimeMillis()))
                unlinked++
            }
        }
        return CalendarDisconnectResult(removed,unlinked)
    }

    fun isImportedEvent(event:EventModel):Boolean=
        event.calendarSync&&event.calendarId!=null&&event.calendarEventId!=null&&CalendarEventOrigin.isImportedFingerprint(event.calendarFingerprint)

    fun deleteLinked(event:EventModel):Boolean{
        if(!event.calendarSync||event.calendarEventId==null)return true
        if(!canWrite())return false
        return runCatching{context.contentResolver.delete(ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI,event.calendarEventId),null,null)>=0}.getOrDefault(false)
    }

    private fun fingerprint(e:EventModel)="${e.title}|${e.startEpochMillis}|${e.endEpochMillis}|${e.locationName}|${e.notes}".hashCode().toString()
}
'''
sync_file.write_text(sync_source, encoding='utf-8')

# Simplify sync screen: one Sync button + one clearly visible Disconnect button.
s = legacy.read_text(encoding='utf-8')
pat = re.compile(r'''@Composable fun CalendarSyncScreen\(\) \{.*?\n\}\n\n@Composable fun PrivacyScreen''', re.S)
replacement = '''@Composable fun CalendarSyncScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = legacyRepo(context)
    val sync = remember { CalendarSyncManager(context, repo) }
    var calendars by remember { mutableStateOf(sync.calendars()) }
    var status by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    val linkedCounts = remember(calendars, revision) { calendars.associate { it.id to sync.linkedCount(it.id) } }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { calendars = sync.calendars(); revision++ }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(tr("مزامنة التقويم", "Calendar sync"), fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text(tr("لكل تقويم زر مزامنة واحد وزر مستقل لإلغاء المزامنة. إلغاء المزامنة لا يحذف شيئًا من Google أو Outlook.", "Each calendar has one Sync button and a separate Disconnect button. Disconnect never deletes anything from Google or Outlook."))
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            Button(onClick = { permission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }, modifier = Modifier.fillMaxWidth()) {
                Text(tr("السماح بالوصول للتقويم", "Allow calendar access"))
            }
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(calendars, key = { it.id }) { c ->
                    val linked = linkedCounts[c.id] ?: 0
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CalendarMonth, null)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(c.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                                    Text(if (linked > 0) tr("${c.account} • $linked مناسبة مرتبطة", "${c.account} • $linked linked event(s)") else c.account, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Button(onClick = {
                                val result = sync.syncCalendar(c.id)
                                status = tr("تمت مزامنة ${c.name}: ${result.importedOrUpdated} مستوردة/محدثة و${result.pushed} محلية", "Synced ${c.name}: ${result.importedOrUpdated} imported/updated and ${result.pushed} local")
                                revision++
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Sync, null)
                                Spacer(Modifier.width(6.dp))
                                Text(tr("مزامنة الآن", "Sync now"))
                            }
                            OutlinedButton(onClick = {
                                val result = sync.disconnectCalendar(c.id)
                                status = tr("تم إلغاء مزامنة ${c.name}: حُذفت ${result.importedRemoved} مناسبة مستوردة من التطبيق وفُك ارتباط ${result.localUnlinked} مناسبة محلية. لم يُحذف شيء من التقويم الخارجي.", "Disconnected ${c.name}: removed ${result.importedRemoved} imported event(s) from the app and unlinked ${result.localUnlinked} local event(s). Nothing was deleted from the external calendar.")
                                revision++
                            }, enabled = linked > 0, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.LinkOff, null)
                                Spacer(Modifier.width(6.dp))
                                Text(tr("إلغاء المزامنة", "Disconnect"))
                            }
                        }
                    }
                }
            }
        }
        if (status.isNotBlank()) Card { Text(status, Modifier.fillMaxWidth().padding(12.dp), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium) }
    }
}

@Composable fun PrivacyScreen'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'calendar sync screen patch failed: {n}')
legacy.write_text(s, encoding='utf-8')

print('Munasabati v5.2.3 fixes applied')
