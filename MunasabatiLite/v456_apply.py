from pathlib import Path

MAIN=Path('app/src/main/java/com/rafat/munasabati/MainActivity.java')
s=MAIN.read_text(encoding='utf-8')

old='''Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(responsive(34,38,44)),dp(responsive(34,38,44))));pin.setOnClickListener(v->togglePinned(e));TextView date='''
new='''Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(responsive(32,36,42)),dp(responsive(34,38,44))));pin.setOnClickListener(v->togglePinned(e));Button del=iconDanger("🗑");del.setContentDescription(tr("حذف المناسبة","Delete event"));LinearLayout.LayoutParams dlp=new LinearLayout.LayoutParams(dp(responsive(32,36,42)),dp(responsive(34,38,44)));dlp.setMargins(dp(3),0,0,0);head.addView(del,dlp);del.setOnClickListener(v->confirmDelete(e));TextView date='''
if old not in s:
    raise SystemExit('v4.5.6 addCard pin/delete insertion target not found')
s=s.replace(old,new,1)

MAIN.write_text(s,encoding='utf-8')
print('Applied Munasabati v4.5.6 delete button beside event cards')
