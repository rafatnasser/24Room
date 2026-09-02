from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.7')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
android_test_dir = root / 'app/src/androidTest/java/com/rafat/munasabati/compat'

# Version bump.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*86\b', 'versionCode = 87', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.6"', 'versionName = "5.2.7"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Replace the calendar bridge with the instance-accurate/history-safe implementation.
sync_file.write_text((assets / 'CalendarSync.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Install unified main-calendar projection and Android regression tests.
compat_dir.mkdir(parents=True, exist_ok=True)
(compat_dir / 'UnifiedCalendarProjection.kt').write_text((assets / 'UnifiedCalendarProjection.kt').read_text(encoding='utf-8'), encoding='utf-8')
android_test_dir.mkdir(parents=True, exist_ok=True)
(android_test_dir / 'UnifiedCalendarProjectionTest.kt').write_text((assets / 'UnifiedCalendarProjectionTest.kt').read_text(encoding='utf-8'), encoding='utf-8')
(android_test_dir / 'CalendarSyncRegressionTest.kt').write_text((assets / 'CalendarSyncRegressionTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Main UI: protect true local history and merge Ahl al-Bayt occasions into all calendar modes.
s = ui.read_text(encoding='utf-8')
if 'import com.rafat.munasabati.compat.UnifiedCalendarProjection\n' not in s:
    anchor = 'import com.rafat.munasabati.compat.CalendarSyncManager\n'
    if anchor not in s:
        raise SystemExit('CalendarSyncManager import anchor missing')
    s = s.replace(anchor, anchor + 'import com.rafat.munasabati.compat.UnifiedCalendarProjection\n', 1)

old = '    val previousAll = active.filter { !v522IsImportedCalendarEvent(it) && it.startEpochMillis < System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
new = '    val previousAll = active.filter { !v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis() }.sortedByDescending { it.startEpochMillis }'
if old not in s:
    raise SystemExit('previous events filter anchor missing')
s = s.replace(old, new, 1)

old = '''        else items(previousEvents, key = { "previous-${it.id}" }) { event ->
            V52EventCard(event, english, onRemoveFromApp = {
                runCatching { ReminderScheduler(context).cancel(event.id) }
                v52Repo(context).deleteEvent(event.id)
                refreshKey++
            })
        }'''
new = '''        else items(previousEvents, key = { "previous-${it.id}" }) { event ->
            V52EventCard(event, english)
        }'''
if old not in s:
    raise SystemExit('previous event card anchor missing')
s = s.replace(old, new, 1)

old = '    fun eventsOn(date: LocalDate): List<EventModel> = allEvents.filter { Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'
new = '    fun eventsOn(date: LocalDate): List<EventModel> = UnifiedCalendarProjection.eventsForDate(context, date, allEvents)'
if old not in s:
    raise SystemExit('calendar eventsOn anchor missing')
s = s.replace(old, new, 1)

old = '                val monthEvents = allEvents.filter { val d = Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(); YearMonth.from(d) == month && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'
new = '                val monthEvents = UnifiedCalendarProjection.eventsForMonth(context, month, allEvents)'
if old not in s:
    raise SystemExit('month events anchor missing')
s = s.replace(old, new, 1)

old = '                if (monthEvents.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا الشهر", "No events this month")) } else items(monthEvents, key = { "cal-list-${it.id}" }) { V52EventCard(it, english) }'
new = '                if (monthEvents.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا الشهر", "No events this month")) } else items(monthEvents, key = { "cal-list-${it.id}" }) { V527CalendarEventCard(it, english) }'
if old not in s:
    raise SystemExit('month list card anchor missing')
s = s.replace(old, new, 1)

old = '            if (selected.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا اليوم", "No events on this day")) } else items(selected, key = { "selected-${it.id}" }) { V52EventCard(it, english) }'
new = '            if (selected.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا اليوم", "No events on this day")) } else items(selected, key = { "selected-${it.id}" }) { V527CalendarEventCard(it, english) }'
if old not in s:
    raise SystemExit('selected day card anchor missing')
s = s.replace(old, new, 1)

anchor = '@Composable\nprivate fun V52MonthCalendar'
helper = '''@Composable
private fun V527CalendarEventCard(event: EventModel, english: Boolean) {
    if (!UnifiedCalendarProjection.isAhlBaytEvent(event)) {
        V52EventCard(event, english)
        return
    }
    val visual = v52CategoryVisual(EventCategory.RELIGIOUS)
    val date = Instant.ofEpochMilli(event.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = visual.soft)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(visual.color), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Star, null, tint = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(v52HijriLabel(date, english), fontSize = 12.sp, color = visual.color, fontWeight = FontWeight.SemiBold)
                if (event.notes.isNotBlank()) Text(event.notes, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(v52Tr(english, "مناسبات أهل البيت عليهم السلام", "Ahl al-Bayt occasions"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun V52MonthCalendar'''
if anchor not in s:
    raise SystemExit('month calendar helper anchor missing')
s = s.replace(anchor, helper, 1)

ui.write_text(s, encoding='utf-8')
print('Munasabati v5.2.7 calendar accuracy/history/Ahl al-Bayt fixes applied')
