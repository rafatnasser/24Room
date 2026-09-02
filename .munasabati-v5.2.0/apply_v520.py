from pathlib import Path
import re, shutil, sys

root = Path(sys.argv[1] if len(sys.argv) > 1 else 'v5src')
assets = Path(sys.argv[2] if len(sys.argv) > 2 else '.munasabati-v5.2.0')

build = root / 'app/build.gradle.kts'
s = build.read_text(encoding='utf-8')
s, n1 = re.subn(r'versionCode\s*=\s*70\b', 'versionCode = 80', s, count=1)
s, n2 = re.subn(r'versionName\s*=\s*"5\.1\.0"', 'versionName = "5.2.0"', s, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit(f'version patch failed: code={n1} name={n2}')
build.write_text(s, encoding='utf-8')

java = root / 'app/src/main/java/com/rafat/munasabati'
(java / 'ui').mkdir(parents=True, exist_ok=True)
(java / 'smart').mkdir(parents=True, exist_ok=True)
shutil.copy2(assets / 'V52Experience.kt', java / 'ui/V52Experience.kt')
shutil.copy2(assets / 'SmartSuggestionEngine.kt', java / 'smart/SmartSuggestionEngine.kt')

test = root / 'app/src/test/java/com/rafat/munasabati/smart'
test.mkdir(parents=True, exist_ok=True)
shutil.copy2(assets / 'SmartSuggestionEngineTest.kt', test / 'SmartSuggestionEngineTest.kt')

appui = java / 'ui/AppUi.kt'
s = appui.read_text(encoding='utf-8')
patterns = [
    (r'TodayDashboardScreen\(nav\)', 'V52DashboardScreen(nav)', 'dashboard'),
    (r'GlobalSearchScreen\(nav\)', 'V52SearchScreen(nav)', 'search'),
    (r'CalendarScreen\(nav\)', 'V52CalendarScreen(nav)', 'calendar'),
]
for pat, repl, label in patterns:
    s, n = re.subn(pat, repl, s, count=1)
    if n != 1:
        raise SystemExit(f'{label} route patch failed: {n}')
appui.write_text(s, encoding='utf-8')
print('Munasabati v5.2.0 patch applied')
