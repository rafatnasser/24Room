from pathlib import Path

def patch(path, old, new, required=True, count=1):
    p=Path(path);s=p.read_text(encoding='utf-8')
    if old not in s:
        if required: raise SystemExit(f'v4.3 finish target not found: {path}: {old[:140]!r}')
        print('skip',path,old[:70]);return
    p.write_text(s.replace(old,new,count),encoding='utf-8')

edit='app/src/main/java/com/rafat/munasabati/EditEventActivity.java'
patch(edit,'private Switch strongAlertSwitch,calendarSyncSwitch;','private Switch strongAlertSwitch,calendarSyncSwitch,secureNoteSwitch;')
patch(edit,'details.setMinLines(3);details.setGravity(Gravity.TOP);card.addView(details);', 'details.setMinLines(3);details.setGravity(Gravity.TOP);card.addView(details);secureNoteSwitch=new Switch(this);secureNoteSwitch.setText(tr("🔐 ملاحظة حساسة مشفّرة محليًا","🔐 Sensitive note encrypted on-device"));secureNoteSwitch.setTextSize(13);secureNoteSwitch.setTextColor(muted);secureNoteSwitch.setPadding(0,dp(7),0,0);card.addView(secureNoteSwitch);')
patch(edit,'title.setText(e.title);details.setText(e.details);eventTime=', 'title.setText(e.title);details.setText(SecureNotes.has(this,e.id)?SecureNotes.get(this,e.id):e.details);if(secureNoteSwitch!=null)secureNoteSwitch.setChecked(SecureNotes.has(this,e.id));eventTime=')
patch(edit,'e.details=details.getText().toString().trim();e.eventTime=', 'String noteText=details.getText().toString().trim();if(secureNoteSwitch!=null&&secureNoteSwitch.isChecked()){if(!SecureNotes.put(this,e.id,noteText)){toast(tr("تعذر تشفير الملاحظة","Could not encrypt note"));return;}e.details="";}else{SecureNotes.remove(this,e.id);e.details=noteText;}e.eventTime=')
patch(edit,'e.strongAlert=!"normal".equals(alertMode)||strongAlertSwitch.isChecked();', 'e.strongAlert="urgent".equals(alertMode)||("normal".equals(alertMode)&&strongAlertSwitch.isChecked());')
patch(edit,'V43Prefs.setAlertProfile(this,e.id,alertMode);V4EditorHooks.afterSave(this,e);', 'V43Prefs.setAlertProfile(this,e.id,alertMode);SmartReminderRules.learn(this,e.category,reminders);V4EditorHooks.afterSave(this,e);')
patch(edit,'for(int x:SmartReminderRules.values(this,code))wanted.add(x);', 'for(int x:SmartReminderRules.contextualValues(this,code,locationName!=null&&!locationName.getText().toString().trim().isEmpty()))wanted.add(x);')

main='app/src/main/java/com/rafat/munasabati/MainActivity.java'
patch(main,'V4Meta.remove(this,e.id);render();', 'V4Meta.remove(this,e.id);render();showUndoDelete(e);',required=False)
patch(main,'    private void registerCalendarObserver(){', '''    private void showUndoDelete(EventStore.Event deleted){new AlertDialog.Builder(this).setTitle(tr("تم نقل المناسبة إلى السلة","Event moved to Trash")).setMessage(tr("يمكنك التراجع الآن أو استعادتها لاحقًا خلال 30 يومًا.","Undo now or restore it from Trash within 30 days.")).setNegativeButton(tr("إغلاق","Close"),null).setPositiveButton(tr("تراجع","Undo"),(d,w)->{if(TrashStore.restore(this,0)){if(deleted.calendarSync)new Thread(()->CalendarIntegration.pushLocalChange(getApplicationContext(),deleted.id),"undo-sync").start();render();toast(tr("تمت استعادة المناسبة","Event restored"));}}).show();}

    private void registerCalendarObserver(){''')
# Long-press the main add button for immediate blank event creation.
patch(main,'add.setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));', 'add.setOnClickListener(v->startActivity(new Intent(this,TemplateChooserActivity.class)));add.setOnLongClickListener(v->{startActivity(new Intent(this,EditEventActivity.class));return true;});',required=False)

# Privacy copy makes the secure-note boundary explicit.
privacy='app/src/main/java/com/rafat/munasabati/PrivacySettingsActivity.java'
patch(privacy,'البيانات الأساسية محفوظة محليًا. الأحداث التي تفعل لها Sync فقط تُرسل إلى التقويم الخارجي المحدد.', 'البيانات الأساسية محفوظة محليًا. ويمكن داخل كل مناسبة تشفير الملاحظة الحساسة بمفتاح Android Keystore؛ الملاحظات المشفّرة لا تُرسل إلى المزامنة الخارجية.',required=False)
patch(privacy,'Core data stays local. Only events with Sync enabled are sent to the selected external calendar.', 'Core data stays local. Sensitive notes can be encrypted with Android Keystore inside each event and are excluded from external calendar sync.',required=False)

print('v4.3 final refinement patches applied')
