from pathlib import Path
p=Path('v5src/app/src/main/java/com/rafat/munasabati/ui/AppUi.kt')
s=p.read_text()

# LazyListScope itself is not composable: compute translated section labels before entering it.
if 'val sectionLabels502' not in s:
    marker='    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {'
    insert='''    val sectionLabels502 = listOf(uiText("اليوم","Today"),uiText("هذا الأسبوع","This week"),uiText("هذا الشهر","This month"),uiText("متأخرة","Overdue"),uiText("مهمة","Important"))\n    val emptyEventsText502 = uiText("لا توجد مناسبات","No events")\n'''
    if marker not in s: raise SystemExit('SmartCenter LazyColumn marker missing')
    s=s.replace(marker,insert+marker,1)

s=s.replace('val sections = listOf(uiText("اليوم","Today") to b.today, uiText("هذا الأسبوع","This week") to b.week, uiText("هذا الشهر","This month") to b.month, uiText("متأخرة","Overdue") to b.overdue, uiText("مهمة","Important") to b.important)',
'''val sections = listOf(sectionLabels502[0] to b.today, sectionLabels502[1] to b.week, sectionLabels502[2] to b.month, sectionLabels502[3] to b.overdue, sectionLabels502[4] to b.important)''')
s=s.replace('EmptyMini(uiText("لا توجد مناسبات","No events"))','EmptyMini(emptyEventsText502)',1)

# Ensure refresh state exists even when prior hotfix reformatted the original line.
smart_start=s.index('@Composable fun SmartCenterScreen')
smart_end=s.index('@Composable private fun HeroCard',smart_start)
smart=s[smart_start:smart_end]
if 'refreshKey' not in smart:
    smart=smart.replace('var buckets by remember { mutableStateOf<SmartCenterBuckets?>(null) }', 'var buckets by remember { mutableStateOf<SmartCenterBuckets?>(null) }\n    var refreshKey by remember { mutableIntStateOf(0) }')
    smart=smart.replace('LaunchedEffect(Unit) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }','LaunchedEffect(refreshKey) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }')
    s=s[:smart_start]+smart+s[smart_end:]
elif 'LaunchedEffect(Unit) { buckets' in smart:
    smart=smart.replace('LaunchedEffect(Unit) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }','LaunchedEffect(refreshKey) { buckets = withContext(Dispatchers.IO) { repo(context).smartBuckets() } }')
    s=s[:smart_start]+smart+s[smart_end:]

# clickable callbacks are not composable: capture context before creating the callbacks.
more='@Composable fun MoreScreen(nav:NavHostController){'
if more in s and 'val context502=LocalContext.current' not in s[s.index(more):]:
    s=s.replace(more,more+'\n    val context502=LocalContext.current',1)
s=s.replace('com.rafat.munasabati.compat.UserExperienceSettings.play(LocalContext.current);nav.navigate("settings")','com.rafat.munasabati.compat.UserExperienceSettings.play(context502);nav.navigate("settings")')

p.write_text(s)
print('v5.0.2 compile fixes applied')
