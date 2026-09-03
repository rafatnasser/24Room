from pathlib import Path
import re, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.3.0')
build = root / 'app/build.gradle.kts'
ui = root / 'app/src/main/java/com/rafat/munasabati/ui/V52Experience.kt'
sync_file = root / 'app/src/main/java/com/rafat/munasabati/compat/CalendarSync.kt'
legacy = root / 'app/src/main/java/com/rafat/munasabati/ui/LegacyScreens.kt'
compat_dir = root / 'app/src/main/java/com/rafat/munasabati/compat'
android_test_dir = root / 'app/src/androidTest/java/com/rafat/munasabati/compat'

# v5.3.0 architecture correction. Preserve package/database/application identity.
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*86\b', 'versionCode = 90', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.2\.6"', 'versionName = "5.3.0"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

compat_dir.mkdir(parents=True, exist_ok=True)
android_test_dir.mkdir(parents=True, exist_ok=True)

# Replace the accumulated v5.2.x sync heuristics with one provider-backed live engine.
sync_file.write_text((assets / 'CalendarSync.kt').read_text(encoding='utf-8'), encoding='utf-8')
(compat_dir / 'UnifiedCalendarSource.kt').write_text((assets / 'UnifiedCalendarSource.kt').read_text(encoding='utf-8'), encoding='utf-8')
(compat_dir / 'PreviousEventRecovery.kt').write_text((assets / 'PreviousEventRecovery.kt').read_text(encoding='utf-8'), encoding='utf-8')
(android_test_dir / 'CalendarSyncSmokeTest.kt').write_text((assets / 'CalendarSyncSmokeTest.kt').read_text(encoding='utf-8'), encoding='utf-8')

# ---------------------------------------------------------------------------
# Dashboard/search/calendar: recover local history once, purge only proven old
# imported copies, and merge current external provider instances in memory.
# ---------------------------------------------------------------------------
s = ui.read_text(encoding='utf-8')

old = '''            val repo = v52Repo(context)
            runCatching { CalendarSyncManager(context, repo).cleanupExpiredCalendarEvents() }
            allEvents = repo.allEvents()
            calendarSources = runCatching {'''
new = '''            val repo = v52Repo(context)
            runCatching { com.rafat.munasabati.compat.PreviousEventRecovery.recoverMissingLegacyEventsOnce(context, repo) }
            val calendarManager = CalendarSyncManager(context, repo)
            runCatching { calendarManager.prepareLiveMode() }
            val liveExternal = runCatching { calendarManager.liveConnectedEvents() }.getOrDefault(emptyList())
            allEvents = repo.allEvents() + liveExternal
            calendarSources = runCatching {'''
if old not in s:
    raise SystemExit('dashboard v5.3 load anchor missing')
s = s.replace(old, new, 1)

old = '''    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { CalendarSyncManager(context, r).cleanupExpiredCalendarEvents() }
        events = r.allEvents().filter { !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis()) }
        people = r.people().associate { it.id to it.name }
    } }'''
new = '''    LaunchedEffect(Unit) { withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { com.rafat.munasabati.compat.PreviousEventRecovery.recoverMissingLegacyEventsOnce(context, r) }
        val calendarManager = CalendarSyncManager(context, r)
        runCatching { calendarManager.prepareLiveMode() }
        events = r.allEvents() + runCatching { calendarManager.liveConnectedEvents() }.getOrDefault(emptyList())
        people = r.people().associate { it.id to it.name }
    } }'''
if old not in s:
    raise SystemExit('search v5.3 load anchor missing')
s = s.replace(old, new, 1)

old = '''    LaunchedEffect(Unit) { allEvents = withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { CalendarSyncManager(context, r).cleanupExpiredCalendarEvents() }
        r.allEvents().filter { !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis()) }
    } }'''
new = '''    LaunchedEffect(Unit) { allEvents = withContext(Dispatchers.IO) {
        val r = v52Repo(context)
        runCatching { com.rafat.munasabati.compat.PreviousEventRecovery.recoverMissingLegacyEventsOnce(context, r) }
        val calendarManager = CalendarSyncManager(context, r)
        runCatching { calendarManager.prepareLiveMode() }
        r.allEvents() + runCatching { calendarManager.liveConnectedEvents() }.getOrDefault(emptyList())
    } }'''
if old not in s:
    raise SystemExit('calendar v5.3 load anchor missing')
s = s.replace(old, new, 1)

# Ahl al-Bayt occasions are a non-persistent calendar source, visible regardless
# of the master notification switch. Per-category switches are still honored.
old = '''    fun eventsOn(date: LocalDate): List<EventModel> = allEvents.filter { Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'''
new = '''    fun eventsOn(date: LocalDate): List<EventModel> = (
        allEvents.filter {
            Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date &&
                it.status != EventStatus.CANCELLED &&
                !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis())
        } + com.rafat.munasabati.compat.UnifiedCalendarSource.ahlBaytEventsForDate(context, date)
    ).distinctBy { it.id }.sortedBy { it.startEpochMillis }'''
if old not in s:
    raise SystemExit('calendar eventsOn v5.3 anchor missing')
s = s.replace(old, new, 1)

old = '''                val monthEvents = allEvents.filter { val d = Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(); YearMonth.from(d) == month && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }'''
new = '''                val monthEvents = (1..month.lengthOfMonth())
                    .flatMap { day -> eventsOn(month.atDay(day)) }
                    .distinctBy { it.id }
                    .sortedBy { it.startEpochMillis }'''
if old not in s:
    raise SystemExit('calendar month list v5.3 anchor missing')
s = s.replace(old, new, 1)

# While the calendar screen remains open, remove a live external instance at its
# exact end time without waiting for a navigation/restart.
anchor = '''    fun eventsOn(date: LocalDate): List<EventModel> = (
        allEvents.filter {
            Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date &&
                it.status != EventStatus.CANCELLED &&
                !(v522IsImportedCalendarEvent(it) && it.endEpochMillis <= System.currentTimeMillis())
        } + com.rafat.munasabati.compat.UnifiedCalendarSource.ahlBaytEventsForDate(context, date)
    ).distinctBy { it.id }.sortedBy { it.startEpochMillis }
'''
expiry = anchor + '''
    LaunchedEffect(allEvents) {
        val nowMillis = System.currentTimeMillis()
        val nextExpiry = allEvents
            .filter { v522IsImportedCalendarEvent(it) && it.endEpochMillis > nowMillis }
            .minOfOrNull { it.endEpochMillis }
        if (nextExpiry != null) {
            delay((nextExpiry - nowMillis).coerceAtLeast(250L) + 250L)
            val expiredAt = System.currentTimeMillis()
            allEvents = allEvents.filterNot { v522IsImportedCalendarEvent(it) && it.endEpochMillis <= expiredAt }
        }
    }
'''
if anchor not in s:
    raise SystemExit('calendar live expiry anchor missing')
s = s.replace(anchor, expiry, 1)

# Existing v5.2.3 per-calendar Remove button used Room deletion. External rows are
# no longer Room rows, so hide that action instead of presenting a dead control.
old = '''                    source = v52CalendarSource(event, calendarSources, english),
                    onRemoveFromApp = {
                        runCatching { ReminderScheduler(context).cancel(event.id) }
                        v52Repo(context).deleteEvent(event.id)
                        refreshKey++
                    }
                )'''
new = '''                    source = v52CalendarSource(event, calendarSources, english)
                )'''
if old not in s:
    raise SystemExit('calendar card live-source action anchor missing')
s = s.replace(old, new, 1)

ui.write_text(s, encoding='utf-8')

# ---------------------------------------------------------------------------
# Sync screen: show connection state independent of persisted imported rows.
# ---------------------------------------------------------------------------
s = legacy.read_text(encoding='utf-8')
old = '''                    val linked = linkedCounts[c.id] ?: 0
                    Card(Modifier.fillMaxWidth()) {'''
new = '''                    val linked = linkedCounts[c.id] ?: 0
                    val connected = sync.isCalendarConnected(c.id)
                    Card(Modifier.fillMaxWidth()) {'''
if old not in s:
    raise SystemExit('sync connected-state anchor missing')
s = s.replace(old, new, 1)

old = '''                                    Text(if (linked > 0) tr("${c.account} • $linked مناسبة مرتبطة", "${c.account} • $linked linked event(s)") else c.account, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)'''
new = '''                                    Text(when {
                                        connected && linked > 0 -> tr("${c.account} • متصل • $linked مناسبة محلية مرتبطة", "${c.account} • Connected • $linked linked local event(s)")
                                        connected -> tr("${c.account} • متصل", "${c.account} • Connected")
                                        else -> c.account
                                    }, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)'''
if old not in s:
    raise SystemExit('sync source label anchor missing')
s = s.replace(old, new, 1)

old = '''status = tr("تمت مزامنة ${c.name}: ${result.importedOrUpdated} مستوردة/محدثة و${result.pushed} محلية", "Synced ${c.name}: ${result.importedOrUpdated} imported/updated and ${result.pushed} local")'''
new = '''status = tr("تم تحديث ${c.name}: ${result.importedOrUpdated} حدثًا ظاهرًا مباشرة من التقويم و${result.pushed} مناسبة محلية مرتبطة", "Refreshed ${c.name}: ${result.importedOrUpdated} live calendar event(s) and ${result.pushed} linked local event(s)")'''
if old not in s:
    raise SystemExit('sync status v5.3 anchor missing')
s = s.replace(old, new, 1)

old = '''                            }, enabled = linked > 0 && syncingCalendarId == null, modifier = Modifier.fillMaxWidth()) {'''
new = '''                            }, enabled = (connected || linked > 0) && syncingCalendarId == null, modifier = Modifier.fillMaxWidth()) {'''
if old not in s:
    raise SystemExit('disconnect enabled v5.3 anchor missing')
s = s.replace(old, new, 1)

legacy.write_text(s, encoding='utf-8')
print('Munasabati v5.3.0 live calendar, previous-history recovery and Ahl al-Bayt integration applied')
