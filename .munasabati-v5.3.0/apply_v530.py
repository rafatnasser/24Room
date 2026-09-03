from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.3.0')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
android_test_dir = root / 'app/src/androidTest/java/com/rafat/munasabati/compat'

# Major version bump: preserve package/database identity; only version code/name change.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*86\b', 'versionCode = 90', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.6"', 'versionName = "5.3.0"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

# Replace the sync engine rather than layering another heuristic patch.
sync_file.write_text((assets / 'CalendarSync.kt').read_text(encoding='utf-8'), encoding='utf-8')

# Ahl al-Bayt occasions become a first-class display source in the app calendar.
compat_dir.mkdir(parents=True, exist_ok=True)
(compat_dir / 'AhlBaytCalendarEventBridge.kt').write_text((assets / 'AhlBaytCalendarEventBridge.kt').read_text(encoding='utf-8'), encoding='utf-8')
android_test_dir.mkdir(parents=True, exist_ok=True)
(android_test_dir / 'V530CalendarIntegrationTest.kt').write_text((assets / 'V530CalendarIntegrationTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

s = ui.read_text(encoding='utf-8')
if 'import com.rafat.munasabati.compat.AhlBaytCalendarEventBridge\n' not in s:
    anchor = 'import com.rafat.munasabati.compat.CalendarSyncManager\n'
    if anchor not in s:
        raise SystemExit('CalendarSyncManager import anchor missing')
    s = s.replace(anchor, anchor + 'import com.rafat.munasabati.compat.AhlBaytCalendarEventBridge\n', 1)

old = '''    fun eventsOn(date: LocalDate): List<EventModel> = allEvents.filter { Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'''
new = '''    fun eventsOn(date: LocalDate): List<EventModel> = (
        allEvents.filter { Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date && it.status != EventStatus.CANCELLED } +
            AhlBaytCalendarEventBridge.eventsForDate(context, date)
    ).sortedBy { it.startEpochMillis }'''
if old not in s:
    raise SystemExit('calendar eventsOn anchor missing')
s = s.replace(old, new, 1)

old = '''                val monthEvents = allEvents.filter { val d = Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(); YearMonth.from(d) == month && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'''
new = '''                val monthEvents = (
                    allEvents.filter { val d = Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(); YearMonth.from(d) == month && it.status != EventStatus.CANCELLED } +
                        AhlBaytCalendarEventBridge.eventsForMonth(context, month)
                ).sortedBy { it.startEpochMillis }'''
if old not in s:
    raise SystemExit('calendar month list anchor missing')
s = s.replace(old, new, 1)

ui.write_text(s, encoding='utf-8')
print('Munasabati v5.3.0 safe calendar sources architecture applied')
