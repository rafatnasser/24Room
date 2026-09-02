from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
build = root / 'app/build.gradle.kts'

s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*80\b', 'versionCode = 81', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.0"', 'versionName = "5.2.1"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

s = ui.read_text(encoding='utf-8')
needle = 'import com.rafat.munasabati.compat.AppPreferences\n'
if 'import com.rafat.munasabati.compat.CalendarSyncManager\n' not in s:
    if needle not in s:
        raise SystemExit('AppPreferences import anchor missing')
    s = s.replace(needle, needle + 'import com.rafat.munasabati.compat.CalendarSyncManager\n', 1)

old = '''    var suggestionDone by remember { mutableStateOf<String?>(null) }\n\n    LaunchedEffect(refreshKey) {\n        allEvents = withContext(Dispatchers.IO) { v52Repo(context).allEvents() }\n    }'''
new = '''    var suggestionDone by remember { mutableStateOf<String?>(null) }\n    var calendarSources by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }\n\n    LaunchedEffect(refreshKey) {\n        withContext(Dispatchers.IO) {\n            val repo = v52Repo(context)\n            allEvents = repo.allEvents()\n            calendarSources = runCatching {\n                CalendarSyncManager(context, repo).calendars().associate { calendar ->\n                    val base = calendar.name.ifBlank { v52Tr(english, "تقويم الجهاز", "Device calendar") }\n                    val source = if (calendar.account.isNotBlank() && !calendar.account.equals(base, ignoreCase = true)) "$base • ${calendar.account}" else base\n                    calendar.id to source\n                }\n            }.getOrDefault(emptyMap())\n        }\n    }'''
if old not in s:
    pat = re.compile(r'    var suggestionDone by remember \{ mutableStateOf<String\?>\(null\) \}\n\s*LaunchedEffect\(refreshKey\) \{ allEvents = withContext\(Dispatchers\.IO\) \{ v52Repo\(context\)\.allEvents\(\) \} \}')
    s, n = pat.subn(new, s, count=1)
    if n != 1:
        raise SystemExit('dashboard load anchor missing')
else:
    s = s.replace(old, new, 1)

anchor = '    val suggestions = allEvents.mapNotNull { SmartSuggestionEngine.suggestionFor(it) }.sortedBy { it.daysUntil }.take(3)'
insert = '''    val suggestions = allEvents.mapNotNull { SmartSuggestionEngine.suggestionFor(it) }.sortedBy { it.daysUntil }.take(3)\n    val previousAll = active.filter { it.endEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }\n    val previousEvents = previousAll.take(6)\n    val calendarEvents = active.filter { it.calendarSync && it.calendarId != null && it.startEpochMillis >= todayStart }.sortedBy { it.startEpochMillis }.take(6)'''
if anchor not in s:
    raise SystemExit('suggestions anchor missing')
s = s.replace(anchor, insert, 1)

pat = re.compile(r'''        item \{ V52SectionTitle\(Icons\.Default\.Upcoming, v52Tr\(english, "القادم", "Coming up"\)\) \}\n        val upcoming = active\.filter \{ it\.startEpochMillis >= tomorrowStart \}\.sortedBy \{ it\.startEpochMillis \}\.take\(5\)\n        if \(upcoming\.isEmpty\(\)\) item \{ V52EmptyCard\(v52Tr\(english, "لا توجد مناسبات قادمة", "No upcoming events"\)\) \}\n        else items\(upcoming, key = \{ "up-\$\{it\.id\}" \}\) \{ V52EventCard\(it, english\) \}''')
replacement = '''        item { V52SectionTitle(Icons.Default.CalendarMonth, v52Tr(english, "مناسبات تقويمية", "Calendar events"), calendarEvents.size) }\n        if (calendarEvents.isEmpty()) {\n            item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات قادمة مستوردة من تقاويم الجهاز", "No upcoming events imported from device calendars")) }\n        } else {\n            items(calendarEvents, key = { "calendar-${it.id}" }) { event ->\n                V52EventCard(event, english, source = v52CalendarSource(event, calendarSources, english))\n            }\n        }\n\n        item { V52SectionTitle(Icons.Default.Upcoming, v52Tr(english, "القادم", "Coming up")) }\n        val upcoming = active.filter { !it.calendarSync && it.startEpochMillis >= tomorrowStart }.sortedBy { it.startEpochMillis }.take(5)\n        if (upcoming.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات قادمة", "No upcoming events")) }\n        else items(upcoming, key = { "up-${it.id}" }) { V52EventCard(it, english) }\n\n        item { V52SectionTitle(Icons.Default.History, v52Tr(english, "المناسبات السابقة", "Previous events"), previousAll.size) }\n        if (previousEvents.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات سابقة", "No previous events")) }\n        else items(previousEvents, key = { "previous-${it.id}" }) { V52EventCard(it, english) }'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'dashboard tail patch failed: {n}')

pat = re.compile(r'''private fun v52RelativeTime\(ms: Long, english: Boolean\): String \{.*?\n\}''', re.S)
replacement = '''private fun v52RelativeTime(ms: Long, english: Boolean): String {\n    val minutes = ((ms - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L)\n    return when {\n        minutes < 60 -> if (english) "${minutes} min" else "${minutes} دقيقة"\n        minutes < 1440 -> {\n            val hours = minutes / 60\n            if (english) "$hours hr" else "$hours ساعة"\n        }\n        else -> {\n            val days = minutes / 1440\n            if (english) "$days day${if (days == 1L) "" else "s"}" else "$days يوم"\n        }\n    }\n}'''
s, n = pat.subn(replacement, s, count=1)
if n != 1:
    raise SystemExit(f'relative time patch failed: {n}')

s, n = re.subn(r'Text\(value,\s*fontSize\s*=\s*19\.sp,\s*fontWeight\s*=\s*FontWeight\.Bold,\s*maxLines\s*=\s*1\)', 'Text(value, fontSize = 18.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2)', s, count=1)
if n != 1:
    raise SystemExit('metric value patch failed')

helper_anchor = '@Composable\nprivate fun V52SectionTitle'
helper = '''private fun v52CalendarSource(event: EventModel, sources: Map<Long, String>, english: Boolean): String {\n    val id = event.calendarId\n    return if (id != null) sources[id] ?: v52Tr(english, "تقويم الجهاز", "Device calendar")\n    else v52Tr(english, "تقويم الجهاز", "Device calendar")\n}\n\n@Composable\nprivate fun V52SectionTitle'''
if helper_anchor not in s:
    raise SystemExit('section title anchor missing')
s = s.replace(helper_anchor, helper, 1)

s, n = re.subn(r'private fun V52EventCard\(event: EventModel, english: Boolean, modifier: Modifier = Modifier\)', 'private fun V52EventCard(event: EventModel, english: Boolean, modifier: Modifier = Modifier, source: String? = null)', s, count=1)
if n != 1:
    raise SystemExit('event card signature patch failed')

anchor = '            Text(v52DateTime(event.startEpochMillis, english), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)'
source_row = '''            Text(v52DateTime(event.startEpochMillis, english), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)\n            if (!source.isNullOrBlank()) {\n                Row(verticalAlignment = Alignment.CenterVertically) {\n                    Icon(Icons.Default.Event, null, tint = visual.color, modifier = Modifier.size(16.dp))\n                    Spacer(Modifier.width(6.dp))\n                    Text(v52Tr(english, "المصدر: $source", "Source: $source"), fontSize = 12.sp, color = visual.color, fontWeight = FontWeight.SemiBold)\n                }\n            }'''
if anchor not in s:
    raise SystemExit('event card date anchor missing')
s = s.replace(anchor, source_row, 1)

ui.write_text(s, encoding='utf-8')
print('Munasabati v5.2.1 dashboard fixes applied')
