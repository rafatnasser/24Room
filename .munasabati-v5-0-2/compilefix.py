from pathlib import Path
p=Path('v5src/app/src/main/java/com/rafat/munasabati/ui/AppUi.kt')
s=p.read_text()
s=s.replace('val appLanguage=remember{AppPreferences(LocalContext.current).language()}','val rootContext=LocalContext.current\n    val appLanguage=remember(rootContext){AppPreferences(rootContext).language()}')
s=s.replace('''@Composable fun SmartCenterScreen(nav: NavHostController) {
    val context=LocalContext.current
    var buckets by remember{mutableStateOf<SmartCenterBuckets?>(null)}''','''@Composable fun SmartCenterScreen(nav: NavHostController) {
    val context=LocalContext.current
    val english = LocalAppLanguage.current == "en"
    var buckets by remember{mutableStateOf<SmartCenterBuckets?>(null)}''')
s=s.replace('''val sections=listOf(tr("اليوم","Today") to b.today,tr("هذا الأسبوع","This week") to b.week,tr("هذا الشهر","This month") to b.month,tr("متأخرة","Overdue") to b.overdue,tr("مهمة","Important") to b.important)''','''val sections=listOf(
                (if(english) "Today" else "اليوم") to b.today,
                (if(english) "This week" else "هذا الأسبوع") to b.week,
                (if(english) "This month" else "هذا الشهر") to b.month,
                (if(english) "Overdue" else "متأخرة") to b.overdue,
                (if(english) "Important" else "مهمة") to b.important
            )''')
p.write_text(s)
print('v5.0.2 compose compile fixes applied')
