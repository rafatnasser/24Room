@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package com.rafat.munasabati.ui

import android.content.Context
import android.icu.util.Calendar as IcuCalendar
import android.icu.util.IslamicCalendar
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.rafat.munasabati.MunasabatiApp
import com.rafat.munasabati.compat.AppPreferences
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import com.rafat.munasabati.model.EventStatus
import com.rafat.munasabati.reminder.ReminderScheduler
import com.rafat.munasabati.smart.SmartSuggestionEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun v52Repo(context: Context) = (context.applicationContext as MunasabatiApp).repository
private fun v52English(context: Context) = AppPreferences(context).language() == "en"
private fun v52Tr(english: Boolean, ar: String, en: String) = if (english) en else ar

private data class V52CategoryVisual(val color: Color, val soft: Color, val icon: ImageVector, val ar: String, val en: String)

private fun v52CategoryVisual(category: EventCategory): V52CategoryVisual = when (category) {
    EventCategory.PERSONAL -> V52CategoryVisual(Color(0xFF147D70), Color(0xFFE4F5F1), Icons.Default.Person, "شخصية", "Personal")
    EventCategory.FAMILY -> V52CategoryVisual(Color(0xFFE05E4B), Color(0xFFFFECE8), Icons.Default.Favorite, "عائلية", "Family")
    EventCategory.WORK -> V52CategoryVisual(Color(0xFF3569B8), Color(0xFFEAF0FB), Icons.Default.Work, "عمل", "Work")
    EventCategory.RELIGIOUS -> V52CategoryVisual(Color(0xFF9A6A2E), Color(0xFFF8F0E4), Icons.Default.Star, "دينية", "Religious")
    EventCategory.TRAVEL -> V52CategoryVisual(Color(0xFF6654A5), Color(0xFFEFECFA), Icons.Default.Flight, "سفر", "Travel")
    EventCategory.OTHER -> V52CategoryVisual(Color(0xFF5F6875), Color(0xFFF0F2F4), Icons.Default.Event, "أخرى", "Other")
}

private fun v52DateTime(ms: Long, english: Boolean): String {
    val locale = if (english) Locale.ENGLISH else Locale("ar")
    return Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern(if (english) "EEE, d MMM • h:mm a" else "EEEE، d MMM • HH:mm", locale))
}

private fun v52HijriLabel(date: LocalDate, english: Boolean): String {
    val zone = ZoneId.systemDefault()
    val cal = IslamicCalendar().apply { timeInMillis = date.atStartOfDay(zone).toInstant().toEpochMilli() }
    val arMonths = arrayOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    val enMonths = arrayOf("Muharram", "Safar", "Rabi I", "Rabi II", "Jumada I", "Jumada II", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhu al-Qidah", "Dhu al-Hijjah")
    val d = cal.get(IcuCalendar.DAY_OF_MONTH)
    val m = cal.get(IcuCalendar.MONTH).coerceIn(0, 11)
    val y = cal.get(IcuCalendar.YEAR)
    return if (english) "$d ${enMonths[m]} $y AH" else "$d ${arMonths[m]} $y هـ"
}

private fun v52Greeting(english: Boolean): String = when {
    LocalTime.now().hour < 12 -> v52Tr(english, "صباح الخير", "Good morning")
    LocalTime.now().hour < 18 -> v52Tr(english, "مساء الخير", "Good afternoon")
    else -> v52Tr(english, "مساء الخير", "Good evening")
}

@Composable
fun V52DashboardScreen(nav: NavHostController) {
    val context = LocalContext.current
    val english = v52English(context)
    var allEvents by remember { mutableStateOf(emptyList<EventModel>()) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var suggestionDone by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refreshKey) { allEvents = withContext(Dispatchers.IO) { v52Repo(context).allEvents() } }

    val now = ZonedDateTime.now()
    val today = now.toLocalDate()
    val zone = now.zone
    val todayStart = today.atStartOfDay(zone).toInstant().toEpochMilli()
    val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
    val dayAfterStart = today.plusDays(2).atStartOfDay(zone).toInstant().toEpochMilli()
    val weekEnd = today.plusDays(7).atTime(LocalTime.MAX).atZone(zone).toInstant().toEpochMilli()
    val active = allEvents.filter { it.status != EventStatus.CANCELLED }
    val todayEvents = active.filter { it.startEpochMillis in todayStart until tomorrowStart }.sortedBy { it.startEpochMillis }
    val nextEvent = active.filter { it.startEpochMillis >= System.currentTimeMillis() }.minByOrNull { it.startEpochMillis }
    val tomorrowEvents = active.filter { it.startEpochMillis in tomorrowStart until dayAfterStart }.sortedBy { it.startEpochMillis }
    val importantWeek = active.filter { it.startEpochMillis in System.currentTimeMillis()..weekEnd && it.importance >= 2 }.minByOrNull { it.startEpochMillis }
    val suggestions = allEvents.mapNotNull { SmartSuggestionEngine.suggestionFor(it) }.sortedBy { it.daysUntil }.take(3)

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { V52TodayHero(english, today, todayEvents.size, { nav.navigate("search") }, { nav.navigate("add/${today}") }) }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V52MetricCard(Modifier.weight(1f), Icons.Default.Today, todayEvents.size.toString(), v52Tr(english, "مناسبات اليوم", "Today"))
                V52MetricCard(Modifier.weight(1f), Icons.Default.Schedule, nextEvent?.let { v52RelativeTime(it.startEpochMillis, english) } ?: "—", v52Tr(english, "الأقرب", "Next"))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                V52MetricCard(Modifier.weight(1f), Icons.Default.EventAvailable, tomorrowEvents.size.toString(), v52Tr(english, "غدًا", "Tomorrow"))
                V52MetricCard(Modifier.weight(1f), Icons.Default.NotificationImportant, importantWeek?.let { v52RelativeDay(it.startEpochMillis, english) } ?: "—", v52Tr(english, "مهم هذا الأسبوع", "Important this week"))
            }
        }
        if (suggestions.isNotEmpty()) {
            item { V52SectionTitle(Icons.Default.AutoAwesome, v52Tr(english, "اقتراحات ذكية", "Smart suggestions")) }
            items(suggestions, key = { "suggest-${it.event.id}" }) { suggestion ->
                V52SmartSuggestionCard(suggestion, english, suggestionDone == suggestion.event.id) {
                    val old = suggestion.event
                    val duration = (old.endEpochMillis - old.startEpochMillis).coerceAtLeast(60 * 60 * 1000L)
                    val updated = old.copy(startEpochMillis = suggestion.nextOccurrenceMillis, endEpochMillis = suggestion.nextOccurrenceMillis + duration, reminderMinutes = SmartSuggestionEngine.SEVEN_DAYS_MINUTES, remindersCsv = SmartSuggestionEngine.mergeSevenDayReminder(old.remindersCsv), updatedAt = System.currentTimeMillis())
                    runCatching { ReminderScheduler(context).cancel(old.id) }
                    v52Repo(context).upsertEvent(updated)
                    runCatching { ReminderScheduler(context).schedule(updated) }
                    suggestionDone = old.id
                    refreshKey++
                }
            }
        }
        item { V52SectionTitle(Icons.Default.Timeline, v52Tr(english, "الجدول الزمني لليوم", "Today's timeline"), todayEvents.size) }
        if (todayEvents.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات اليوم. يوم هادئ ✨", "No events today. Enjoy the open space ✨")) }
        else items(todayEvents, key = { "timeline-${it.id}" }) { V52TimelineEvent(it, english) }
        item { V52SectionTitle(Icons.Default.Upcoming, v52Tr(english, "القادم", "Coming up")) }
        val upcoming = active.filter { it.startEpochMillis >= tomorrowStart }.sortedBy { it.startEpochMillis }.take(5)
        if (upcoming.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات قادمة", "No upcoming events")) }
        else items(upcoming, key = { "up-${it.id}" }) { V52EventCard(it, english) }
    }
}

@Composable
private fun V52TodayHero(english: Boolean, today: LocalDate, todayCount: Int, onSearch: () -> Unit, onAdd: () -> Unit) {
    val locale = if (english) Locale.ENGLISH else Locale("ar")
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF125F55))) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(v52Greeting(english), color = Color.White.copy(alpha = .82f), fontSize = 15.sp)
                    Text(v52Tr(english, "يومك في مناسبـاتي", "Your day in Munasabati"), color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSearch) { Icon(Icons.Default.Search, v52Tr(english, "بحث", "Search"), tint = Color.White) }
            }
            Text(today.format(DateTimeFormatter.ofPattern(if (english) "EEEE, d MMMM yyyy" else "EEEE، d MMMM yyyy", locale)), color = Color.White)
            Text(v52HijriLabel(today, english), color = Color(0xFFD5F2E9), fontWeight = FontWeight.SemiBold)
            HorizontalDivider(color = Color.White.copy(alpha = .16f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.EventAvailable, null, tint = Color(0xFFFFD466)); Spacer(Modifier.width(8.dp))
                Text(if (english) "$todayCount event${if (todayCount == 1) "" else "s"} today" else "لديك $todayCount مناسبة اليوم", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = onAdd, colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.White.copy(alpha = .16f), contentColor = Color.White)) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(v52Tr(english, "إضافة", "Add")) }
            }
        }
    }
}

@Composable
private fun V52MetricCard(modifier: Modifier, icon: ImageVector, value: String, label: String) {
    Card(modifier = modifier.animateContentSize(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Icon(icon, null, tint = Color(0xFF147D70), modifier = Modifier.size(21.dp)); Text(value, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 1); Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun v52RelativeTime(ms: Long, english: Boolean): String {
    val minutes = ((ms - System.currentTimeMillis()).coerceAtLeast(0L) / 60_000L)
    return when { minutes < 60 -> if (english) "${minutes}m" else "${minutes} د"; minutes < 1440 -> if (english) "${minutes / 60}h" else "${minutes / 60} س"; else -> if (english) "${minutes / 1440}d" else "${minutes / 1440} ي" }
}

private fun v52RelativeDay(ms: Long, english: Boolean): String {
    val date = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
    val d = Duration.between(LocalDate.now().atStartOfDay(), date.atStartOfDay()).toDays()
    return when (d) { 0L -> v52Tr(english, "اليوم", "Today"); 1L -> v52Tr(english, "غدًا", "Tomorrow"); else -> if (english) "In $d days" else "بعد $d أيام" }
}

@Composable
private fun V52SectionTitle(icon: ImageVector, title: String, count: Int? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Color(0xFF147D70), modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); count?.let { Badge { Text(it.toString()) } } }
}

@Composable
private fun V52EmptyCard(text: String) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) { Text(text, Modifier.fillMaxWidth().padding(18.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun V52SmartSuggestionCard(suggestion: SmartSuggestionEngine.AnnualSuggestion, english: Boolean, done: Boolean, onAccept: () -> Unit) {
    AnimatedContent(targetState = done, label = "smart-suggestion") { completed ->
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (completed) Color(0xFFE4F5F1) else Color(0xFFFFF5D9)), shape = RoundedCornerShape(22.dp)) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(if (completed) Color(0xFF147D70) else Color(0xFFF2B84B)), contentAlignment = Alignment.Center) { Icon(if (completed) Icons.Default.Check else Icons.Default.AutoAwesome, null, tint = Color.White) }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(if (completed) v52Tr(english, "تم إنشاء التذكير", "Reminder created") else suggestion.event.title, fontWeight = FontWeight.Bold)
                    if (!completed) Text(if (english) "Annual event in ${suggestion.daysUntil} day${if (suggestion.daysUntil == 1L) "" else "s"}. Add a reminder 7 days before?" else "مناسبة سنوية بعد ${suggestion.daysUntil} أيام. هل تريد إنشاء تذكير قبل 7 أيام؟", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!completed) TextButton(onClick = onAccept) { Text(v52Tr(english, "إنشاء", "Create")) }
            }
        }
    }
}

@Composable
private fun V52TimelineEvent(event: EventModel, english: Boolean) {
    val time = Instant.ofEpochMilli(event.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(DateTimeFormatter.ofPattern(if (english) "h:mm a" else "HH:mm", if (english) Locale.ENGLISH else Locale("ar")))
    val visual = v52CategoryVisual(event.category)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(time, modifier = Modifier.width(66.dp).padding(top = 17.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = visual.color)
        Box(Modifier.width(24.dp).height(118.dp)) { Box(Modifier.width(2.dp).fillMaxHeight().align(Alignment.Center).background(visual.color.copy(alpha = .22f))); Box(Modifier.size(12.dp).align(Alignment.TopCenter).offset(y = 20.dp).background(visual.color, CircleShape)) }
        Spacer(Modifier.width(4.dp)); V52EventCard(event, english, Modifier.weight(1f))
    }
}

@Composable
private fun V52EventCard(event: EventModel, english: Boolean, modifier: Modifier = Modifier) {
    var expanded by remember(event.id) { mutableStateOf(false) }
    val visual = v52CategoryVisual(event.category)
    Card(modifier = modifier.fillMaxWidth().animateContentSize(tween(260)).clickable { expanded = !expanded }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = visual.soft)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(36.dp).background(visual.color, CircleShape), contentAlignment = Alignment.Center) { Icon(visual.icon, null, tint = Color.White, modifier = Modifier.size(19.dp)) }
                Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(event.title, fontWeight = FontWeight.Bold, fontSize = 16.sp); Text(v52Tr(english, visual.ar, visual.en), fontSize = 11.sp, color = visual.color, fontWeight = FontWeight.SemiBold) }
                if (event.importance >= 2) Icon(Icons.Default.Star, null, tint = Color(0xFFF2B84B), modifier = Modifier.size(18.dp)); Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(v52DateTime(event.startEpochMillis, english), fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            AnimatedVisibility(visible = expanded, enter = fadeIn(tween(180)) + expandVertically(tween(240)), exit = fadeOut(tween(140)) + shrinkVertically(tween(200))) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    HorizontalDivider(color = visual.color.copy(alpha = .18f))
                    if (event.locationName.isNotBlank()) Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, tint = visual.color, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(6.dp)); Text(event.locationName, fontSize = 13.sp) }
                    if (event.notes.isNotBlank()) Text(event.notes, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AssistChip(onClick = {}, label = { Text(if (event.recurrence.equals("yearly", true)) v52Tr(english, "سنوية", "Yearly") else v52Tr(english, "مرة واحدة", "One-time")) }, leadingIcon = { Icon(Icons.Default.Repeat, null, modifier = Modifier.size(16.dp)) })
                        AssistChip(onClick = {}, label = { Text(v52Tr(english, "قبل ${event.reminderMinutes} د", "${event.reminderMinutes}m before")) }, leadingIcon = { Icon(Icons.Default.Notifications, null, modifier = Modifier.size(16.dp)) })
                    }
                }
            }
        }
    }
}

@Composable
fun V52SearchScreen(nav: NavHostController) {
    val context = LocalContext.current
    val english = v52English(context)
    var query by remember { mutableStateOf("") }
    var events by remember { mutableStateOf(emptyList<EventModel>()) }
    var people by remember { mutableStateOf(emptyMap<String, String>()) }
    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { val r = v52Repo(context); events = r.allEvents(); people = r.people().associate { it.id to it.name } } }
    val normalized = query.trim().lowercase()
    val results = remember(events, people, normalized) {
        if (normalized.isBlank()) emptyList() else events.filter { e ->
            val date = Instant.ofEpochMilli(e.startEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"))
            listOf(e.title, e.locationName, e.notes, e.category.labelAr, e.category.name, people[e.personId].orEmpty(), date).joinToString(" ").lowercase().contains(normalized)
        }.sortedBy { it.startEpochMillis }.take(100)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(v52Tr(english, "البحث الشامل", "Global search"), fontSize = 28.sp, fontWeight = FontWeight.Bold); Spacer(Modifier.height(6.dp)); Text(v52Tr(english, "ابحث بالاسم، الشخص، الموقع، الملاحظات، التاريخ أو التصنيف.", "Search by event, person, location, notes, date, or category."), color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, null) } }, label = { Text(v52Tr(english, "اكتب للبحث", "Type to search")) }) }
        if (query.isNotBlank()) item { Text(if (english) "${results.size} result${if (results.size == 1) "" else "s"}" else "${results.size} نتيجة", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (query.isBlank()) item { V52EmptyCard(v52Tr(english, "ابدأ بكتابة كلمة للبحث في جميع بيانات مناسباتك.", "Start typing to search all your event data.")) }
        else if (results.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد نتائج مطابقة", "No matching results")) }
        else items(results, key = { "search-${query}-${it.id}" }) { event ->
            var visible by remember(query, event.id) { mutableStateOf(false) }
            LaunchedEffect(query, event.id) { visible = true }
            AnimatedVisibility(visible = visible, enter = fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 4 }, exit = fadeOut(tween(120))) { V52EventCard(event, english) }
        }
    }
}

private enum class V52CalendarMode { MONTH, WEEK, LIST }

@Composable
fun V52CalendarScreen(nav: NavHostController) {
    val context = LocalContext.current
    val english = v52English(context)
    var allEvents by remember { mutableStateOf(emptyList<EventModel>()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var mode by remember { mutableStateOf(V52CalendarMode.MONTH) }
    LaunchedEffect(Unit) { allEvents = withContext(Dispatchers.IO) { v52Repo(context).allEvents() } }
    fun eventsOn(date: LocalDate): List<EventModel> = allEvents.filter { Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate() == date && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .55f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(v52Tr(english, "التقويم", "Calendar"), fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        V52CalendarMode.entries.forEachIndexed { index, value ->
                            val label = when (value) { V52CalendarMode.MONTH -> v52Tr(english, "شهر", "Month"); V52CalendarMode.WEEK -> v52Tr(english, "أسبوع", "Week"); V52CalendarMode.LIST -> v52Tr(english, "قائمة", "List") }
                            SegmentedButton(selected = mode == value, onClick = { mode = value }, shape = SegmentedButtonDefaults.itemShape(index, 3)) { Text(label) }
                        }
                    }
                }
            }
        }
        when (mode) {
            V52CalendarMode.MONTH -> item { V52MonthCalendar(month, selectedDate, english, { eventsOn(it) }, { month = it }, { month = YearMonth.now(); selectedDate = LocalDate.now() }) { date -> val dayEvents = eventsOn(date); selectedDate = date; if (dayEvents.isEmpty()) nav.navigate("add/$date") } }
            V52CalendarMode.WEEK -> item { V52WeekCalendar(selectedDate, english, { eventsOn(it) }) { selectedDate = it } }
            V52CalendarMode.LIST -> {
                val monthEvents = allEvents.filter { val d = Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate(); YearMonth.from(d) == month && it.status != EventStatus.CANCELLED }.sortedBy { it.startEpochMillis }
                item { V52SectionTitle(Icons.Default.FormatListBulleted, v52Tr(english, "مناسبات الشهر", "Month events"), monthEvents.size) }
                if (monthEvents.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا الشهر", "No events this month")) } else items(monthEvents, key = { "cal-list-${it.id}" }) { V52EventCard(it, english) }
            }
        }
        if (mode != V52CalendarMode.LIST) {
            val selected = eventsOn(selectedDate)
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(v52Tr(english, "اليوم المختار", "Selected day"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(selectedDate.format(DateTimeFormatter.ofPattern(if (english) "EEEE, d MMMM" else "EEEE، d MMMM", if (english) Locale.ENGLISH else Locale("ar"))), fontSize = 19.sp, fontWeight = FontWeight.Bold) }; FilledTonalButton(onClick = { nav.navigate("add/$selectedDate") }) { Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text(v52Tr(english, "إضافة", "Add")) } } }
            if (selected.isEmpty()) item { V52EmptyCard(v52Tr(english, "لا توجد مناسبات في هذا اليوم", "No events on this day")) } else items(selected, key = { "selected-${it.id}" }) { V52EventCard(it, english) }
        }
    }
}

@Composable
private fun V52MonthCalendar(month: YearMonth, selectedDate: LocalDate, english: Boolean, eventProvider: (LocalDate) -> List<EventModel>, onMonth: (YearMonth) -> Unit, onToday: () -> Unit, onDay: (LocalDate) -> Unit) {
    val locale = if (english) Locale.ENGLISH else Locale("ar")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onMonth(month.minusMonths(1)) }) { Icon(if (english) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, null) }
                AnimatedContent(targetState = month, modifier = Modifier.weight(1f), transitionSpec = { if (targetState.isAfter(initialState)) (slideInHorizontally(tween(260)) { it / 2 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(tween(240)) { -it / 2 } + fadeOut(tween(180))) else (slideInHorizontally(tween(260)) { -it / 2 } + fadeIn(tween(220))) togetherWith (slideOutHorizontally(tween(240)) { it / 2 } + fadeOut(tween(180))) }, label = "month-title") { shown -> Text(shown.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                IconButton(onClick = { onMonth(month.plusMonths(1)) }) { Icon(if (english) Icons.Default.ChevronRight else Icons.Default.ChevronLeft, null) }
                TextButton(onClick = onToday) { Text(v52Tr(english, "اليوم", "Today")) }
            }
            val weekdays = if (english) listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat") else listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
            Row(Modifier.fillMaxWidth()) { weekdays.forEachIndexed { i, d -> Text(d, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 11.sp, color = if (i >= 5) Color(0xFF9A6A2E) else MaterialTheme.colorScheme.onSurfaceVariant) } }
            AnimatedContent(targetState = month, transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) }, label = "month-grid") { shown ->
                val first = shown.atDay(1); val offset = first.dayOfWeek.value % 7; val cells = offset + shown.lengthOfMonth(); val rows = (cells + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) { repeat(rows) { row -> Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) { repeat(7) { col -> val index = row * 7 + col; val day = index - offset + 1; if (day !in 1..shown.lengthOfMonth()) Spacer(Modifier.weight(1f).aspectRatio(1f)) else { val date = shown.atDay(day); V52DayCell(date, selectedDate, eventProvider(date), Modifier.weight(1f)) { onDay(date) } } } } } }
            }
        }
    }
}

@Composable
private fun V52DayCell(date: LocalDate, selectedDate: LocalDate, events: List<EventModel>, modifier: Modifier, onClick: () -> Unit) {
    val isWeekend = date.dayOfWeek == DayOfWeek.FRIDAY || date.dayOfWeek == DayOfWeek.SATURDAY
    val selected = date == selectedDate
    val today = date == LocalDate.now()
    val base = when { selected -> Color(0xFF147D70); isWeekend -> Color(0xFFFFF1E6); else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .42f) }
    Column(modifier.aspectRatio(.9f).clip(RoundedCornerShape(12.dp)).background(base).clickable(onClick = onClick).padding(5.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
        Text(date.dayOfMonth.toString(), fontWeight = if (today || selected) FontWeight.Bold else FontWeight.Normal, color = if (selected) Color.White else if (isWeekend) Color(0xFF9A6A2E) else MaterialTheme.colorScheme.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) { events.take(3).forEach { Box(Modifier.size(5.dp).background(if (selected) Color.White else v52CategoryVisual(it.category).color, CircleShape)) }; if (events.isEmpty()) Text("+", fontSize = 10.sp, color = if (selected) Color.White.copy(alpha = .75f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .45f)) }
    }
}

@Composable
private fun V52WeekCalendar(selectedDate: LocalDate, english: Boolean, eventProvider: (LocalDate) -> List<EventModel>, onDay: (LocalDate) -> Unit) {
    val start = selectedDate.minusDays((selectedDate.dayOfWeek.value % 7).toLong())
    val locale = if (english) Locale.ENGLISH else Locale("ar")
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(v52Tr(english, "الأسبوع", "Week"), fontWeight = FontWeight.Bold, fontSize = 19.sp)
            repeat(7) { i ->
                val date = start.plusDays(i.toLong()); val events = eventProvider(date); val selected = date == selectedDate
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (selected) Color(0xFFE4F5F1) else Color.Transparent).clickable { onDay(date) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(date.format(DateTimeFormatter.ofPattern("EEE", locale)), modifier = Modifier.width(54.dp), fontWeight = FontWeight.Bold); Text(date.dayOfMonth.toString(), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF147D70)); Spacer(Modifier.width(12.dp)); Text(if (events.isEmpty()) v52Tr(english, "فارغ", "Open") else events.joinToString(" • ") { it.title }, modifier = Modifier.weight(1f), maxLines = 1, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); if (events.isNotEmpty()) Badge { Text(events.size.toString()) }
                }
            }
        }
    }
}
