from pathlib import Path
import sys
root=Path(sys.argv[1])

# version
p=root/'app/build.gradle.kts'
s=p.read_text()
s=s.replace('versionCode = 62','versionCode = 63').replace('versionName = "5.0.2"','versionName = "5.0.3"')
p.write_text(s)

# preferences: UI sound option + short feedback
p=root/'app/src/main/java/com/rafat/munasabati/compat/AppPreferences.kt'
s=p.read_text()
if 'import android.media.AudioManager' not in s:
    s=s.replace('import android.media.RingtoneManager\n','import android.media.RingtoneManager\nimport android.media.AudioManager\nimport android.media.ToneGenerator\nimport android.os.Handler\nimport android.os.Looper\n')
if 'fun uiSoundEnabled()' not in s:
    s=s.replace('    fun hapticEnabled(): Boolean = bool("haptic_enabled", true)\n', '''    fun uiSoundEnabled(): Boolean = bool("ui_sound_enabled", true)\n    fun setUiSoundEnabled(value: Boolean) { prefs.edit().putBoolean("ui_sound_enabled", value).apply() }\n\n    fun hapticEnabled(): Boolean = bool("haptic_enabled", true)\n''')
if 'fun uiClick()' not in s:
    s=s.replace('    fun previewFeedback() {\n', '''    fun uiClick() {\n        if (uiSoundEnabled()) {\n            runCatching {\n                val audio = context.getSystemService(AudioManager::class.java)\n                if (audio.ringerMode != AudioManager.RINGER_MODE_SILENT) {\n                    val tone = ToneGenerator(AudioManager.STREAM_SYSTEM, 70)\n                    tone.startTone(ToneGenerator.TONE_PROP_ACK, 55)\n                    Handler(Looper.getMainLooper()).postDelayed({ runCatching { tone.release() } }, 120)\n                }\n            }\n        }\n        if (hapticEnabled()) vibrate(longArrayOf(0, 24))\n    }\n\n    fun previewFeedback() {\n''')
    s=s.replace('    fun previewFeedback() {\n        if (soundEnabled()) {','    fun previewFeedback() {\n        uiClick()\n        if (soundEnabled()) {')
p.write_text(s)

# notification channels fresh IDs, ensuring user device gets sound back
p=root/'app/src/main/java/com/rafat/munasabati/reminder/ReminderEngine.kt'
s=p.read_text()
s=s.replace('munasabati_events_sound_v502','munasabati_events_sound_v503')
s=s.replace('munasabati_events_silent_v502','munasabati_events_silent_v503')
s=s.replace('munasabati_strong_v502','munasabati_strong_v503')
p.write_text(s)

# create channels at app startup
p=root/'app/src/main/java/com/rafat/munasabati/MunasabatiApp.kt'
s=p.read_text()
if 'import com.rafat.munasabati.reminder.ReminderNotifier' not in s:
    s=s.replace('import com.rafat.munasabati.compat.AhlBaytCalendar\n','import com.rafat.munasabati.compat.AhlBaytCalendar\nimport com.rafat.munasabati.reminder.ReminderNotifier\n')
if 'ReminderNotifier.ensureChannels(this)' not in s:
    s=s.replace('        repository = EventRepository(MunasabatiDatabase(this))\n','        repository = EventRepository(MunasabatiDatabase(this))\n        runCatching { ReminderNotifier.ensureChannels(this) }\n')
p.write_text(s)

# Ahl al-Bayt notifications use the same sound preference/channels
p=root/'app/src/main/java/com/rafat/munasabati/compat/AhlBaytCalendar.kt'
s=p.read_text()
old='class AhlBaytReminderReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val list=AhlBaytCalendar.todayOccasions();if(list.isNotEmpty()){val mgr=context.getSystemService(android.app.NotificationManager::class.java);if(Build.VERSION.SDK_INT>=26)mgr.createNotificationChannel(android.app.NotificationChannel("ahlbayt_occasions_v1","مناسبات أهل البيت",android.app.NotificationManager.IMPORTANCE_HIGH));val open=PendingIntent.getActivity(context,554,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val text=list.joinToString(" • "){it.title};NotificationManagerCompat.from(context).notify(554,NotificationCompat.Builder(context,"ahlbayt_occasions_v1").setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("مناسبة اليوم").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())};AhlBaytCalendar.scheduleNext(context)}}'
new='''class AhlBaytReminderReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){val list=AhlBaytCalendar.todayOccasions();if(list.isNotEmpty()){ReminderNotifier.ensureChannels(context);val channel=if(AppPreferences(context).soundEnabled()) ReminderNotifier.CHANNEL_SOUND else ReminderNotifier.CHANNEL_SILENT;val open=PendingIntent.getActivity(context,554,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE);val text=list.joinToString(" • "){it.title};NotificationManagerCompat.from(context).notify(554,NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle("مناسبة اليوم").setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(open).setAutoCancel(true).build())};AhlBaytCalendar.scheduleNext(context)}}'''
if old in s: s=s.replace(old,new)
p.write_text(s)

# trash: never let linked-calendar cleanup stop local delete, verify deletion, keep saved trash copy
p=root/'app/src/main/java/com/rafat/munasabati/compat/LegacyDataTools.kt'
s=p.read_text()
old='fun moveToTrash(e:EventModel):Boolean{val a=items();a.put(EventJsonCodec.toJson(e).put("deletedAt",System.currentTimeMillis()));val saved=prefs.edit().putString("items",a.toString()).commit();runCatching{CalendarSyncManager(context,repo).deleteLinked(e)};runCatching{ReminderScheduler(context).cancel(e.id)};val deleted=repo.deleteEvent(e.id)>0;return saved&&deleted}'
new='fun moveToTrash(e:EventModel):Boolean{val a=items();a.put(EventJsonCodec.toJson(e).put("deletedAt",System.currentTimeMillis()));val saved=prefs.edit().putString("items",a.toString()).commit();if(!saved)return false;runCatching{ReminderScheduler(context).cancel(e.id)};val deleted=runCatching{repo.deleteEvent(e.id)}.getOrDefault(0);val gone=runCatching{repo.eventById(e.id)==null}.getOrDefault(false);if(deleted>0||gone)runCatching{CalendarSyncManager(context,repo).deleteLinked(e)};return deleted>0||gone}'
if old not in s:
    raise SystemExit('TrashManager pattern not found')
s=s.replace(old,new)
p.write_text(s)

# UI: use UI sound on key actions; make credits visible immediately in More; expose UI sound setting
p=root/'app/src/main/java/com/rafat/munasabati/ui/AppUi.kt'
s=p.read_text()
if 'private inline fun uiTap' not in s:
    s=s.replace('private fun repo(context: Context) = (context.applicationContext as MunasabatiApp).repository\n','private fun repo(context: Context) = (context.applicationContext as MunasabatiApp).repository\nprivate inline fun uiTap(context: Context, action: () -> Unit) { AppPreferences(context).uiClick(); action() }\n')
s=s.replace('floatingActionButton = { FloatingActionButton(onClick = { nav.navigate("add") }) { Icon(Icons.Default.Add, null) } }','floatingActionButton = { FloatingActionButton(onClick = { uiTap(context) { nav.navigate("add") } }) { Icon(Icons.Default.Add, null) } }')
s=s.replace('@Composable private fun BottomBar(nav: NavHostController) {\n    val entry by nav.currentBackStackEntryAsState()','@Composable private fun BottomBar(nav: NavHostController) {\n    val context = LocalContext.current\n    val entry by nav.currentBackStackEntryAsState()')
s=s.replace('onClick = { nav.navigate(target) { launchSingleTop = true; restoreState = true; popUpTo("center") { saveState = true } } },','onClick = { uiTap(context) { nav.navigate(target) { launchSingleTop = true; restoreState = true; popUpTo("center") { saveState = true } } } },')
s=s.replace('item { AssistChip(onClick = { confirmDelete = true }, label = { Text(tr("حذف", "Delete")) }, leadingIcon = { Icon(Icons.Default.Delete, null) }) }','item { AssistChip(onClick = { uiTap(context) { confirmDelete = true } }, label = { Text(tr("حذف", "Delete")) }, leadingIcon = { Icon(Icons.Default.Delete, null) }) }')
s=s.replace('Button(onClick = {\n                    val ok = TrashManager(context, repo(context)).moveToTrash(e)','Button(onClick = {\n                    AppPreferences(context).uiClick()\n                    val ok = TrashManager(context, repo(context)).moveToTrash(e)')
s=s.replace('dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("إلغاء", "Cancel")) } }','dismissButton = { TextButton(onClick = { AppPreferences(context).uiClick(); confirmDelete = false }) { Text(tr("إلغاء", "Cancel")) } }')
s=s.replace('@Composable fun MoreScreen(nav: NavHostController) {\n    LazyColumn','@Composable fun MoreScreen(nav: NavHostController) {\n    val context = LocalContext.current\n    LazyColumn')
for target in ('preferences','stats','legacy','diagnostics'):
    s=s.replace(f'modifier = Modifier.clickable {{ nav.navigate("{target}") }}',f'modifier = Modifier.clickable {{ uiTap(context) {{ nav.navigate("{target}") }} }}')
credit_line='        item { CreditsCard() }\n'
s=s.replace(credit_line,'',1)
settings_line='        item { ListItem(headlineContent = { Text(tr("الإعدادات واللغة والصوت", "Settings, language & sound")) }, supportingContent = { Text(tr("العربية / English • الصوت • الاهتزاز", "Arabic / English • sound • vibration")) }, leadingContent = { Icon(Icons.Default.Settings, null) }, modifier = Modifier.clickable { uiTap(context) { nav.navigate("preferences") } }) }\n'
if settings_line in s:
    s=s.replace(settings_line,settings_line+credit_line,1)
s=s.replace('    var sound by remember { mutableStateOf(prefs.soundEnabled()) }\n    var haptic','    var sound by remember { mutableStateOf(prefs.soundEnabled()) }\n    var uiSound by remember { mutableStateOf(prefs.uiSoundEnabled()) }\n    var haptic')
s=s.replace('onClick = { language = "ar"; prefs.setLanguage("ar"); AppI18n.language = "ar"; onLanguageChanged("ar") }','onClick = { prefs.uiClick(); language = "ar"; prefs.setLanguage("ar"); AppI18n.language = "ar"; onLanguageChanged("ar") }')
s=s.replace('onClick = { language = "en"; prefs.setLanguage("en"); AppI18n.language = "en"; onLanguageChanged("en") }','onClick = { prefs.uiClick(); language = "en"; prefs.setLanguage("en"); AppI18n.language = "en"; onLanguageChanged("en") }')
marker='        Text(tr("الصوت واللمس والحركة", "Sound, touch & motion"), fontSize = 21.sp, fontWeight = FontWeight.Bold)\n'
if marker in s and 'Interface & icon sounds' not in s:
    s=s.replace(marker,marker+'        Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(tr("أصوات الأيقونات والواجهة", "Interface & icon sounds"), fontWeight = FontWeight.Medium); Text(tr("نغمة قصيرة عند الضغط على الأيقونات الرئيسية", "Short click sound for main icons"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }; Switch(uiSound, { uiSound = it; prefs.setUiSoundEnabled(it); if (it) prefs.uiClick() }) }\n')
s=s.replace('enabled = sound || haptic','enabled = sound || uiSound || haptic')
p.write_text(s)

p=root/'app/src/androidTest/java/com/rafat/munasabati/FeatureRegressionInstrumentedTest.kt'
s=p.read_text()
if 'p.setUiSoundEnabled(true)' not in s:
    s=s.replace('p.setSoundEnabled(true); assertTrue(p.soundEnabled())\n','p.setSoundEnabled(true); assertTrue(p.soundEnabled())\n        p.setUiSoundEnabled(true); assertTrue(p.uiSoundEnabled()); p.uiClick()\n')
p.write_text(s)

print('patched', root)
