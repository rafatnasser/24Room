package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.*;

public class EditEventActivity extends Activity {
    private static final int PICK_FILE=41,LOC=42,CALENDAR=43;
    private EditText title,details,locationName,locationUrl;
    private Button dateBtn,timeBtn,attachBtn;
    private TextView hijriPreview,calendarStatus;
    private Spinner category,recurrence,colorSpinner;
    private CheckBox favoriteBox,pinnedBox;
    private Switch strongAlertSwitch,calendarSyncSwitch;
    private CheckBox[] reminderChecks;
    private final int[] reminderValues={10080,2880,1440,180,60,30,10,0};
    private long eventTime,id;
    private int lastCategoryPos=0;
    private boolean loadingExisting=false;
    private String attachmentUri="",attachmentName="",attachmentType="";
    private LinearLayout root;
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115),ink=Color.rgb(31,42,55);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(primary);eventTime=System.currentTimeMillis()+3600000L;id=getIntent().getLongExtra("event_id",0L);buildUi();loadExisting();refreshDateTime();refreshCalendarStatus();animateSections();
    }
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(15),dp(18),dp(17));hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=iconButton("‹");top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(42)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView head=label(id==0?tr("مناسبة جديدة","New event"):tr("تعديل المناسبة","Edit event"),27,true);head.setTextColor(Color.WHITE);titles.addView(head);
        TextView sub=label(id==0?tr("أنشئ مناسبة مرتبة بكل تفاصيلها","Create a polished event with all its details"):tr("حدّث التفاصيل ثم احفظ التعديلات","Update the details and save your changes"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        TextView badge=label(tr("✦ تنظيم • تنبيه • مزامنة","✦ Organize • Alert • Sync"),11,true);badge.setTextColor(Color.rgb(255,241,196));badge.setPadding(0,dp(9),0,0);hero.addView(badge);

        addDetailsCard();addDateCard();addReminderCard();addSyncCard();addAttachmentCard();addLocationCard();addBottomActions();
        setContentView(scroll);
    }

    private void addDetailsCard(){
        LinearLayout card=section(tr("تفاصيل المناسبة","Event details"),"✦",tr("الاسم والفئة والوصف والمظهر","Name, category, description and appearance"));
        card.addView(fieldLabel(tr("اسم المناسبة","Event name")));title=input(tr("مثال: عيد ميلاد أحمد","Example: Ahmed's birthday"));title.setSingleLine(true);card.addView(title,new LinearLayout.LayoutParams(-1,dp(56)));
        card.addView(fieldLabel(tr("الفئة","Category")));category=new Spinner(this);category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));card.addView(category,new LinearLayout.LayoutParams(-1,dp(52)));
        card.addView(fieldLabel(tr("الملاحظات","Notes")));details=input(tr("أضف تفاصيل أو ملاحظات للمناسبة","Add details or notes"));details.setMinLines(3);details.setGravity(Gravity.TOP);card.addView(details);
        card.addView(fieldLabel(tr("لون المناسبة","Event color")));colorSpinner=new Spinner(this);colorSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ColorPalette.labels(this,true)));card.addView(colorSpinner,new LinearLayout.LayoutParams(-1,dp(52)));

        LinearLayout flags=new LinearLayout(this);flags.setPadding(0,dp(7),0,0);card.addView(flags);
        favoriteBox=flagBox(tr("⭐ مفضلة","⭐ Favorite"));pinnedBox=flagBox(tr("📌 تثبيت بالأعلى","📌 Pin to top"));flags.addView(favoriteBox,new LinearLayout.LayoutParams(0,dp(50),1));LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(0,dp(50),1);fp.setMargins(dp(7),0,0,0);flags.addView(pinnedBox,fp);
        category.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long rowId){if(pos!=lastCategoryPos){lastCategoryPos=pos;String auto=Recurrence.autoForCategory(Categories.CODES[pos]);if(!Recurrence.NONE.equals(auto)&&recurrence!=null)recurrence.setSelection(Recurrence.indexOf(auto));}}public void onNothingSelected(AdapterView<?> p){}});
    }

    private void addDateCard(){
        LinearLayout card=section(tr("التاريخ والتكرار","Date & recurrence"),"◷",tr("الميلادي والهجري مع التكرار التلقائي","Gregorian, Hijri and recurrence"));
        LinearLayout row=new LinearLayout(this);card.addView(row);dateBtn=outlineButton("");timeBtn=outlineButton("");row.addView(dateBtn,new LinearLayout.LayoutParams(0,dp(56),1));LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,dp(56),1);tp.setMargins(dp(7),0,0,0);row.addView(timeBtn,tp);dateBtn.setOnClickListener(v->pickDate());timeBtn.setOnClickListener(v->pickTime());
        hijriPreview=label("",14,true);hijriPreview.setTextColor(accent);hijriPreview.setGravity(Gravity.CENTER_VERTICAL);hijriPreview.setPadding(dp(13),dp(10),dp(13),dp(10));hijriPreview.setBackground(round(Color.rgb(255,249,235),14));card.addView(hijriPreview,margin(-1,-2,0,dp(8),0,0));
        card.addView(fieldLabel(tr("تكرار المناسبة","Repeat event")));recurrence=new Spinner(this);recurrence.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Recurrence.labels(this)));card.addView(recurrence,new LinearLayout.LayoutParams(-1,dp(52)));
        TextView hint=label(tr("عيد الميلاد وعيد الزواج والسنوية = سنوي تلقائيًا، والعادة الأسبوعية = أسبوعي.","Birthday, wedding anniversary and Anniversary default to yearly; Weekly habit defaults to weekly."),12,false);hint.setTextColor(muted);hint.setPadding(0,dp(5),0,0);card.addView(hint);
    }

    private void addReminderCard(){
        LinearLayout card=section(tr("التنبيهات","Reminders"),"🔔",tr("اختر أكثر من تنبيه للمناسبة نفسها","Choose multiple reminders for the same event"));
        reminderChecks=new CheckBox[reminderValues.length];GridLayout grid=new GridLayout(this);grid.setColumnCount(2);card.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        for(int i=0;i<reminderValues.length;i++){CheckBox cb=new CheckBox(this);cb.setText(reminderLabel(reminderValues[i]));cb.setTextSize(13);cb.setTextColor(ink);cb.setPadding(dp(8),dp(5),dp(8),dp(5));cb.setBackground(round(Color.rgb(248,250,251),12));reminderChecks[i]=cb;if(reminderValues[i]==30)cb.setChecked(true);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(3),dp(3),dp(3));grid.addView(cb,p);}

        LinearLayout strong=new LinearLayout(this);strong.setOrientation(LinearLayout.VERTICAL);strong.setPadding(dp(13),dp(11),dp(13),dp(11));strong.setBackground(round(Color.rgb(255,248,229),15));card.addView(strong,margin(-1,-2,0,dp(10),0,0));
        strongAlertSwitch=new Switch(this);strongAlertSwitch.setText(tr("🔔 تنبيه قوي","🔔 Strong Alert"));strongAlertSwitch.setTextSize(15);strongAlertSwitch.setTypeface(null,Typeface.BOLD);strongAlertSwitch.setTextColor(Color.rgb(116,78,12));strong.addView(strongAlertSwitch);
        TextView sh=label(tr("صوت منبّه متكرر، اهتزاز وواجهة فوق شاشة القفل للمناسبات المهمة جدًا.","Repeating alarm sound, vibration and lock-screen full-screen alert for very important events."),12,false);sh.setTextColor(Color.rgb(130,99,42));strong.addView(sh);
        strongAlertSwitch.setOnCheckedChangeListener((button,checked)->{if(checked&&!loadingExisting)requestStrongAlertPermissionIfNeeded();});
    }

    private void addSyncCard(){
        LinearLayout card=section(tr("المزامنة مع التقويم","Calendar sync"),"↔",tr("تحكم في مزامنة هذه المناسبة بشكل مستقل","Control synchronization for this event independently"));
        LinearLayout syncBox=new LinearLayout(this);syncBox.setOrientation(LinearLayout.VERTICAL);syncBox.setPadding(dp(13),dp(11),dp(13),dp(11));syncBox.setBackground(round(Color.rgb(237,247,245),15));card.addView(syncBox);
        calendarSyncSwitch=new Switch(this);calendarSyncSwitch.setText(tr("مزامنة هذه المناسبة فقط","Sync this event"));calendarSyncSwitch.setTextSize(15);calendarSyncSwitch.setTypeface(null,Typeface.BOLD);calendarSyncSwitch.setTextColor(primary);syncBox.addView(calendarSyncSwitch);
        calendarStatus=label("",12,false);calendarStatus.setTextColor(muted);calendarStatus.setPadding(0,dp(4),0,0);syncBox.addView(calendarStatus);
        Button center=outlineButton(tr("فتح مركز المزامنة","Open Sync Center"));card.addView(center,margin(-1,dp(48),0,dp(8),0,0));center.setOnClickListener(v->startActivity(new Intent(this,SyncCenterActivity.class)));
        calendarSyncSwitch.setOnCheckedChangeListener((button,checked)->{if(checked&&!loadingExisting)ensureCalendarReady();refreshCalendarStatus();});
    }

    private void addAttachmentCard(){
        LinearLayout card=section(tr("المرفقات","Attachments"),"📎",tr("صورة أو ملف PDF مرتبط بالمناسبة","Image or PDF linked to this event"));
        attachBtn=outlineButton(tr("＋  إضافة صورة أو PDF","＋  Add image or PDF"));card.addView(attachBtn,new LinearLayout.LayoutParams(-1,dp(52)));attachBtn.setOnClickListener(v->pickFile());
        Button remove=linkButton(tr("إزالة المرفق الحالي","Remove current attachment"));card.addView(remove);remove.setOnClickListener(v->{attachmentUri="";attachmentName="";attachmentType="";attachBtn.setText(tr("＋  إضافة صورة أو PDF","＋  Add image or PDF"));});
    }

    private void addLocationCard(){
        LinearLayout card=section(tr("موقع المناسبة","Event location"),"⌖",tr("اسم المكان أو رابط Google Maps","Place name or Google Maps link"));
        card.addView(fieldLabel(tr("اسم المكان","Place name")));locationName=input(tr("مثال: الخبر","Example: Khobar"));locationName.setSingleLine(true);card.addView(locationName,new LinearLayout.LayoutParams(-1,dp(54)));
        card.addView(fieldLabel(tr("رابط الموقع","Location link")));locationUrl=input(tr("https://maps.google.com/...","https://maps.google.com/..."));locationUrl.setSingleLine(true);card.addView(locationUrl,new LinearLayout.LayoutParams(-1,dp(54)));
        Button current=outlineButton(tr("📍  استخدام موقعي الحالي","📍  Use my current location"));card.addView(current,margin(-1,dp(50),0,dp(8),0,0));current.setOnClickListener(v->useLocation());
    }

    private void addBottomActions(){
        LinearLayout actions=new LinearLayout(this);actions.setPadding(0,dp(2),0,0);root.addView(actions,new LinearLayout.LayoutParams(-1,-2));
        Button cancel=outlineButton(tr("إلغاء","Cancel"));actions.addView(cancel,new LinearLayout.LayoutParams(0,dp(58),1));cancel.setOnClickListener(v->finish());
        Button save=premiumButton(id==0?tr("✓  حفظ المناسبة","✓  Save event"):tr("✓  حفظ التعديلات","✓  Save changes"));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(0,dp(58),2);sp.setMargins(dp(8),0,0,0);actions.addView(save,sp);save.setOnClickListener(v->save());
    }

    private void loadExisting(){
        if(id==0){calendarSyncSwitch.setChecked(CalendarIntegration.defaultSyncForNewEvent(this));return;}EventStore.Event e=EventStore.find(this,id);if(e==null)return;loadingExisting=true;
        title.setText(e.title);details.setText(e.details);eventTime=Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,System.currentTimeMillis()):e.eventTime;lastCategoryPos=Categories.indexOf(e.category);category.setSelection(lastCategoryPos);recurrence.setSelection(Recurrence.indexOf(e.recurrence));
        if(e.color==null||e.color.isEmpty())colorSpinner.setSelection(0);else colorSpinner.setSelection(ColorPalette.indexOf(e.color)+1);favoriteBox.setChecked(e.favorite);pinnedBox.setChecked(e.pinned);strongAlertSwitch.setChecked(e.strongAlert);calendarSyncSwitch.setChecked(e.calendarSync);
        Set<Integer> active=new HashSet<>(EventStore.reminders(e));for(int i=0;i<reminderValues.length;i++)reminderChecks[i].setChecked(active.contains(reminderValues[i]));attachmentUri=e.attachmentUri;attachmentName=e.attachmentName;attachmentType=e.attachmentType;attachBtn.setText(attachmentName.isEmpty()?tr("＋  إضافة صورة أو PDF","＋  Add image or PDF"):"📎  "+attachmentName);locationName.setText(e.locationName);locationUrl.setText(e.locationUrl);loadingExisting=false;refreshCalendarStatus();
    }

    private void save(){
        String n=title.getText().toString().trim();if(n.isEmpty()){title.setError(tr("اكتب اسم المناسبة","Enter the event name"));title.requestFocus();return;}
        ArrayList<Integer> reminders=new ArrayList<>();for(int i=0;i<reminderValues.length;i++)if(reminderChecks[i].isChecked())reminders.add(reminderValues[i]);if(reminders.isEmpty()){toast(tr("اختر تنبيهًا واحدًا على الأقل","Select at least one reminder"));return;}
        List<EventStore.Event> all=new ArrayList<>(EventStore.load(this));EventStore.Event e=id==0?new EventStore.Event():EventStore.find(this,id);if(e==null)e=new EventStore.Event();if(id==0)e.id=System.currentTimeMillis();else ReminderScheduler.cancel(this,id);
        e.title=n;e.category=Categories.CODES[category.getSelectedItemPosition()];e.recurrence=Recurrence.CODES[recurrence.getSelectedItemPosition()];e.color=colorSpinner.getSelectedItemPosition()==0?"":ColorPalette.HEX[colorSpinner.getSelectedItemPosition()-1];e.favorite=favoriteBox.isChecked();e.pinned=pinnedBox.isChecked();e.strongAlert=strongAlertSwitch.isChecked();e.calendarSync=calendarSyncSwitch.isChecked();
        e.details=details.getText().toString().trim();e.eventTime=eventTime;e.remindersCsv=EventStore.remindersCsv(reminders);e.reminderMinutes=reminders.get(0);e.attachmentUri=attachmentUri;e.attachmentName=attachmentName;e.attachmentType=attachmentType==null?"":attachmentType;e.locationName=locationName.getText().toString().trim();e.locationUrl=locationUrl.getText().toString().trim();e.updatedAt=System.currentTimeMillis();
        long finalId=e.id;all.removeIf(x->x.id==finalId);all.add(e);EventStore.save(this,all);ReminderScheduler.schedule(this,e);if(e.strongAlert)StrongAlertSupport.ensureChannel(this);if(e.calendarSync){CalendarSyncScheduler.schedule(this);final long syncId=e.id;new Thread(()->CalendarIntegration.pushLocalChange(getApplicationContext(),syncId),"event-calendar-push").start();}
        toast(id==0?tr("تم حفظ المناسبة","Event saved"):tr("تم حفظ التعديلات","Changes saved"));finish();
    }

    private void ensureCalendarReady(){
        if(!CalendarIntegration.hasPermission(this)){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},CALENDAR);return;}
        if(CalendarIntegration.selectedCalendarId(this)<=0)new AlertDialog.Builder(this).setTitle(tr("اختر تقويمًا للمزامنة","Choose a calendar")).setMessage(tr("قبل مزامنة هذه المناسبة اختر Google Calendar أو Outlook من إعدادات المزامنة.","Choose a Google Calendar or Outlook calendar in synchronization settings before syncing this event.")).setNegativeButton(tr("لاحقًا","Later"),null).setPositiveButton(tr("فتح الإعدادات","Open settings"),(d,w)->startActivity(new Intent(this,SettingsActivity.class))).show();
    }
    private void refreshCalendarStatus(){if(calendarStatus==null)return;String label=CalendarIntegration.selectedCalendarLabel(this);if(!CalendarIntegration.hasPermission(this))calendarStatus.setText(tr("يلزم السماح بالوصول إلى التقويم.","Calendar access permission is required."));else if(label.isEmpty())calendarStatus.setText(tr("لم يتم اختيار تقويم افتراضي بعد.","No default calendar selected yet."));else calendarStatus.setText(tr("سيتم الربط مع: ","Will sync with: ")+label);}
    private void requestStrongAlertPermissionIfNeeded(){StrongAlertSupport.ensureChannel(this);if(Build.VERSION.SDK_INT>=34&&!StrongAlertSupport.canUseFullScreen(this))new AlertDialog.Builder(this).setTitle(tr("السماح بالتنبيه القوي","Allow Strong Alert")).setMessage(tr("لإظهار التنبيه فوق شاشة القفل اسمح لتطبيق مناسبـاتي باستخدام التنبيهات بملء الشاشة.","Allow Munasabati to use full-screen notifications to show Strong Alerts over the lock screen.")).setNegativeButton(tr("لاحقًا","Later"),null).setPositiveButton(tr("فتح الإعدادات","Open settings"),(d,w)->StrongAlertSupport.openFullScreenSettings(this)).show();}

    private void pickDate(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(y,m,d);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private void pickTime(){Calendar c=Calendar.getInstance();c.setTimeInMillis(eventTime);new TimePickerDialog(this,(v,h,m)->{Calendar n=Calendar.getInstance();n.setTimeInMillis(eventTime);n.set(Calendar.HOUR_OF_DAY,h);n.set(Calendar.MINUTE,m);n.set(Calendar.SECOND,0);n.set(Calendar.MILLISECOND,0);eventTime=n.getTimeInMillis();refreshDateTime();},c.get(Calendar.HOUR_OF_DAY),c.get(Calendar.MINUTE),false).show();}
    private void refreshDateTime(){if(dateBtn==null)return;dateBtn.setText("📅  "+DateTools.gregorianShort(this,eventTime));timeBtn.setText("🕒  "+DateTools.time(this,eventTime));int o=AppSettings.hijriOffset(this);String suffix=o==0?tr("بدون تعديل","no adjustment"):(o>0?"+"+o:String.valueOf(o))+tr(" يوم"," day");hijriPreview.setText("☾  "+DateTools.hijri(this,eventTime)+"  •  "+suffix);}
    private String reminderLabel(int m){if(m==0)return tr("وقت المناسبة","At event time");if(m==10)return tr("قبل 10 دقائق","10 min before");if(m==30)return tr("قبل 30 دقيقة","30 min before");if(m==60)return tr("قبل ساعة","1 hour before");if(m==180)return tr("قبل 3 ساعات","3 hours before");if(m==1440)return tr("قبل يوم","1 day before");if(m==2880)return tr("قبل يومين","2 days before");if(m==10080)return tr("قبل أسبوع","1 week before");return m+tr(" دقيقة قبل"," min before");}

    private void pickFile(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"image/*","application/pdf"});startActivityForResult(i,PICK_FILE);}
    @Override protected void onActivityResult(int r,int c,Intent data){super.onActivityResult(r,c,data);if(r==PICK_FILE&&c==RESULT_OK&&data!=null&&data.getData()!=null){Uri u=data.getData();try{getContentResolver().takePersistableUriPermission(u,Intent.FLAG_GRANT_READ_URI_PERMISSION);}catch(Exception ignored){}attachmentUri=u.toString();attachmentType=getContentResolver().getType(u);attachmentName=queryName(u);attachBtn.setText("📎  "+attachmentName);}}
    private String queryName(Uri u){try(Cursor c=getContentResolver().query(u,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(c!=null&&c.moveToFirst())return c.getString(0);}return tr("مرفق","Attachment");}

    private void useLocation(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED&&checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},LOC);return;}fillLocation();}
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] g){super.onRequestPermissionsResult(r,p,g);if(r==LOC&&g.length>0&&g[0]==PackageManager.PERMISSION_GRANTED)fillLocation();else if(r==CALENDAR){refreshCalendarStatus();if(CalendarIntegration.hasPermission(this)){CalendarIntegration.setEnabled(this,true);CalendarSyncScheduler.schedule(this);}else calendarSyncSwitch.setChecked(false);}}
    @SuppressWarnings("MissingPermission") private void fillLocation(){LocationManager lm=(LocationManager)getSystemService(LOCATION_SERVICE);Location best=null;for(String p:new String[]{LocationManager.GPS_PROVIDER,LocationManager.NETWORK_PROVIDER}){try{Location x=lm.getLastKnownLocation(p);if(x!=null&&(best==null||x.getTime()>best.getTime()))best=x;}catch(Exception ignored){}}if(best==null){toast(tr("تعذر تحديد الموقع الحالي. فعّل الموقع وحاول مرة أخرى.","Could not determine your current location. Enable location and try again."));return;}if(locationName.getText().toString().trim().isEmpty())locationName.setText(tr("الموقع الحالي","Current location"));locationUrl.setText("geo:"+best.getLatitude()+","+best.getLongitude()+"?q="+best.getLatitude()+","+best.getLongitude());}

    private LinearLayout section(String title,String icon,String subtitle){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(15));c.setBackground(round(Color.WHITE,21));root.addView(c,margin(-1,-2,0,0,0,dp(10)));LinearLayout h=new LinearLayout(this);h.setGravity(Gravity.CENTER_VERTICAL);c.addView(h);TextView i=label(icon,20,true);i.setGravity(Gravity.CENTER);i.setTextColor(accent);i.setBackground(round(Color.rgb(255,248,229),13));h.addView(i,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(dp(10),0,dp(10),0);h.addView(tt,tp);TextView t=label(title,18,true);tt.addView(t);TextView s=label(subtitle,12,false);s.setTextColor(muted);tt.addView(s);View line=new View(this);line.setBackgroundColor(Color.rgb(239,242,245));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(12),0,dp(10));c.addView(line,lp);return c;}
    private EditText input(String hint){EditText e=new EditText(this);e.setHint(hint);e.setTextSize(15);e.setTextColor(ink);e.setHintTextColor(Color.rgb(145,151,159));e.setPadding(dp(13),dp(10),dp(13),dp(10));e.setBackground(round(Color.rgb(247,249,250),13));return e;}
    private TextView fieldLabel(String s){TextView t=label(s,13,true);t.setTextColor(Color.rgb(64,74,86));t.setPadding(0,dp(8),0,dp(4));return t;}
    private CheckBox flagBox(String s){CheckBox c=new CheckBox(this);c.setText(s);c.setTextSize(13);c.setTextColor(ink);c.setPadding(dp(8),0,dp(8),0);c.setBackground(round(Color.rgb(247,249,250),13));return c;}
    private Button premiumButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(15);b.setTypeface(null,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackground(round(primary,15));b.setOnTouchListener((v,e)->{if(e.getAction()==0)v.animate().scaleX(.985f).scaleY(.985f).setDuration(70).start();else if(e.getAction()==1||e.getAction()==3)v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();return false;});return b;}
    private Button outlineButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTextColor(primary);GradientDrawable g=round(Color.WHITE,14);g.setStroke(dp(1),Color.rgb(211,222,220));b.setBackground(g);return b;}
    private Button linkButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(12);b.setTextColor(muted);b.setBackgroundColor(Color.TRANSPARENT);b.setGravity(Gravity.START);return b;}
    private Button iconButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(26);b.setTextColor(Color.WHITE);b.setBackground(round(Color.argb(25,255,255,255),13));return b;}
    private TextView label(String x,int z,boolean bold){TextView t=new TextView(this);t.setText(x);t.setTextSize(z);t.setTextColor(ink);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private void animateSections(){for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(260).setStartDelay(i*45L).start();}}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
