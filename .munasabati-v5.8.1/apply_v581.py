#!/usr/bin/env python3
from pathlib import Path
import shutil, sys, re

if len(sys.argv) != 3:
    raise SystemExit('usage: apply_v581_ci.py <source_root> <icon_webp>')
root = Path(sys.argv[1]).resolve()
icon = Path(sys.argv[2]).resolve()
if not icon.is_file():
    raise SystemExit(f'icon not found: {icon}')

app = root / 'app'
res = app / 'src/main/res'
manifest = app / 'src/main/AndroidManifest.xml'
gradle = app / 'build.gradle.kts'

g = gradle.read_text(encoding='utf-8')
g, n1 = re.subn(r'versionCode\s*=\s*140\b', 'versionCode = 141', g, count=1)
g, n2 = re.subn(r'versionName\s*=\s*"5\.8\.0"', 'versionName = "5.8.1"', g, count=1)
if n1 != 1 or n2 != 1:
    raise SystemExit('could not bump version 5.8.0/140 -> 5.8.1/141')
gradle.write_text(g, encoding='utf-8')

m = manifest.read_text(encoding='utf-8')
m, nicon = re.subn(r'android:icon="@[^"]+"', 'android:icon="@mipmap/ic_launcher"', m, count=1)
if nicon != 1:
    raise SystemExit('could not update android:icon')
if 'android:roundIcon=' in m:
    m = re.sub(r'android:roundIcon="@[^"]+"', 'android:roundIcon="@mipmap/ic_launcher_round"', m, count=1)
else:
    m = m.replace('android:icon="@mipmap/ic_launcher"', 'android:icon="@mipmap/ic_launcher"\n        android:roundIcon="@mipmap/ic_launcher_round"', 1)
manifest.write_text(m, encoding='utf-8')

for d in ['drawable-nodpi', 'drawable', 'mipmap-nodpi', 'mipmap-anydpi-v26', 'mipmap-anydpi-v33']:
    (res / d).mkdir(parents=True, exist_ok=True)

# Approved official artwork used for both legacy and adaptive color icon.
shutil.copyfile(icon, res / 'drawable-nodpi/ic_launcher_adaptive_art.webp')
shutil.copyfile(icon, res / 'mipmap-nodpi/ic_launcher.webp')
shutil.copyfile(icon, res / 'mipmap-nodpi/ic_launcher_round.webp')

(res / 'drawable/ic_launcher_background.xml').write_text('''<layer-list xmlns:android="http://schemas.android.com/apk/res/android">\n    <item><shape android:shape="rectangle"><solid android:color="#006B4F" /></shape></item>\n    <item><bitmap android:src="@drawable/ic_launcher_adaptive_art" android:gravity="fill" /></item>\n</layer-list>\n''', encoding='utf-8')

(res / 'drawable/ic_launcher_foreground.xml').write_text('''<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp" android:height="108dp"\n    android:viewportWidth="108" android:viewportHeight="108">\n    <path android:fillColor="#00000000" android:pathData="M0,0h108v108h-108z" />\n</vector>\n''', encoding='utf-8')

# Android 13+ themed icon: simplified monochrome calendar + check.
(res / 'drawable/ic_launcher_monochrome.xml').write_text('''<vector xmlns:android="http://schemas.android.com/apk/res/android"\n    android:width="108dp" android:height="108dp"\n    android:viewportWidth="108" android:viewportHeight="108">\n    <path android:fillColor="#FFFFFFFF" android:pathData="M27,25h54c4.4,0 8,3.6 8,8v47c0,4.4 -3.6,8 -8,8H27c-4.4,0 -8,-3.6 -8,-8V33c0,-4.4 3.6,-8 8,-8zM19,42h70v-9c0,-4.4 -3.6,-8 -8,-8H27c-4.4,0 -8,3.6 -8,8zM34,17h8v17h-8zM66,17h8v17h-8zM48,73l-11,-11 6,-6 5,5 18,-18 6,6z" />\n</vector>\n''', encoding='utf-8')

adaptive26 = '''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/ic_launcher_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n</adaptive-icon>\n'''
adaptive33 = '''<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n    <background android:drawable="@drawable/ic_launcher_background" />\n    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n</adaptive-icon>\n'''
for name in ['ic_launcher.xml', 'ic_launcher_round.xml']:
    (res / 'mipmap-anydpi-v26' / name).write_text(adaptive26, encoding='utf-8')
    (res / 'mipmap-anydpi-v33' / name).write_text(adaptive33, encoding='utf-8')

old = res / 'drawable/ic_launcher.xml'
if old.exists():
    old.unlink()

print('Munasabati v5.8.1 official icon applied')
