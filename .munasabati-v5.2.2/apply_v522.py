from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.2')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
legacy = root / 'app/src/main/java/com/rafat/munasabati/ui/LegacyScreens.kt'

s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*81\b', 'versionCode = 82', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.1"', 'versionName = "5.2.2"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Add formatter and its unit test.
ui_src = root / 'app/src/main/java/com/rafat/munasabati/ui/RelativeTimeFormatter.kt'
ui_src.write_text((assets / 'RelativeTimeFormatter.kt').read_text(encoding='utf-8'), encoding='utf-8')
test_dir = root / 'app/src/test/java/com/rafat/munasabati/ui'
test_dir.mkdir(parents=True, exist_ok=True)
(test_dir / 'RelativeTimeFormatterTest.kt').write_text((assets / 'RelativeTimeFormatterTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

s = ui.read_text(encoding='utf-8')

# Identify events that actually originated from an external calendar. Events created
# in Munasabati and later pushed to a calendar are not treated as imported events.
anchor = 'private fun v52Greeting(english: Boolean): String = when {'
helper = '''private fun v522IsImportedCalendarEvent(event: EventModel): Boolean =\n    event.calendarSync && event.calendarId != null && event.calendarEventId != null &&\n        Regex("^\\\\d+:\\\\d+:").containsMatchIn(event.calendarFingerprint)\n\nprivate fun v52Greeting(english: Boolean): String = when {'''
if anchor not in s:
    raise SystemExit('greeting anchor missing')
s = s.replace(anchor, helper, 1)

old = '    val previousAll = active.filter { it.endEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
new = '    val previousAll = active.filter { !v522IsImportedCalendarEvent(it) && it.endEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
if old not in s:
    raise SystemExit('previous events filter anchor missing')
s = s.replace(old, new, 1)

old = '    val calendarEvents = active.filter { it.calendarSync && it.calendarId != null && it.startEpochMillis >= todayStart }.sortedBy { it.startEpochMillis }.take(6)'
new = '    val calendarEvents = active.filter { v522IsImportedCalendarEvent(it) && it.startEpochMillis >= todayStart }.sortedBy { it.startEpochMillis }.take(6)'
if old not in s:
    raise SystemExit('calendar events filter anchor missing')
s = s.replace(old, new, 1)

old = '        val upcoming = active.filter { !it.calendarSync && it.startEpochMillis >= tomorrowStart }.sortedBy { it.startEpochMillis }.take(5)'
new = '        val upcoming = active.filter { !v522IsImportedCalendarEvent(it) && it.startEpochMillis >= tomorrowStart }.sortedBy { it.startEpochMillis }.take(5)'
if old not in s:
    raise SystemExit('upcoming filter anchor missing')
s = s.replace(old, new, 1)

pat = re.compile(r'''private fun v52RelativeTime\(ms: Long, english: Boolean\): String \{.*?\n\}''', re.S)
replacement = '''private fun v52RelativeTime(ms: Long, english: Boolean): String {\n    val minutes = ((ms - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L)\n    return V522RelativeTimeFormatter.formatMinutes(minutes, english)\n}'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'relative time patch failed: {n}')
ui.write_text(s, encoding='utf-8')

# Add safe disconnect logic. Imported external events are removed only from the app;
# user-created events that were pushed are preserved locally and simply unlinked.
s = sync_file.read_text(encoding='utf-8')
anchor = '    fun syncMarked():Int{var n=0;repo.allEvents().filter{it.calendarSync&&it.calendarId!=null}.forEach{if(push(it,it.calendarId!!)!=null)n++};return n}\n'
addition = '''    fun syncMarked():Int{var n=0;repo.allEvents().filter{it.calendarSync&&it.calendarId!=null}.forEach{if(push(it,it.calendarId!!)!=null)n++};return n}\n    fun linkedCount(calendarId:Long):Int=repo.allEvents().count{it.calendarSync&&it.calendarId==calendarId}\n    fun disconnectCalendar(calendarId:Long):Int{\n        val linked=repo.allEvents().filter{it.calendarSync&&it.calendarId==calendarId}\n        linked.forEach{event->\n            if(isImported(event)){\n                repo.deleteEvent(event.id)\n            }else{\n                repo.upsertEvent(event.copy(calendarId=null,calendarEventId=null,calendarSync=false,calendarFingerprint="",updatedAt=System.currentTimeMillis()))\n            }\n        }\n        return linked.size\n    }\n    private fun isImported(event:EventModel)=Regex("^\\\\d+:\\\\d+:").containsMatchIn(event.calendarFingerprint)\n'''
if anchor not in s:
    raise SystemExit('syncMarked anchor missing')
s = s.replace(anchor, addition, 1)
sync_file.write_text(s, encoding='utf-8')

# Replace calendar sync UI with per-calendar disconnect controls.
s = legacy.read_text(encoding='utf-8')
pat = re.compile(r'''@Composable fun CalendarSyncScreen\(\) \{.*?\n\}\n\n@Composable fun PrivacyScreen''', re.S)
replacement = '''@Composable fun CalendarSyncScreen() {\n    val context = androidx.compose.ui.platform.LocalContext.current\n    val repo = legacyRepo(context)\n    val sync = remember { CalendarSyncManager(context, repo) }\n    var calendars by remember { mutableStateOf(sync.calendars()) }\n    var status by remember { mutableStateOf("") }\n    var revision by remember { mutableIntStateOf(0) }\n    val linkedCounts = remember(calendars, revision) { calendars.associate { it.id to sync.linkedCount(it.id) } }\n    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { calendars = sync.calendars(); revision++ }\n\n    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {\n        Text(tr("مزامنة التقويم", "Calendar sync"), fontSize = 27.sp, fontWeight = FontWeight.Bold)\n        Text(tr("يدعم تقاويم Google وOutlook الظاهرة في نظام Android. يمكنك استيراد أو تحديث كل تقويم وإلغاء مزامنته بشكل مستقل.", "Supports Google and Outlook calendars exposed by Android. Each calendar can be imported, refreshed, or disconnected independently."))\n        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {\n            Button(onClick = { permission.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) }) { Text(tr("السماح بالوصول للتقويم", "Allow calendar access")) }\n        } else {\n            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {\n                items(calendars, key = { it.id }) { c ->\n                    val linked = linkedCounts[c.id] ?: 0\n                    Card(Modifier.fillMaxWidth()) {\n                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {\n                            ListItem(\n                                headlineContent = { Text(c.name, fontWeight = FontWeight.Bold) },\n                                supportingContent = { Text(if (linked > 0) tr("${c.account} • $linked مناسبة مرتبطة", "${c.account} • $linked linked event(s)") else c.account) },\n                                leadingContent = { Icon(Icons.Default.CalendarMonth, null) }\n                            )\n                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {\n                                TextButton(onClick = {\n                                    val n = sync.importCalendar(c.id)\n                                    status = tr("تم استيراد/تحديث $n مناسبة من ${c.name}", "Imported/updated $n events from ${c.name}")\n                                    revision++\n                                }) { Text(tr("استيراد / تحديث", "Import / Refresh")) }\n                                if (c.writable) TextButton(onClick = {\n                                    val n = repo.allEvents().filter { it.calendarSync }.count { sync.push(it, c.id) != null }\n                                    status = tr("تم دفع $n مناسبة", "Pushed $n events")\n                                    revision++\n                                }) { Text(tr("رفع", "Push")) }\n                                Spacer(Modifier.weight(1f))\n                                OutlinedButton(\n                                    onClick = {\n                                        val n = sync.disconnectCalendar(c.id)\n                                        status = tr("تم إلغاء مزامنة ${c.name} وفك ارتباط $n مناسبة دون حذف أحداث التقويم الخارجي", "Disconnected ${c.name} and unlinked $n event(s) without deleting external calendar events")\n                                        revision++\n                                    },\n                                    enabled = linked > 0\n                                ) { Icon(Icons.Default.LinkOff, null); Spacer(Modifier.width(4.dp)); Text(tr("إلغاء المزامنة", "Disconnect")) }\n                            }\n                        }\n                    }\n                }\n            }\n        }\n        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)\n    }\n}\n\n@Composable fun PrivacyScreen'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'calendar sync screen patch failed: {n}')
legacy.write_text(s, encoding='utf-8')

print('Munasabati v5.2.2 fixes applied')
