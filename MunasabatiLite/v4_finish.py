from pathlib import Path

def rep(path,a,b):
 p=Path(path);s=p.read_text(encoding='utf-8')
 if a in s:p.write_text(s.replace(a,b),encoding='utf-8')

w='app/src/main/java/com/rafat/munasabati/WidgetUpdater.java'
rep(w,'Intent home=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);','Intent home=new Intent(context,LauncherActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);')
rep(w,'Intent add=new Intent(context,EditEventActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);','Intent add=new Intent(context,LauncherActivity.class).putExtra("open_templates",true).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);')
rep(w,'Intent open=new Intent(context,EditEventActivity.class).putExtra("event_id",e.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);','Intent open=new Intent(context,LauncherActivity.class).putExtra("event_id",e.id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);')
e='app/src/main/java/com/rafat/munasabati/EditEventActivity.java'
rep(e,'صورة أو ملف PDF مرتبط بالمناسبة','صورة، PDF، Office، صوت أو ملف مرتبط بالمناسبة')
rep(e,'Image or PDF linked to this event','Image, PDF, Office, audio or other file linked to this event')
rep(e,'＋  إضافة صورة أو PDF','＋  إضافة ملف')
rep(e,'＋  Add image or PDF','＋  Add file')
print('v4 finish patches applied')
