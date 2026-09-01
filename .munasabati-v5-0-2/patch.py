from pathlib import Path
import re
root=Path('v5src')

p=root/'app/build.gradle.kts'
s=p.read_text()
s=s.replace('versionCode = 61','versionCode = 62').replace('versionName = "5.0.1"','versionName = "5.0.2"').replace('versionCode = 60','versionCode = 62').replace('versionName = "5.0.0"','versionName = "5.0.2"')
s=s.replace('isMinifyEnabled = true','isMinifyEnabled = false').replace('isShrinkResources = true','isShrinkResources = false')
p.write_text(s)

p=root/'app/src/main/java/com/rafat/munasabati/compat/AppPreferences.kt'
p.write_text(r'''package com.rafat.munasabati.compat

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

object PreferenceCoercion {
    fun bool(value: Any?, defaultValue: Boolean = false): Boolean = when (value) {
        is Boolean -> value
        is Number -> value.toInt() != 0
        is String -> when (value.trim().lowercase()) { "true", "1", "yes", "on" -> true; "false", "0", "no", "off" -> false; else -> defaultValue }
        else -> defaultValue
    }
    fun language(value: Any?): String = when (value?.toString()?.trim()?.lowercase()) {
        "en", "english" -> "en"
        else -> "ar"
    }
}

class AppPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("munasabati_settings", Context.MODE_PRIVATE)
    private fun any(key: String): Any? = runCatching { prefs.all[key] }.getOrNull()

    fun language(): String = PreferenceCoercion.language(any("language"))
    fun setLanguage(code: String) { prefs.edit().putString("language", if (code == "en") "en" else "ar").apply() }

    fun soundEnabled(): Boolean = PreferenceCoercion.bool(any("sound_enabled"), true)
    fun setSoundEnabled(enabled: Boolean) { prefs.edit().putBoolean("sound_enabled", enabled).apply() }

    fun hapticEnabled(): Boolean = PreferenceCoercion.bool(any("haptic_enabled"), true)
    fun setHapticEnabled(enabled: Boolean) { prefs.edit().putBoolean("haptic_enabled", enabled).apply() }

    fun hapticLevel(): Int = when (val value = any("haptic_level")) {
        is Number -> value.toInt().coerceIn(1, 3)
        is String -> value.toIntOrNull()?.coerceIn(1, 3) ?: 2
        else -> 2
    }
    fun setHapticLevel(level: Int) { prefs.edit().putInt("haptic_level", level.coerceIn(1, 3)).apply() }
}

object UiFeedback {
    private fun tone(context: Context, tone: Int, duration: Int) {
        if (!AppPreferences(context).soundEnabled()) return
        runCatching {
            val generator = ToneGenerator(AudioManager.STREAM_SYSTEM, 70)
            generator.startTone(tone, duration)
            Handler(Looper.getMainLooper()).postDelayed({ runCatching { generator.release() } }, duration.toLong() + 80L)
        }
    }

    private fun vibrate(context: Context, millis: Long) {
        val prefs = AppPreferences(context)
        if (!prefs.hapticEnabled()) return
        val vibrator = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(VibratorManager::class.java)?.defaultVibrator else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        val scaled = (millis * when (prefs.hapticLevel()) { 1 -> 0.6; 3 -> 1.5; else -> 1.0 }).toLong().coerceAtLeast(20L)
        runCatching {
            if (Build.VERSION.SDK_INT >= 26) vibrator?.vibrate(VibrationEffect.createOneShot(scaled, VibrationEffect.DEFAULT_AMPLITUDE))
            else @Suppress("DEPRECATION") vibrator?.vibrate(scaled)
        }
    }

    fun click(context: Context) { tone(context, ToneGenerator.TONE_PROP_BEEP, 70); vibrate(context, 30) }
    fun success(context: Context) { tone(context, ToneGenerator.TONE_PROP_ACK, 120); vibrate(context, 45) }
    fun delete(context: Context) { tone(context, ToneGenerator.TONE_PROP_NACK, 120); vibrate(context, 70) }
    fun preview(context: Context) { tone(context, ToneGenerator.TONE_PROP_ACK, 220); vibrate(context, 100) }
}
''')

p=root/'app/src/main/java/com/rafat/munasabati/ui/SettingsAboutScreen.kt'
p.write_text(r'''package com.rafat.munasabati.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafat.munasabati.BuildConfig
import com.rafat.munasabati.compat.AppPreferences
import com.rafat.munasabati.compat.UiFeedback

@Composable fun SettingsAboutScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var language by remember { mutableStateOf(prefs.language()) }
    var sound by remember { mutableStateOf(prefs.soundEnabled()) }
    var haptic by remember { mutableStateOf(prefs.hapticEnabled()) }
    var hapticLevel by remember { mutableIntStateOf(prefs.hapticLevel()) }
    val en = LocalAppLanguage.current == "en"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Language, null); Spacer(Modifier.width(8.dp)); Text(if(en) "Appearance & language" else "المظهر واللغة", fontSize=27.sp, fontWeight=FontWeight.Bold) }
        Text(if(en) "App language" else "لغة البرنامج", fontWeight=FontWeight.Bold)
        listOf("ar" to "العربية", "en" to "English").forEach { (code, label) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected=language==code, onClick={
                    if(language!=code){ language=code; prefs.setLanguage(code); UiFeedback.click(context); (context as? Activity)?.recreate() }
                })
                Text(label)
            }
        }
        HorizontalDivider()
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MusicNote, null); Spacer(Modifier.width(8.dp)); Text(if(en) "Sound, touch & motion" else "الصوت واللمس والحركة", fontSize=22.sp, fontWeight=FontWeight.Bold) }
        Row(verticalAlignment=Alignment.CenterVertically){Text(if(en) "Interface & reminder sounds" else "أصوات الواجهة والتنبيهات",Modifier.weight(1f));Switch(sound,{sound=it;prefs.setSoundEnabled(it);if(it)UiFeedback.preview(context)})}
        Row(verticalAlignment=Alignment.CenterVertically){Text(if(en) "Haptic feedback & vibration" else "اللمس والاهتزاز",Modifier.weight(1f));Switch(haptic,{haptic=it;prefs.setHapticEnabled(it);if(it)UiFeedback.preview(context)})}
        if(haptic){Text(if(en) "Haptic strength" else "قوة الاهتزاز",fontWeight=FontWeight.Bold);SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()){listOf(1,2,3).forEachIndexed{i,v->SegmentedButton(selected=hapticLevel==v,onClick={hapticLevel=v;prefs.setHapticLevel(v);UiFeedback.preview(context)},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(if(en) listOf("Light","Medium","Strong")[i] else listOf("خفيف","متوسط","قوي")[i])}}}}
        OutlinedButton(onClick={UiFeedback.preview(context)},modifier=Modifier.fillMaxWidth()){Text(if(en) "Preview sound & haptic" else "تشغيل تجربة قصيرة للصوت واللمس")}

        HorizontalDivider()
        Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Info,null);Spacer(Modifier.width(8.dp));Text(if(en) "About Munasabati" else "حول مناسبـاتي",fontSize=22.sp,fontWeight=FontWeight.Bold)}
            Text("رأفت ناصر الناصر  •  Rafat Nasser Alnasser",fontWeight=FontWeight.Bold)
            Text(if(en) "© 2026 Rafat Nasser Alnasser — All rights reserved." else "© 2026 رأفت ناصر الناصر — جميع حقوق البرنامج محفوظة.")
            Text("v${BuildConfig.VERSION_NAME}",style=MaterialTheme.typography.bodySmall)
        }}
    }
}
''')

p=root/'app/src/main/java/com/rafat/munasabati/ui/AppUi.kt'
s=p.read_text()
if 'import com.rafat.munasabati.compat.AppPreferences' not in s:
    s=s.replace('import com.rafat.munasabati.calendar.CalendarRules','import com.rafat.munasabati.calendar.CalendarRules\nimport com.rafat.munasabati.compat.AppPreferences\nimport com.rafat.munasabati.compat.UiFeedback')
s=s.replace('private val ar = Locale("ar")\nprivate fun repo(context: Context)', 'private val ar = Locale("ar")\nval LocalAppLanguage = staticCompositionLocalOf { "ar" }\n@Composable fun tr(arText:String,enText:String):String = if(LocalAppLanguage.current=="en") enText else arText\nprivate fun repo(context: Context)')
s=s.replace('CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {','val appLanguage=remember{AppPreferences(LocalContext.current).language()}\n    CompositionLocalProvider(LocalAppLanguage provides appLanguage, LocalLayoutDirection provides if(appLanguage=="en") LayoutDirection.Ltr else LayoutDirection.Rtl) {')
s=s.replace('Text("مناسباتي", fontWeight = FontWeight.Bold); Text("لا تتذكر الموعد فقط، تذكّر الشخص واللحظة.", fontSize = 11.sp)', 'Text(tr("مناسباتي","Munasabati"), fontWeight = FontWeight.Bold); Text(tr("لا تتذكر الموعد فقط، تذكّر الشخص واللحظة.","Remember the person and the moment, not only the date."), fontSize = 11.sp)')
s=s.replace('composable("diagnostics") { DiagnosticsScreen() }','composable("diagnostics") { DiagnosticsScreen() }\n            composable("settings") { SettingsAboutScreen() }')
s=re.sub(r'@Composable private fun BottomBar\(nav: NavHostController\) \{.*?\n\}', r'''@Composable private fun BottomBar(nav: NavHostController) {
    val labels=listOf(
        "center" to Pair(Icons.Default.Home,tr("المركز","Center")),
        "calendar" to Pair(Icons.Default.DateRange,tr("التقويم","Calendar")),
        "people" to Pair(Icons.Default.People,tr("الأشخاص","People")),
        "memories" to Pair(Icons.Default.Favorite,tr("الذكريات","Memories")),
        "more" to Pair(Icons.Default.MoreHoriz,tr("المزيد","More"))
    )
    NavigationBar { labels.forEach { (route,pair) -> NavigationBarItem(selected=nav.currentDestination?.route==route,onClick={nav.navigate(route){launchSingleTop=true}},icon={Icon(pair.first,pair.second)},label={Text(pair.second)}) } }
}''', s, count=1, flags=re.S)
s=re.sub(r'@Composable fun SmartCenterScreen\(nav: NavHostController\) \{.*?\n\}\n\n@Composable private fun HeroCard', r'''@Composable fun SmartCenterScreen(nav: NavHostController) {
    val context=LocalContext.current
    var buckets by remember{mutableStateOf<SmartCenterBuckets?>(null)}
    var refreshToken by remember{mutableIntStateOf(0)}
    LaunchedEffect(refreshToken){buckets=withContext(Dispatchers.IO){repo(context).smartBuckets()}}
    LazyColumn(contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{HeroCard()}
        buckets?.let{b->
            val sections=listOf(tr("اليوم","Today") to b.today,tr("هذا الأسبوع","This week") to b.week,tr("هذا الشهر","This month") to b.month,tr("متأخرة","Overdue") to b.overdue,tr("مهمة","Important") to b.important)
            sections.forEach{(title,events)->item{SectionHeader(title,events.size)};if(events.isEmpty())item{EmptyMini(tr("لا توجد مناسبات","No events"))}else items(events.take(5),key={it.id}){event->EventCard(event,context){refreshToken++}}}
        }
    }
}

@Composable private fun HeroCard''', s, count=1, flags=re.S)
s=s.replace('Text("مركز المناسبات الذكي"','Text(tr("مركز المناسبات الذكي","Smart Events Center")')
s=s.replace('Text("الأقرب والأهم أولًا، مع إجراءات سريعة لكل مناسبة."','Text(tr("الأقرب والأهم أولًا، مع إجراءات سريعة لكل مناسبة.","Closest and most important first, with quick actions for every event.")')
s=re.sub(r'@Composable private fun EventCard\(e: EventModel, context: Context\) \{.*?\n\}\n\n@Composable fun SmartAddScreen', r'''@Composable private fun EventCard(e:EventModel,context:Context,onChanged:()->Unit={}){
    var confirmDelete by remember(e.id){mutableStateOf(false)}
    var deleted by remember(e.id){mutableStateOf(false)}
    if(deleted)return
    val locale=if(LocalAppLanguage.current=="en") Locale.ENGLISH else ar
    val date=remember(e.startEpochMillis,LocalAppLanguage.current){Instant.ofEpochMilli(e.startEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("EEEE d MMM • HH:mm",locale))}
    Card(modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){Box(Modifier.size(10.dp).background(Color(e.category.color),RoundedCornerShape(50)));Spacer(Modifier.width(8.dp));Text(e.title,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f),textAlign=TextAlign.Start);if(e.importance>=2)Icon(Icons.Default.Star,tr("مهم","Important"),tint=Coral)}
            Text(date,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Start,color=Color.Gray);if(e.locationName.isNotBlank())Text("📍 ${e.locationName}",modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Start)
            val linkedPerson=e.personId?.let{repo(context).personById(it)}
            LazyRow(horizontalArrangement=Arrangement.spacedBy(4.dp)){
                item{AssistChip(onClick={UiFeedback.click(context);ShareTools.shareText(context,ShareTools.eventText(e),true);linkedPerson?.let{repo(context).recordInteraction(it.id,e.id,"message")}},label={Text(tr("رسالة","Message"))},leadingIcon={Icon(Icons.Default.Message,null)})}
                if(linkedPerson?.phone?.isNotBlank()==true)item{AssistChip(onClick={UiFeedback.click(context);context.startActivity(Intent(Intent.ACTION_DIAL,Uri.parse("tel:${linkedPerson.phone}")));repo(context).recordInteraction(linkedPerson.id,e.id,"call")},label={Text(tr("اتصال","Call"))},leadingIcon={Icon(Icons.Default.Call,null)})}
                if(e.locationName.isNotBlank()||e.latitude!=null)item{AssistChip(onClick={UiFeedback.click(context);val uri=if(e.latitude!=null&&e.longitude!=null)Uri.parse("geo:${e.latitude},${e.longitude}?q=${e.latitude},${e.longitude}")else Uri.parse("geo:0,0?q=${Uri.encode(e.locationName)}");context.startActivity(Intent(Intent.ACTION_VIEW,uri))},label={Text(tr("الموقع","Location"))},leadingIcon={Icon(Icons.Default.LocationOn,null)})}
                item{AssistChip(onClick={UiFeedback.click(context);ShareTools.shareBitmap(context,ShareTools.qrBitmap(ShareTools.eventText(e)),"event_qr_${e.id}.png")},label={Text("QR")},leadingIcon={Icon(Icons.Default.QrCode,null)})}
                item{AssistChip(onClick={UiFeedback.click(context);ShareTools.shareBitmap(context,ShareTools.memoryCard(e),"event_card_${e.id}.png")},label={Text(tr("بطاقة","Card"))},leadingIcon={Icon(Icons.Default.Image,null)})}
                item{AssistChip(onClick={UiFeedback.click(context);confirmDelete=true},label={Text(tr("حذف","Delete"))},leadingIcon={Icon(Icons.Default.Delete,null)})}
                item{AssistChip(onClick={val r=repo(context);r.upsertEvent(e.copy(attended=true,status=EventStatus.DONE,updatedAt=System.currentTimeMillis()));e.personId?.let{r.recordInteraction(it,e.id,"attended")};UiFeedback.success(context);onChanged()},label={Text(tr("حضرت","Attended"))},leadingIcon={Icon(Icons.Default.Check,null)})}
            }
        }
    }
    if(confirmDelete)AlertDialog(onDismissRequest={confirmDelete=false},title={Text(tr("حذف المناسبة","Delete event"))},text={Text(tr("هل تريد نقل «${e.title}» إلى سلة المحذوفات؟ يمكنك استعادتها لاحقًا.","Move “${e.title}” to Trash? You can restore it later."))},confirmButton={TextButton(onClick={runCatching{ReminderScheduler(context).cancel(e.id);com.rafat.munasabati.compat.TrashManager(context,repo(context)).moveToTrash(e)};UiFeedback.delete(context);deleted=true;confirmDelete=false;onChanged()}){Text(tr("حذف","Delete"))}},dismissButton={TextButton(onClick={confirmDelete=false}){Text(tr("إلغاء","Cancel"))}})
}

@Composable fun SmartAddScreen''', s, count=1, flags=re.S)
s=s.replace('repo(context).upsertEvent(e); selectedMediaUri?.let{repo(context).addMedia(e.id,it.toString())}; ReminderScheduler(context).schedule(e); saved=true','repo(context).upsertEvent(e); selectedMediaUri?.let{repo(context).addMedia(e.id,it.toString())}; ReminderScheduler(context).schedule(e); UiFeedback.success(context); saved=true')
s=s.replace('SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf("شهر","أسبوع","Timeline").forEachIndexed { i,m -> SegmentedButton(selected=mode==m,onClick={mode=m},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(m)} } }', 'SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) { listOf("شهر","أسبوع","Timeline").forEachIndexed { i,m -> val label=when(m){"شهر"->tr("شهر","Month");"أسبوع"->tr("أسبوع","Week");else->"Timeline"}; SegmentedButton(selected=mode==m,onClick={mode=m},shape=SegmentedButtonDefaults.itemShape(i,3)){Text(label)} } }')
s=s.replace('Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",ar)),fontWeight=FontWeight.Bold,fontSize=20.sp)', 'val locale=if(LocalAppLanguage.current=="en") Locale.ENGLISH else ar;Text(month.format(DateTimeFormatter.ofPattern("MMMM yyyy",locale)),fontWeight=FontWeight.Bold,fontSize=20.sp)')
s=s.replace('val weekdays=CalendarRules.weekdayNamesArabic; LazyVerticalGrid', 'val weekdays=if(LocalAppLanguage.current=="en") listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday") else CalendarRules.weekdayNamesArabic; val weekendNames=if(LocalAppLanguage.current=="en") listOf("Friday","Saturday") else listOf("الجمعة","السبت"); LazyVerticalGrid')
s=s.replace('if(d in listOf("الجمعة","السبت")) Weekend else Color.Transparent','if(d in weekendNames) Weekend else Color.Transparent')
s=s.replace('if(d in listOf("الجمعة","السبت")) WeekendText else Color.Gray','if(d in weekendNames) WeekendText else Color.Gray')
s=re.sub(r'@Composable private fun WeekView\(events:List<EventModel>\)\{.*?\}\}\n@Composable private fun TimelineView', r'''@Composable private fun WeekView(events:List<EventModel>){val start=LocalDate.now();val locale=if(LocalAppLanguage.current=="en")Locale.ENGLISH else ar;LazyColumn{items((0..6).toList()){i->val d=start.plusDays(i.toLong());val es=events.filter{Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).toLocalDate()==d};Column(Modifier.fillMaxWidth().padding(8.dp)){Text(d.format(DateTimeFormatter.ofPattern("EEEE d MMM",locale)),fontWeight=FontWeight.Bold);es.forEach{Text("• ${it.title} — ${Instant.ofEpochMilli(it.startEpochMillis).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm"))}")};if(es.isEmpty())Text(tr("لا توجد مناسبات","No events"),color=Color.Gray)}}}}
@Composable private fun TimelineView''', s, count=1, flags=re.S)
s=re.sub(r'@Composable fun MoreScreen\(nav:NavHostController\)\{.*?\}\}\}\s*$', r'''@Composable fun MoreScreen(nav:NavHostController){LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
    item{ListItem(headlineContent={Text(tr("الإعدادات واللغة والصوت","Settings, language & sound"))},supportingContent={Text(tr("لغة البرنامج • أصوات الواجهة والتنبيهات • الاهتزاز","App language • sounds • vibration"))},leadingContent={Icon(Icons.Default.Settings,null)},modifier=Modifier.clickable{nav.navigate("settings")})}
    item{ListItem(headlineContent={Text(tr("Dashboard والإحصائيات","Dashboard & statistics"))},leadingContent={Icon(Icons.Default.BarChart,null)},modifier=Modifier.clickable{nav.navigate("stats")})}
    item{ListItem(headlineContent={Text(tr("الأدوات والمزايا الكاملة","All tools & features"))},supportingContent={Text(tr("النسخ الاحتياطي • المزامنة • أهل البيت • السلة • القوالب • الخصوصية","Backup • sync • Ahl al-Bayt • trash • templates • privacy"))},leadingContent={Icon(Icons.Default.Apps,null)},modifier=Modifier.clickable{nav.navigate("legacy")})}
    item{ListItem(headlineContent={Text("Diagnostics")},leadingContent={Icon(Icons.Default.HealthAndSafety,null)},modifier=Modifier.clickable{nav.navigate("diagnostics")})}
    item{Card(colors=CardDefaults.cardColors(containerColor=Mint)){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(4.dp)){Text(tr("حول مناسبـاتي","About Munasabati"),fontWeight=FontWeight.Bold);Text("رأفت ناصر الناصر  •  Rafat Nasser Alnasser",fontWeight=FontWeight.Bold);Text(tr("© 2026 رأفت ناصر الناصر — جميع حقوق البرنامج محفوظة.","© 2026 Rafat Nasser Alnasser — All rights reserved."),fontSize=12.sp);Text("${BuildConfig.CHANNEL.uppercase()} • v${BuildConfig.VERSION_NAME}",color=Color.Gray)}}}
}}''', s, count=1, flags=re.S)
p.write_text(s)

p=root/'app/src/main/java/com/rafat/munasabati/reminder/ReminderEngine.kt'
s=p.read_text()
if 'android.media.AudioAttributes' not in s:
    s=s.replace('import android.location.LocationManager','import android.location.LocationManager\nimport android.media.AudioAttributes\nimport android.media.RingtoneManager')
if 'import com.rafat.munasabati.compat.AppPreferences' not in s:
    s=s.replace('import com.rafat.munasabati.compat.AhlBaytCalendar','import com.rafat.munasabati.compat.AhlBaytCalendar\nimport com.rafat.munasabati.compat.AppPreferences')
s=s.replace('    private fun lastKnownLocation(): Location? {', r'''    fun cancel(eventId:String){
        val am=context.getSystemService(AlarmManager::class.java)
        listOf(false,true).forEach{follow->
            val intent=Intent(context,EventAlarmReceiver::class.java).putExtra("event_id",eventId).putExtra("follow_up",follow)
            val pi=PendingIntent.getBroadcast(context,(eventId+follow).hashCode(),intent,PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            if(pi!=null){runCatching{am.cancel(pi)};pi.cancel()}
            WorkManager.getInstance(context).cancelUniqueWork("event-$eventId-$follow")
        }
        runCatching{LocationServices.getGeofencingClient(context).removeGeofences(listOf(eventId))}
    }

    private fun lastKnownLocation(): Location? {''')
s=re.sub(r'object ReminderNotifier \{.*?\n\}', r'''object ReminderNotifier {
    private const val CHANNEL_SOUND="munasabati_events_v502_sound"
    private const val CHANNEL_SILENT="munasabati_events_v502_silent"
    fun show(context:Context,event:EventModel,subtitle:String){
        val prefs=AppPreferences(context);val soundEnabled=prefs.soundEnabled();val channelId=if(soundEnabled)CHANNEL_SOUND else CHANNEL_SILENT
        val manager=context.getSystemService(NotificationManager::class.java)
        if(Build.VERSION.SDK_INT>=26){
            val importance=if(event.importance>=2)NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT
            val channel=NotificationChannel(channelId,if(soundEnabled)"مناسباتي — صوت" else "مناسباتي — صامت",importance)
            if(soundEnabled){val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();channel.setSound(uri,attrs);channel.enableVibration(true)}else{channel.setSound(null,null);channel.enableVibration(false)}
            manager.createNotificationChannel(channel)
        }
        val open=PendingIntent.getActivity(context,event.id.hashCode(),Intent(context,MainActivity::class.java).putExtra("event_id",event.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val ack=PendingIntent.getBroadcast(context,event.id.hashCode()+7,Intent(context,ReminderActionReceiver::class.java).putExtra("event_id",event.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val builder=NotificationCompat.Builder(context,channelId).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(event.title).setContentText(subtitle).setContentIntent(open).setAutoCancel(true).setPriority(if(event.importance>=2)NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT).setCategory(NotificationCompat.CATEGORY_EVENT).setVisibility(NotificationCompat.VISIBILITY_PUBLIC).addAction(0,"تم الاطلاع",ack)
        if(Build.VERSION.SDK_INT<26){if(soundEnabled)builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))else builder.setSound(null)}
        if(event.strongAlert||event.importance>=3){val strong=PendingIntent.getActivity(context,event.id.hashCode()+99,Intent(context,StrongAlertActivity::class.java).putExtra("event_id",event.id),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);builder.setFullScreenIntent(strong,true).setOngoing(false)}
        if(Build.VERSION.SDK_INT<33||ActivityCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)NotificationManagerCompat.from(context).notify(event.id.hashCode(),builder.build())
    }
}''', s, count=1, flags=re.S)
p.write_text(s)

p=root/'app/src/main/java/com/rafat/munasabati/compat/StrongAlertActivity.kt'
p.write_text(r'''package com.rafat.munasabati.compat

import android.app.Activity
import android.content.Context
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.widget.*
import com.rafat.munasabati.MunasabatiApp

class StrongAlertActivity:Activity(){
    private var ringtone:Ringtone?=null
    private var vibrator:Vibrator?=null
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        startAlertFeedback()
        val id=intent.getStringExtra("event_id");val e=id?.let{(applicationContext as MunasabatiApp).repository.eventById(it)}
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER;setPadding(48,48,48,48);setBackgroundColor(0xFFFFF9F0.toInt())}
        box.addView(TextView(this).apply{text="تنبيه مهم";textSize=30f;gravity=android.view.Gravity.CENTER})
        box.addView(TextView(this).apply{text=e?.title?:"مناسبة مهمة";textSize=24f;gravity=android.view.Gravity.CENTER;setPadding(0,32,0,32)})
        box.addView(Button(this).apply{text="تم الاطلاع";setOnClickListener{e?.let{(applicationContext as MunasabatiApp).repository.upsertEvent(it.copy(acknowledged=true,updatedAt=System.currentTimeMillis()))};finish()}})
        setContentView(box)
    }
    private fun startAlertFeedback(){
        val prefs=AppPreferences(this)
        if(prefs.soundEnabled())runCatching{val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?:RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);ringtone=RingtoneManager.getRingtone(this,uri);if(Build.VERSION.SDK_INT>=28)ringtone?.isLooping=true;ringtone?.play()}
        if(prefs.hapticEnabled())runCatching{vibrator=if(Build.VERSION.SDK_INT>=31)getSystemService(VibratorManager::class.java).defaultVibrator else @Suppress("DEPRECATION")(getSystemService(Context.VIBRATOR_SERVICE) as Vibrator);val pattern=longArrayOf(0,500,250,500,500);if(Build.VERSION.SDK_INT>=26)vibrator?.vibrate(VibrationEffect.createWaveform(pattern,1))else @Suppress("DEPRECATION")vibrator?.vibrate(pattern,1)}
    }
    override fun onDestroy(){runCatching{ringtone?.stop()};runCatching{vibrator?.cancel()};super.onDestroy()}
}
''')

p=root/'app/src/test/java/com/rafat/munasabati/compat/AppPreferencesLogicTest.kt'
p.write_text(r'''package com.rafat.munasabati.compat
import org.junit.Assert.*
import org.junit.Test
class AppPreferencesLogicTest{
 @Test fun legacyBooleanTypesAreSafe(){assertTrue(PreferenceCoercion.bool(true));assertTrue(PreferenceCoercion.bool("true"));assertTrue(PreferenceCoercion.bool("1"));assertFalse(PreferenceCoercion.bool("false",true));assertFalse(PreferenceCoercion.bool(0,true))}
 @Test fun legacyLanguageValuesAreRestored(){assertEquals("ar",PreferenceCoercion.language("ar"));assertEquals("en",PreferenceCoercion.language("en"));assertEquals("en",PreferenceCoercion.language("English"));assertEquals("ar",PreferenceCoercion.language(null))}
}
''')

print('patched v5.0.2 source')
