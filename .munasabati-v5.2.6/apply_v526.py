from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.6')
build = root / 'app/build.gradle.kts'
legacy = root / 'app/src/main/java/com/rafat/munasabati/ui/LegacyScreens.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
android_test = root / 'app/src/androidTest/java/com/rafat/munasabati/compat/CalendarSyncSmokeTest.kt'

# Version bump.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*85\b', 'versionCode = 86', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.5"', 'versionName = "5.2.6"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Install real Android CalendarProvider smoke test.
android_test.parent.mkdir(parents=True, exist_ok=True)
android_test.write_text((assets / 'CalendarSyncSmokeTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Make SharedPreferences registry reads defensive and batch writes during imports.
s = sync_file.read_text(encoding='utf-8')
old = '''    private fun trackedTokens(calendarId:Long):MutableSet<String> =
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toMutableSet() ?: mutableSetOf()'''
new = '''    private fun trackedTokens(calendarId:Long):MutableSet<String> = runCatching {
        importRegistry.getStringSet(registryKey(calendarId), emptySet())?.toMutableSet() ?: mutableSetOf()
    }.getOrElse {
        importRegistry.edit().remove(registryKey(calendarId)).apply()
        mutableSetOf()
    }'''
if old not in s:
    raise SystemExit('trackedTokens anchor missing')
s = s.replace(old, new, 1)

old = '''    private fun rememberImported(calendarId:Long,providerId:Long,appId:String,start:Long){
        val values=trackedTokens(calendarId)
        values.removeAll{ tokenAppId(it)==appId }
        values += "$providerId|$appId|$start"
        importRegistry.edit().putStringSet(registryKey(calendarId),values).apply()
    }'''
new = '''    private fun rememberImportedBatch(calendarId:Long,entries:List<Triple<Long,String,Long>>){
        if(entries.isEmpty()) return
        val values=trackedTokens(calendarId)
        val appIds=entries.map{it.second}.toSet()
        values.removeAll{ tokenAppId(it) in appIds }
        entries.forEach{entry -> values += "${entry.first}|${entry.second}|${entry.third}" }
        importRegistry.edit().putStringSet(registryKey(calendarId),values).apply()
    }'''
if old not in s:
    raise SystemExit('rememberImported anchor missing')
s = s.replace(old, new, 1)

# Reduce provider scan window while still including ongoing multi-day events.
s, n = re.subn(
    r'fun importCalendar\(calendarId:Long,from:Long=System\.currentTimeMillis\(\)-365L\*86_400_000,to:Long=System\.currentTimeMillis\(\)\+730L\*86_400_000\):Int\{',
    'fun importCalendar(calendarId:Long,from:Long=System.currentTimeMillis()-30L*86_400_000,to:Long=System.currentTimeMillis()+730L*86_400_000):Int{',
    s,
    count=1,
)
if n != 1:
    raise SystemExit(f'import range patch failed: {n}')

old = '''        if(!canRead()) return 0
        var count=0
        context.contentResolver.query('''
new = '''        if(!canRead()) return 0
        var count=0
        val existingSnapshot=repo.allEvents()
        val existingByProvider=existingSnapshot.filter{it.calendarId==calendarId && it.calendarEventId!=null}.associateBy{it.calendarEventId}
        val registryBatch=mutableListOf<Triple<Long,String,Long>>()
        context.contentResolver.query('''
if old not in s:
    raise SystemExit('import setup anchor missing')
s = s.replace(old, new, 1)

old = '            val existing=repo.allEvents().firstOrNull{it.calendarEventId==providerId&&it.calendarId==calendarId}'
new = '            val existing=existingByProvider[providerId]'
if old not in s:
    raise SystemExit('existing lookup anchor missing')
s = s.replace(old, new, 1)

old = '            if(CalendarEventOrigin.isImportedFingerprint(origin)) rememberImported(calendarId,providerId,importedEvent.id.toString(),start)'
new = '            if(CalendarEventOrigin.isImportedFingerprint(origin)) registryBatch += Triple(providerId,importedEvent.id.toString(),start)'
if old not in s:
    raise SystemExit('rememberImported call anchor missing')
s = s.replace(old, new, 1)

old = '''            count++
        }}
        return count
    }

    fun syncCalendar'''
new = '''            count++
        }}
        rememberImportedBatch(calendarId,registryBatch)
        return count
    }

    fun syncCalendar'''
if old not in s:
    raise SystemExit('import completion anchor missing')
s = s.replace(old, new, 1)

# Harden provider-facing sync so a vendor CalendarProvider issue becomes a recoverable error.
old = '''    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        cleanupExpiredCalendarEvents()
        val imported=importCalendar(calendarId)
        var pushed=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId&&!isImportedEvent(it)}.forEach{
            if(push(it,calendarId)!=null) pushed++
        }
        cleanupExpiredCalendarEvents()
        return CalendarSyncResult(imported,pushed)
    }'''
new = '''    fun syncCalendar(calendarId:Long):CalendarSyncResult{
        cleanupExpiredCalendarEvents()
        val imported=importCalendar(calendarId)
        var pushed=0
        repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId&&!isImportedEvent(it)}.forEach{
            runCatching { push(it,calendarId) }.getOrNull()?.let { pushed++ }
        }
        cleanupExpiredCalendarEvents()
        return CalendarSyncResult(imported,pushed)
    }'''
if old not in s:
    raise SystemExit('syncCalendar anchor missing')
s = s.replace(old, new, 1)

sync_file.write_text(s, encoding='utf-8')

# Calendar Sync UI: never execute provider/database sync work on the Compose main thread.
s = legacy.read_text(encoding='utf-8')
package_anchor = 'package com.rafat.munasabati.ui\n'
imports = 'package com.rafat.munasabati.ui\n\nimport kotlinx.coroutines.Dispatchers\nimport kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\n'
if 'import kotlinx.coroutines.launch\n' not in s:
    if package_anchor not in s:
        raise SystemExit('LegacyScreens package anchor missing')
    s = s.replace(package_anchor, imports, 1)

old = '''    var calendars by remember { mutableStateOf(sync.calendars()) }
    var status by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }'''
new = '''    var calendars by remember { mutableStateOf(sync.calendars()) }
    var status by remember { mutableStateOf("") }
    var revision by remember { mutableIntStateOf(0) }
    var syncingCalendarId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()'''
if old not in s:
    raise SystemExit('sync screen state anchor missing')
s = s.replace(old, new, 1)

old = '''                            Button(onClick = {
                                val result = sync.syncCalendar(c.id)
                                status = tr("تمت مزامنة ${c.name}: ${result.importedOrUpdated} مستوردة/محدثة و${result.pushed} محلية", "Synced ${c.name}: ${result.importedOrUpdated} imported/updated and ${result.pushed} local")
                                revision++
                            }, modifier = Modifier.fillMaxWidth()) {
                                Icon(Icons.Default.Sync, null)
                                Spacer(Modifier.width(6.dp))
                                Text(tr("مزامنة الآن", "Sync now"))
                            }'''
new = '''                            Button(
                                onClick = {
                                    if (syncingCalendarId == null) {
                                        syncingCalendarId = c.id
                                        status = tr("جارٍ مزامنة ${c.name}…", "Syncing ${c.name}…")
                                        scope.launch {
                                            val outcome = withContext(Dispatchers.IO) { runCatching { sync.syncCalendar(c.id) } }
                                            outcome.onSuccess { result ->
                                                status = tr("تمت مزامنة ${c.name}: ${result.importedOrUpdated} مستوردة/محدثة و${result.pushed} محلية", "Synced ${c.name}: ${result.importedOrUpdated} imported/updated and ${result.pushed} local")
                                            }.onFailure { error ->
                                                status = tr("تعذر مزامنة ${c.name}. تحقق من صلاحية التقويم ثم أعد المحاولة.", "Could not sync ${c.name}. Check calendar permission and try again.")
                                                android.util.Log.e("MunasabatiSync", "Calendar sync failed for ${c.id}", error)
                                            }
                                            syncingCalendarId = null
                                            revision++
                                        }
                                    }
                                },
                                enabled = syncingCalendarId == null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (syncingCalendarId == c.id) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Sync, null)
                                }
                                Spacer(Modifier.width(6.dp))
                                Text(tr(if (syncingCalendarId == c.id) "جارٍ المزامنة…" else "مزامنة الآن", if (syncingCalendarId == c.id) "Syncing…" else "Sync now"))
                            }'''
if old not in s:
    raise SystemExit('sync button anchor missing')
s = s.replace(old, new, 1)

old = '''                            OutlinedButton(onClick = {
                                val result = sync.disconnectCalendar(c.id)
                                status = tr("تم إلغاء مزامنة ${c.name}: حُذفت ${result.importedRemoved} مناسبة مستوردة من التطبيق وفُك ارتباط ${result.localUnlinked} مناسبة محلية. لم يُحذف شيء من التقويم الخارجي.", "Disconnected ${c.name}: removed ${result.importedRemoved} imported event(s) from the app and unlinked ${result.localUnlinked} local event(s). Nothing was deleted from the external calendar.")
                                revision++
                            }, enabled = linked > 0, modifier = Modifier.fillMaxWidth()) {'''
new = '''                            OutlinedButton(onClick = {
                                if (syncingCalendarId == null) {
                                    syncingCalendarId = c.id
                                    scope.launch {
                                        val outcome = withContext(Dispatchers.IO) { runCatching { sync.disconnectCalendar(c.id) } }
                                        outcome.onSuccess { result ->
                                            status = tr("تم إلغاء مزامنة ${c.name}: حُذفت ${result.importedRemoved} مناسبة مستوردة من التطبيق وفُك ارتباط ${result.localUnlinked} مناسبة محلية. لم يُحذف شيء من التقويم الخارجي.", "Disconnected ${c.name}: removed ${result.importedRemoved} imported event(s) from the app and unlinked ${result.localUnlinked} local event(s). Nothing was deleted from the external calendar.")
                                        }.onFailure { error ->
                                            status = tr("تعذر إلغاء مزامنة ${c.name} دون إغلاق التطبيق.", "Could not disconnect ${c.name}; the app remained open.")
                                            android.util.Log.e("MunasabatiSync", "Calendar disconnect failed for ${c.id}", error)
                                        }
                                        syncingCalendarId = null
                                        revision++
                                    }
                                }
                            }, enabled = linked > 0 && syncingCalendarId == null, modifier = Modifier.fillMaxWidth()) {'''
if old not in s:
    raise SystemExit('disconnect button anchor missing')
s = s.replace(old, new, 1)

legacy.write_text(s, encoding='utf-8')
print('Munasabati v5.2.6 safe background calendar sync applied')
