from pathlib import Path
import re
root=Path('v5src')

# Version bump; keep R8 disabled from 5.0.1 recovery release.
p=root/'app/build.gradle.kts'; s=p.read_text()
s=s.replace('versionCode = 61','versionCode = 62').replace('versionName = "5.0.1"','versionName = "5.0.2"')
p.write_text(s)

# Restore user-experience settings compatible with v4 preference keys.
ux=r'''package com.rafat.munasabati.compat

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper

object UserExperienceSettings {
    private const val PREFS = "munasabati_settings"
    fun normalizeLanguage(value: Any?): String {
        val v=value?.toString()?.trim()?.lowercase().orEmpty()
        return if(v=="en" || v=="english" || v.contains("eng")) "en" else "ar"
    }
    fun language(context:Context):String{
        val all=runCatching{context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).all}.getOrDefault(emptyMap())
        return normalizeLanguage(all["language"]?:all["app_language"]?:all["ui_language"])
    }
    fun setLanguage(context:Context,language:String){
        val lang=normalizeLanguage(language)
        runCatching{context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString("language",lang).putString("app_language",lang).apply()}
    }
    private fun boolValue(value:Any?,default:Boolean):Boolean=when(value){
        is Boolean->value; is Number->value.toInt()!=0
        is String->when(value.trim().lowercase()){ "1","true","yes","on","enabled"->true;"0","false","no","off","disabled"->false;else->default }
        else->default
    }
    fun soundEnabled(context:Context):Boolean{
        val all=runCatching{context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).all}.getOrDefault(emptyMap())
        return boolValue(all["sound_enabled"]?:all["soundEnabled"]?:all["sound_effects_enabled"],true)
    }
    fun setSoundEnabled(context:Context,enabled:Boolean){runCatching{context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean("sound_enabled",enabled).putBoolean("sound_effects_enabled",enabled).apply()}}
    enum class Feedback{CLICK,SUCCESS,DELETE}
    fun play(context:Context,feedback:Feedback=Feedback.CLICK){
        if(!soundEnabled(context))return
        val audio=context.getSystemService(AudioManager::class.java);if(audio?.ringerMode==AudioManager.RINGER_MODE_SILENT)return
        val toneId=when(feedback){Feedback.SUCCESS->ToneGenerator.TONE_PROP_ACK;Feedback.DELETE->ToneGenerator.TONE_PROP_NACK;Feedback.CLICK->ToneGenerator.TONE_PROP_BEEP}
        runCatching{val tone=ToneGenerator(AudioManager.STREAM_SYSTEM,70);tone.startTone(toneId,if(feedback==Feedback.DELETE)120 else 80);Handler(Looper.getMainLooper()).postDelayed({runCatching{tone.release()}},220)}
    }
}
'''
p=root/'app/src/main/java/com/rafat/munasabati/compat/UserExperienceSettings.kt';p.write_text(ux)

settings=r'''@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.rafat.munasabati.ui

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rafat.munasabati.compat.UserExperienceSettings

@Composable fun uiText(ar:String,en:String):String{val c=LocalContext.current;return if(UserExperienceSettings.language(c)=="en")en else ar}

@Composable fun SettingsScreen(){
 val context=LocalContext.current;var language by remember{mutableStateOf(UserExperienceSettings.language(context))};var sound by remember{mutableStateOf(UserExperienceSettings.soundEnabled(context))}
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),verticalArrangement=Arrangement.spacedBy(14.dp)){
  Text(uiText("الإعدادات","Settings"),fontSize=28.sp,fontWeight=FontWeight.Bold)
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Language,null);Spacer(Modifier.width(8.dp));Text(uiText("اللغة","Language"),fontWeight=FontWeight.Bold,fontSize=20.sp)}
   Text(uiText("اختر لغة واجهة التطبيق","Choose the app interface language"),color=MaterialTheme.colorScheme.onSurfaceVariant)
   Row(verticalAlignment=Alignment.CenterVertically){
    RadioButton(selected=language=="ar",onClick={language="ar";UserExperienceSettings.setLanguage(context,"ar");UserExperienceSettings.play(context);(context as? Activity)?.recreate()});Text("العربية",Modifier.weight(1f))
    RadioButton(selected=language=="en",onClick={language="en";UserExperienceSettings.setLanguage(context,"en");UserExperienceSettings.play(context);(context as? Activity)?.recreate()});Text("English")
   }
  }}
  Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
   Row(verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.MusicNote,null);Spacer(Modifier.width(8.dp));Text(uiText("الصوت واللمس","Sound & touch"),fontWeight=FontWeight.Bold,fontSize=20.sp)}
   Row(verticalAlignment=Alignment.CenterVertically){Text(uiText("أصوات الواجهة والأيقونات","Interface & icon sounds"),Modifier.weight(1f));Switch(checked=sound,onCheckedChange={sound=it;UserExperienceSettings.setSoundEnabled(context,it);if(it)UserExperienceSettings.play(context,UserExperienceSettings.Feedback.SUCCESS)})}
   OutlinedButton(onClick={UserExperienceSettings.play(context,UserExperienceSettings.Feedback.SUCCESS)},enabled=sound){Text(uiText("تجربة الصوت","Preview sound"))}
   Text(uiText("تنبيهات المناسبات تستخدم قناة صوت واهتزاز مفعّلة.","Event reminders use a sound-and-vibration enabled notification channel."),fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
  }}
  DesignerRightsCard()
 }
}

@Composable fun DesignerRightsCard(){Card(Modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surfaceVariant)){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(4.dp)){Icon(Icons.Default.Verified,null);Text("Designed & developed by",fontWeight=FontWeight.SemiBold);Text("Rafat Nasser Alnasser",fontWeight=FontWeight.Bold,fontSize=18.sp);Text("© 2026 Rafat Nasser Alnasser",textAlign=TextAlign.Center,fontSize=12.sp);Text("All rights reserved.",textAlign=TextAlign.Center,fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}}}
'''
p=root/'app/src/main/java/com/rafat/munasabati/ui/SettingsScreen.kt';p.write_text(settings)

# Main activity follows selected UI direction.
p=root/'app/src/main/java/com/rafat/munasabati/MainActivity.kt';s=p.read_text()
if 'import com.rafat.munasabati.compat.UserExperienceSettings' not in s:s=s.replace('import com.rafat.munasabati.ui.theme.MunasabatiTheme','import com.rafat.munasabati.ui.theme.MunasabatiTheme\nimport com.rafat.munasabati.compat.UserExperienceSettings')
s=s.replace('window.decorView.layoutDirection = View.LAYOUT_DIRECTION_RTL','window.decorView.layoutDirection = if (UserExperienceSettings.language(this)=="en") View.LAYOUT_DIRECTION_LTR else View.LAYOUT_DIRECTION_RTL')
p.write_text(s)

# Full weekday names in both languages.
p=root/'app/src/main/java/com/rafat/munasabati/calendar/CalendarRules.kt';s=p.read_text()
if 'weekdayNamesEnglish' not in s:s=s.replace('val weekdayNamesArabic = listOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")','val weekdayNamesArabic = listOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت")\n    val weekdayNamesEnglish = listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday")')
p.write_text(s)

# Trash is committed synchronously before deleting the database row, and returns a result to UI.
p=root/'app/src/main/java/com/rafat/munasabati/compat/LegacyDataTools.kt';s=p.read_text()
s=s.replace('fun moveToTrash(e:EventModel){val a=items();a.put(EventJsonCodec.toJson(e).put("deletedAt",System.currentTimeMillis()));prefs.edit().putString("items",a.toString()).apply();repo.deleteEvent(e.id)}','''fun moveToTrash(e:EventModel):Boolean=runCatching{\n        val a=items();a.put(EventJsonCodec.toJson(e).put("deletedAt",System.currentTimeMillis()))\n        if(!prefs.edit().putString("items",a.toString()).commit()) return@runCatching false\n        repo.deleteEvent(e.id)>0\n    }.getOrDefault(false)''')
p.write_text(s)

# Restore notification sound with a fresh Android notification channel.
p=root/'app/src/main/java/com/rafat/munasabati/reminder/ReminderEngine.kt';s=p.read_text()
if 'android.media.AudioAttributes' not in s:s=s.replace('import android.os.Build','import android.os.Build\nimport android.media.AudioAttributes\nimport android.media.RingtoneManager')
s=s.replace('const val CHANNEL = "munasabati_events_v5"','const val CHANNEL = "munasabati_events_v5_2"')
old='if (Build.VERSION.SDK_INT >= 26) manager.createNotificationChannel(NotificationChannel(CHANNEL, "مناسباتي", if (event.importance >= 2) NotificationManager.IMPORTANCE_HIGH else NotificationManager.IMPORTANCE_DEFAULT))'
new='''if (Build.VERSION.SDK_INT >= 26) {\n            val attrs=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()\n            val ch=NotificationChannel(CHANNEL,"مناسباتي",NotificationManager.IMPORTANCE_HIGH).apply{description="تنبيهات المناسبات بالصوت والاهتزاز";enableVibration(true);vibrationPattern=longArrayOf(0,250,180,250);setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),attrs)}\n            manager.createNotificationChannel(ch)\n        }'''
if old not in s: raise SystemExit('notification channel marker missing')
s=s.replace(old,new)
s=s.replace('.addAction(0, "تم الاطلاع", ack)', '.addAction(0, "تم الاطلاع", ack).setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)')
p.write_text(s)

# Strong Alert again has alarm sound + vibration until acknowledged.
p=root/'app/src/main/java/com/rafat/munasabati/compat/StrongAlertActivity.kt'
p.write_text(r'''package com.rafat.munasabati.compat
import android.app.Activity
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.view.WindowManager
import android.widget.*
import com.rafat.munasabati.MunasabatiApp
class StrongAlertActivity:Activity(){
 private var ringtone:Ringtone?=null;private var vibrator:Vibrator?=null
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);val id=intent.getStringExtra("event_id");val e=id?.let{(applicationContext as MunasabatiApp).repository.eventById(it)}
  if(UserExperienceSettings.soundEnabled(this)){runCatching{val uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?:RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);ringtone=RingtoneManager.getRingtone(this,uri);if(Build.VERSION.SDK_INT>=28)ringtone?.isLooping=true;ringtone?.play()};runCatching{vibrator=getSystemService(Vibrator::class.java);if(Build.VERSION.SDK_INT>=26)vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0,500,300,500),0)) else @Suppress("DEPRECATION") vibrator?.vibrate(longArrayOf(0,500,300,500),0)}}
  val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=android.view.Gravity.CENTER;setPadding(48,48,48,48);setBackgroundColor(0xFFFFF9F0.toInt())};box.addView(TextView(this).apply{text="تنبيه مهم";textSize=30f;gravity=android.view.Gravity.CENTER});box.addView(TextView(this).apply{text=e?.title?:"مناسبة مهمة";textSize=24f;gravity=android.view.Gravity.CENTER;setPadding(0,32,0,32)});box.addView(Button(this).apply{text="تم الاطلاع";setOnClickListener{e?.let{(applicationContext as MunasabatiApp).repository.upsertEvent(it.copy(acknowledged=true,updatedAt=System.currentTimeMillis()))};finish()}});setContentView(box)}
 override fun onDestroy(){runCatching{ringtone?.stop()};runCatching{vibrator?.cancel()};super.onDestroy()}
}
''')

# UI: live language choice, settings route, delete confirmation/refresh, credits.
p=root/'app/src/main/java/com/rafat/munasabati/ui/AppUi.kt';s=p.read_text()
if 'private fun uiLocale' not in s:s=s.replace('private val ar = Locale("ar")','private val ar = Locale("ar")\nprivate fun uiLocale(context:Context)=if(com.rafat.munasabati.compat.UserExperienceSettings.language(context)=="en") Locale.ENGLISH else ar')
s=s.replace('CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl)','CompositionLocalProvider(LocalLayoutDirection provides if (com.rafat.munasabati.compat.UserExperienceSettings.language(LocalContext.current)=="en") LayoutDirection.Ltr else LayoutDirection.Rtl)')
s=s.replace('composable("privacy") { PrivacyScreen() }','composable("privacy") { PrivacyScreen() }\n            composable("settings") { SettingsScreen() }')
s=s.replace('Text("مناسباتي", fontWeight = FontWeight.Bold); Text("لا تتذكر الموعد فقط، تذكّر الشخص واللحظة.", fontSize = 11.sp)','Text(uiText("مناسباتي","Munasabati"), fontWeight = FontWeight.Bold); Text(uiText("لا تتذكر الموعد فقط، تذكّر الشخص واللحظة.","Don’t just remember the date — remember the person and the moment."), fontSize = 11.sp)')
s=s.replace('listOf("center" to Pair(Icons.Default.Home,"المركز"), "calendar" to Pair(Icons.Default.DateRange,"التقويم"), "people" to Pair(Icons.Default.People,"الأشخاص"), "memories" to Pair(Icons.Default.Favorite,"الذكريات"), "more" to Pair(Icons.Default.MoreHoriz,"المزيد"))','listOf("center" to Pair(Icons.Default.Home,uiText("المركز","Center")), "calendar" to Pair(Icons.Default.DateRange,uiText("التقويم","Calendar")), "people" to Pair(Icons.Default.People,uiText("الأشخاص","People")), "memories" to Pair(Icons.Default.Favorite,uiText("الذكريات","Memories")), "more" to Pair(Icons.Default.MoreHoriz,uiText("المزيد","More")))')
s=s.replace('var buckets by remember { mutableStateOf<SmartCenterBuckets?>(null) }\n    LaunchedEffect(Unit) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }','var buckets by remember { mutableStateOf<SmartCenterBuckets?>(null) }\n    var refreshKey by remember { mutableIntStateOf(0) }\n    LaunchedEffect(refreshKey) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }')
s=s.replace('val sections = listOf("اليوم" to b.today, "هذا الأسبوع" to b.week, "هذا الشهر" to b.month, "متأخرة" to b.overdue, "مهمة" to b.important)','val sections = listOf(uiText("اليوم","Today") to b.today, uiText("هذا الأسبوع","This week") to b.week, uiText("هذا الشهر","This month") to b.month, uiText("متأخرة","Overdue") to b.overdue, uiText("مهمة","Important") to b.important)')
s=s.replace('if (events.isEmpty()) item { EmptyMini("لا توجد مناسبات") }','if (events.isEmpty()) item { EmptyMini(uiText("لا توجد مناسبات","No events")) }')
s=s.replace('else items(events.take(5), key = { it.id }) { EventCard(it, context) }','else items(events.take(5), key = { it.id }) { EventCard(it, context) { refreshKey++ } }')
s=s.replace('Text("مركز المناسبات الذكي", modifier','Text(uiText("مركز المناسبات الذكي","Smart Event Center"), modifier')
s=s.replace('Text("الأقرب والأهم أولًا، مع إجراءات سريعة لكل مناسبة.", modifier','Text(uiText("الأقرب والأهم أولًا، مع إجراءات سريعة لكل مناسبة.","Nearest and most important first, with quick actions for every event."), modifier')
s=s.replace('@Composable private fun EventCard(e: EventModel, context: Context) {','@Composable private fun EventCard(e: EventModel, context: Context, onChanged:()->Unit = {}) {\n    var removed by remember(e.id) { mutableStateOf(false) }\n    var confirmDelete by remember(e.id) { mutableStateOf(false) }\n    if (removed) return')
s=s.replace('DateTimeFormatter.ofPattern("EEEE d MMM • HH:mm", ar)','DateTimeFormatter.ofPattern("EEEE d MMM • HH:mm", uiLocale(context))')
s=s.replace('item { AssistChip(onClick={ com.rafat.munasabati.compat.TrashManager(context,repo(context)).moveToTrash(e) },label={Text("السلة")},leadingIcon={Icon(Icons.Default.Delete,null)}) }','item { AssistChip(onClick={ confirmDelete=true },label={Text(uiText("حذف","Delete"))},leadingIcon={Icon(Icons.Default.Delete,null)}) }')
for a,e in [('رسالة','Message'),('اتصال','Call'),('الموقع','Location'),('بطاقة','Card'),('حضرت','Attended')]:s=s.replace(f'Text("{a}")',f'Text(uiText("{a}","{e}"))')
needle='''        }\n    }\n}\n\n@Composable fun SmartAddScreen'''
repl='''        }\n    }\n    if(confirmDelete){AlertDialog(onDismissRequest={confirmDelete=false},title={Text(uiText("حذف المناسبة","Delete event"))},text={Text(uiText("سيتم نقل المناسبة إلى سلة المحذوفات ويمكن استعادتها لاحقًا.","The event will be moved to Trash and can be restored later."))},confirmButton={TextButton(onClick={val ok=com.rafat.munasabati.compat.TrashManager(context,repo(context)).moveToTrash(e);if(ok){com.rafat.munasabati.compat.UserExperienceSettings.play(context,com.rafat.munasabati.compat.UserExperienceSettings.Feedback.DELETE);removed=true;onChanged()};confirmDelete=false}){Text(uiText("حذف","Delete"))}},dismissButton={TextButton(onClick={confirmDelete=false}){Text(uiText("إلغاء","Cancel"))}})}\n}\n\n@Composable fun SmartAddScreen'''
if needle not in s:raise SystemExit('event card end marker missing')
s=s.replace(needle,repl,1)
# Calendar language + full names.
s=s.replace('var mode by remember{mutableStateOf("شهر")}', 'var mode by remember{mutableStateOf(if(com.rafat.munasabati.compat.UserExperienceSettings.language(context)=="en") "Month" else "شهر")}')
s=s.replace('listOf("شهر","أسبوع","Timeline")','listOf(uiText("شهر","Month"),uiText("أسبوع","Week"),uiText("خط زمني","Timeline"))')
s=s.replace('when(mode){"شهر"->MonthView(month,filtered,onPrev={month=month.minusMonths(1)},onNext={month=month.plusMonths(1)});"أسبوع"->WeekView(filtered);else->TimelineView(filtered)}','when(mode){uiText("شهر","Month")->MonthView(month,filtered,onPrev={month=month.minusMonths(1)},onNext={month=month.plusMonths(1)});uiText("أسبوع","Week")->WeekView(filtered);else->TimelineView(filtered)}')
s=s.replace('@Composable private fun MonthView(month: YearMonth, events: List<EventModel>, onPrev:()->Unit,onNext:()->Unit) {\n    Row(', '@Composable private fun MonthView(month: YearMonth, events: List<EventModel>, onPrev:()->Unit,onNext:()->Unit) {\n    val context=LocalContext.current; val locale=uiLocale(context)\n    Row(')
s=s.replace('DateTimeFormatter.ofPattern("MMMM yyyy",ar)','DateTimeFormatter.ofPattern("MMMM yyyy",locale)')
s=s.replace('val weekdays=CalendarRules.weekdayNamesArabic;','val english=com.rafat.munasabati.compat.UserExperienceSettings.language(context)=="en"; val weekdays=if(english) CalendarRules.weekdayNamesEnglish else CalendarRules.weekdayNamesArabic;')
s=s.replace('if(d in listOf("الجمعة","السبت")) Weekend','if(d in listOf("الجمعة","السبت","Friday","Saturday")) Weekend').replace('if(d in listOf("الجمعة","السبت")) WeekendText','if(d in listOf("الجمعة","السبت","Friday","Saturday")) WeekendText')
s=s.replace('DateTimeFormatter.ofPattern("EEEE d MMM",ar)','DateTimeFormatter.ofPattern("EEEE d MMM",uiLocale(LocalContext.current))')
# More screen includes settings + exact legacy designer credit.
pat=r'@Composable fun MoreScreen\(nav:NavHostController\)\{.*?\}\}\}\s*$';m=re.search(pat,s,re.S)
if not m:raise SystemExit('MoreScreen marker missing')
more='''@Composable fun MoreScreen(nav:NavHostController){\n    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){\n        item{ListItem(headlineContent={Text(uiText("الإعدادات","Settings"))},supportingContent={Text(uiText("اللغة • الأصوات • حقوق المصمم","Language • sounds • designer credits"))},leadingContent={Icon(Icons.Default.Settings,null)},modifier=Modifier.clickable{com.rafat.munasabati.compat.UserExperienceSettings.play(LocalContext.current);nav.navigate("settings")})}\n        item{ListItem(headlineContent={Text(uiText("Dashboard والإحصائيات","Dashboard & statistics"))},leadingContent={Icon(Icons.Default.BarChart,null)},modifier=Modifier.clickable{nav.navigate("stats")})}\n        item{ListItem(headlineContent={Text(uiText("الأدوات والمزايا الكاملة","Full tools & features"))},supportingContent={Text(uiText("النسخ الاحتياطي • المزامنة • أهل البيت • السلة • القوالب • الخصوصية","Backup • sync • Ahl al-Bayt • trash • templates • privacy"))},leadingContent={Icon(Icons.Default.Apps,null)},modifier=Modifier.clickable{nav.navigate("legacy")})}\n        item{ListItem(headlineContent={Text("Diagnostics")},leadingContent={Icon(Icons.Default.HealthAndSafety,null)},modifier=Modifier.clickable{nav.navigate("diagnostics")})}\n        item{Card(colors=CardDefaults.cardColors(containerColor=Mint)){Column(Modifier.padding(16.dp)){Text(uiText("قناة الإصدار","Release channel"),fontWeight=FontWeight.Bold);Text(BuildConfig.CHANNEL.uppercase());Text("v${BuildConfig.VERSION_NAME}",color=Color.Gray)}}}\n        item{DesignerRightsCard()}\n    }\n}\n'''
s=s[:m.start()]+more
p.write_text(s)

# Pure JVM tests for language normalization and weekday completeness.
t=root/'app/src/test/java/com/rafat/munasabati/compat/UserExperienceSettingsTest.kt';t.parent.mkdir(parents=True,exist_ok=True);t.write_text(r'''package com.rafat.munasabati.compat
import org.junit.Assert.assertEquals
import org.junit.Test
class UserExperienceSettingsTest{
 @Test fun legacyLanguageValuesNormalize(){assertEquals("en",UserExperienceSettings.normalizeLanguage("ENGLISH"));assertEquals("en",UserExperienceSettings.normalizeLanguage("English"));assertEquals("ar",UserExperienceSettings.normalizeLanguage("ARABIC"));assertEquals("ar",UserExperienceSettings.normalizeLanguage(null))}
}
''')
t=root/'app/src/test/java/com/rafat/munasabati/calendar/CalendarFullNamesTest.kt';t.parent.mkdir(parents=True,exist_ok=True);t.write_text(r'''package com.rafat.munasabati.calendar
import org.junit.Assert.assertEquals
import org.junit.Test
class CalendarFullNamesTest{
 @Test fun fullWeekdayNames(){assertEquals(listOf("الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"),CalendarRules.weekdayNamesArabic);assertEquals(listOf("Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"),CalendarRules.weekdayNamesEnglish)}
}
''')
print('v5.0.2 patch applied')
