from pathlib import Path

def patch(path, old, new, required=True):
    p=Path(path); s=p.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit(f'Patch target not found: {path}: {old[:80]!r}')
        return
    p.write_text(s.replace(old,new),encoding='utf-8')

# Main screen: templates, v4 center, trash instead of permanent delete.
main='app/src/main/java/com/rafat/munasabati/MainActivity.java'
patch(main,
'Button sync=heroIcon("↔");top.addView(sync,new LinearLayout.LayoutParams(dp(46),dp(44)));sync.setOnClickListener(v->startActivity(new Intent(this,SyncCenterActivity.class)));\n        Button settings=heroIcon("⚙");',
'Button sync=heroIcon("↔");top.addView(sync,new LinearLayout.LayoutParams(dp(46),dp(44)));sync.setOnClickListener(v->startActivity(new Intent(this,SyncCenterActivity.class)));\n        Button v4=heroIcon("✦");top.addView(v4,new LinearLayout.LayoutParams(dp(46),dp(44)));v4.setOnClickListener(v->startActivity(new Intent(this,V4CenterActivity.class)));\n        Button settings=heroIcon("⚙");')
patch(main,
'add.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class)));',
'add.setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));')
patch(main,
'ReminderScheduler.cancel(this,e.id);CalendarIntegration.deleteForLocalDelete(this,e);List<EventStore.Event> all=EventStore.load(this);all.removeIf(x->x.id==e.id);EventStore.save(this,all);render();',
'TrashStore.move(this,e);ReminderScheduler.cancel(this,e.id);CalendarIntegration.deleteForLocalDelete(this,e);List<EventStore.Event> all=EventStore.load(this);all.removeIf(x->x.id==e.id);EventStore.save(this,all);V4Meta.remove(this,e.id);render();')

# Editor: 15-minute reminder, template presets, v4 metadata card, duplicate detection, expanded attachments, metadata persistence.
edit='app/src/main/java/com/rafat/munasabati/EditEventActivity.java'
patch(edit,
'private final int[] reminderValues={10080,2880,1440,180,60,30,10,0};',
'private final int[] reminderValues={10080,2880,1440,180,60,30,15,10,0};')
patch(edit,
'buildUi();loadExisting();refreshDateTime();refreshCalendarStatus();animateSections();',
'buildUi();loadExisting();if(id==0)V4EditorHooks.applyTemplate(this,category,recurrence,reminderChecks,reminderValues);refreshDateTime();refreshCalendarStatus();animateSections();')
patch(edit,
'addDetailsCard();addDateCard();addReminderCard();addSyncCard();addAttachmentCard();addLocationCard();addBottomActions();',
'addDetailsCard();addDateCard();addReminderCard();addSyncCard();addAttachmentCard();addLocationCard();V4EditorHooks.decorate(this,root,id,category,reminderChecks,reminderValues);addBottomActions();')
patch(edit,
'String n=title.getText().toString().trim();if(n.isEmpty()){title.setError(tr("اكتب اسم المناسبة","Enter the event name"));title.requestFocus();return;}\n        ArrayList<Integer> reminders=',
'String n=title.getText().toString().trim();if(n.isEmpty()){title.setError(tr("اكتب اسم المناسبة","Enter the event name"));title.requestFocus();return;}if(id==0&&V4EditorHooks.duplicate(this,id,n,eventTime)){new AlertDialog.Builder(this).setTitle(tr("قد تكون المناسبة موجودة بالفعل","Possible duplicate event")).setMessage(tr("وجدت مناسبة بنفس الاسم وفي وقت قريب. غيّر الاسم أو الوقت إذا كنت تريد إنشاء مناسبة مختلفة.","An event with the same name exists at a nearby time. Change the name or time to create a separate event.")).setPositiveButton("OK",null).show();return;}\n        ArrayList<Integer> reminders=')
patch(edit,
'all.removeIf(x->x.id==finalId);all.add(e);EventStore.save(this,all);ReminderScheduler.schedule(this,e);',
'all.removeIf(x->x.id==finalId);all.add(e);EventStore.save(this,all);V4EditorHooks.afterSave(this,e);ReminderScheduler.schedule(this,e);')
patch(edit,
'i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});',
'i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf","application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document","application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation","text/plain","audio/*"});')
patch(edit,
'if(r==PICK_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}attachmentUri=u.toString();attachmentType=getContentResolver().getType(u);attachmentName=queryName(u);attachBtn.setText("📎  "+attachmentName);}}',
'if(r==PICK_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}attachmentUri=u.toString();attachmentType=getContentResolver().getType(u);attachmentName=queryName(u);attachBtn.setText("📎  "+attachmentName);}V4EditorHooks.onActivityResult(this,r,c,data);}')
patch(edit,
'if(m==30)return tr("قبل 30 دقيقة","30 min before");if(m==60)',
'if(m==30)return tr("قبل 30 دقيقة","30 min before");if(m==15)return tr("قبل 15 دقيقة","15 min before");if(m==60)')

# Refined WidgetUpdater already owns privacy + extra-widget refresh. Keep old patches optional.
widget='app/src/main/java/com/rafat/munasabati/WidgetUpdater.java'
patch(widget,
'for(int id:ids)update(context,manager,id);\n    }',
'for(int id:ids)update(context,manager,id);context.getSharedPreferences("munasabati_v4",0).edit().putLong("widget_last_update",System.currentTimeMillis()).apply();refreshExtras(context,manager);\n    }',required=False)
patch(widget,
'    private static int rowsForSize(Bundle options){',
'    private static void refreshExtras(Context c,AppWidgetManager m){int[] a=m.getAppWidgetIds(new ComponentName(c,MiniWidgetProvider.class));for(int id:a)ExtraWidgetUpdater.mini(c,m,id);int[] b=m.getAppWidgetIds(new ComponentName(c,CountdownWidgetProvider.class));for(int id:b)ExtraWidgetUpdater.countdown(c,m,id);int[] d=m.getAppWidgetIds(new ComponentName(c,MonthWidgetProvider.class));for(int id:d)ExtraWidgetUpdater.month(c,m,id);}\n\n    private static int rowsForSize(Bundle options){',required=False)

# Boot: restart location and built-in Ahl al-Bayt reminder checks too.
boot='app/src/main/java/com/rafat/munasabati/BootReceiver.java'
p=Path(boot);s=p.read_text(encoding='utf-8')
if 'LocationReminderScheduler.schedule' not in s:s=s.replace('AutoBackupScheduler.schedule(context);','AutoBackupScheduler.schedule(context);LocationReminderScheduler.schedule(context);')
if 'AhlBaytReminderScheduler.schedule' not in s:s=s.replace('AutoBackupScheduler.schedule(context);','AutoBackupScheduler.schedule(context);AhlBaytReminderScheduler.schedule(context);')
p.write_text(s,encoding='utf-8')

print('v4 patches applied')
