package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.provider.Settings;
import android.transition.LayoutTransition;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SettingsActivity extends Activity {
    private static final int REQ_CALENDAR=901,REQ_BACKUP_FOLDER=902;
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115);
    private LinearLayout root;
    private TextView calendarStatus,backupStatus,notificationStatus;
    private Spinner calendarSpinner;
    private Switch calendarSwitch;
    private List<CalendarIntegration.CalendarItem> calendars=new ArrayList<>();

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();}
    @Override protected void onResume(){super.onResume();refreshStatuses();}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        LayoutTransition transition=new LayoutTransition();transition.enableTransitionType(LayoutTransition.CHANGING);root.setLayoutTransition(transition);scroll.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(17),dp(18),dp(18));hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=iconButton("‹");top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(42)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("الإعدادات","Settings"),28,true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("خصّص مناسبـاتي واربطه بخدماتك","Personalize Munasabati and connect your services"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        TextView premium=text("PREMIUM  •  v3.2",11,true);premium.setTextColor(Color.rgb(255,241,196));premium.setPadding(0,dp(10),0,0);hero.addView(premium);

        addAppearanceCard();
        addCalendarCard();
        addNotificationCard();
        addBackupCard();
        addAboutCard();

        setContentView(scroll);animateCards();
    }

    private void addAppearanceCard(){
        LinearLayout card=card(tr("المظهر واللغة","Appearance & language"),"✦",tr("لغة التطبيق، التاريخ الهجري، وألوان الفئات","App language, Hijri date and category colors"));
        LinearLayout line=new LinearLayout(this);line.setGravity(Gravity.CENTER_VERTICAL);card.addView(line);
        TextView langLabel=text(tr("لغة البرنامج","App language"),14,true);line.addView(langLabel,new LinearLayout.LayoutParams(0,-2,1));
        Spinner lang=new Spinner(this);lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"العربية","English"}));lang.setSelection(ar()?0:1);line.addView(lang,new LinearLayout.LayoutParams(dp(130),dp(48)));
        lang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}String next=pos==0?"ar":"en";if(!next.equals(AppSettings.language(SettingsActivity.this))){AppSettings.setLanguage(SettingsActivity.this,next);recreate();}}public void onNothingSelected(AdapterView<?> p){}});

        TextView hijri=text(tr("تعديل التاريخ الهجري","Hijri date adjustment"),14,true);hijri.setPadding(0,dp(10),0,dp(4));card.addView(hijri);
        Spinner offset=new Spinner(this);String[] opts=ar()?new String[]{"بدون تعديل","-1 يوم","-2 يوم","+1 يوم","+2 يوم"}:new String[]{"No adjustment","-1 day","-2 days","+1 day","+2 days"};int[] vals={0,-1,-2,1,2};offset.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));int current=AppSettings.hijriOffset(this),sel=0;for(int i=0;i<vals.length;i++)if(vals[i]==current)sel=i;offset.setSelection(sel);card.addView(offset,new LinearLayout.LayoutParams(-1,dp(48)));
        offset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}AppSettings.setHijriOffset(SettingsActivity.this,vals[pos]);}public void onNothingSelected(AdapterView<?> p){}});

        Button colors=premiumButton(tr("🎨  تخصيص ألوان الفئات","🎨  Customize category colors"));card.addView(colors,margin(-1,dp(48),0,dp(8),0,0));colors.setOnClickListener(v->showCategoryColors());
    }

    private void addCalendarCard(){
        LinearLayout card=card(tr("التقويمات المتصلة","Connected calendars"),"◫",tr("Google Calendar و Outlook / Microsoft المتاحان على الجهاز","Google Calendar and Outlook / Microsoft available on this device"));
        calendarStatus=text("",13,true);calendarStatus.setTextColor(primary);calendarStatus.setPadding(0,dp(2),0,dp(7));card.addView(calendarStatus);
        Button permission=premiumButton(tr("السماح بالوصول إلى التقويم","Allow calendar access"));card.addView(permission,new LinearLayout.LayoutParams(-1,dp(48)));permission.setOnClickListener(v->requestCalendarPermission());

        TextView target=text(tr("التقويم الهدف","Target calendar"),14,true);target.setPadding(0,dp(10),0,dp(4));card.addView(target);
        calendarSpinner=new Spinner(this);card.addView(calendarSpinner,new LinearLayout.LayoutParams(-1,dp(52)));
        calendarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(pos>=0&&pos<calendars.size())CalendarIntegration.selectCalendar(SettingsActivity.this,calendars.get(pos));}public void onNothingSelected(AdapterView<?> p){}});

        calendarSwitch=new Switch(this);calendarSwitch.setText(tr("مزامنة مناسباتي تلقائيًا مع التقويم المختار","Automatically sync Munasabati with selected calendar"));calendarSwitch.setTextSize(14);calendarSwitch.setChecked(CalendarIntegration.enabled(this));card.addView(calendarSwitch);
        calendarSwitch.setOnCheckedChangeListener((b,checked)->{if(checked&&!CalendarIntegration.hasPermission(this)){b.setChecked(false);requestCalendarPermission();return;}if(checked&&CalendarIntegration.selectedCalendarId(this)<0){b.setChecked(false);toast(tr("اختر تقويمًا أولًا","Choose a calendar first"));return;}CalendarIntegration.setEnabled(this,checked);if(checked)syncCalendarsNow();});

        Button sync=premiumButton(tr("↻  مزامنة جميع المناسبات الآن","↻  Sync all events now"));card.addView(sync,margin(-1,dp(48),0,dp(8),0,0));sync.setOnClickListener(v->syncCalendarsNow());
        TextView note=text(tr("Outlook: إذا لم يظهر التقويم هنا، افتح Outlook ← الإعدادات ← الحساب ← Calendar ثم فعّل Sync calendars.","Outlook: if your calendar does not appear here, open Outlook → Settings → account → Calendar and enable Sync calendars."),12,false);note.setTextColor(muted);note.setPadding(0,dp(8),0,0);card.addView(note);
    }

    private void addNotificationCard(){
        LinearLayout card=card(tr("الإشعارات والتنبيهات","Notifications & alerts"),"🔔",tr("الصوت، شاشة القفل، والتنبيه القوي","Sound, lock screen and Strong Alert"));
        notificationStatus=text("",13,true);notificationStatus.setPadding(0,0,0,dp(7));card.addView(notificationStatus);
        Button normal=premiumButton(tr("إعدادات إشعارات المناسبات","Event notification settings"));card.addView(normal,new LinearLayout.LayoutParams(-1,dp(48)));normal.setOnClickListener(v->NotificationSupport.openChannelSettings(this));
        Button strong=premiumButton(tr("إعدادات التنبيه القوي","Strong Alert settings"));card.addView(strong,margin(-1,dp(48),0,dp(7),0,0));strong.setOnClickListener(v->StrongAlertSupport.openFullScreenSettings(this));
    }

    private void addBackupCard(){
        LinearLayout card=card(tr("النسخ الاحتياطي","Backup"),"☁",tr("حماية المناسبات والمرفقات بنسخ دورية","Protect events and attachments with scheduled backups"));
        backupStatus=text("",13,true);backupStatus.setTextColor(primary);backupStatus.setPadding(0,0,0,dp(7));card.addView(backupStatus);
        Button folder=premiumButton(tr("اختيار / تغيير مجلد النسخ","Choose / change backup folder"));card.addView(folder,new LinearLayout.LayoutParams(-1,dp(48)));folder.setOnClickListener(v->chooseBackupFolder());
        Button now=premiumButton(tr("إنشاء Backup الآن","Create backup now"));card.addView(now,margin(-1,dp(48),0,dp(7),0,0));now.setOnClickListener(v->runBackupNow());
    }

    private void addAboutCard(){
        LinearLayout card=card(tr("حول مناسبـاتي","About Munasabati"),"ⓘ",tr("الإصدار وحالة التطبيق","Version and app status"));
        String version="?";try{PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);version=p.versionName;}catch(Exception ignored){}
        TextView v=text(tr("الإصدار ","Version ")+version,15,true);v.setTextColor(primary);card.addView(v);
        TextView info=text(tr("مصمم لإدارة المناسبات الشخصية والاجتماعية بالتاريخين الهجري والميلادي، مع تنبيهات ونسخ احتياطي وتكامل تقويم.","Designed for personal and social events with Hijri/Gregorian dates, alerts, backup and calendar integration."),13,false);info.setTextColor(muted);info.setPadding(0,dp(5),0,0);card.addView(info);
    }

    private void refreshStatuses(){
        if(calendarStatus!=null){boolean p=CalendarIntegration.hasPermission(this);String label=CalendarIntegration.selectedCalendarLabel(this);calendarStatus.setText(p?(label.isEmpty()?tr("✓ صلاحية التقويم متاحة — اختر تقويمًا","✓ Calendar access granted — choose a calendar"):tr("✓ مرتبط بـ: ","✓ Connected to: ")+label):tr("يلزم السماح بقراءة وكتابة التقويم","Calendar read/write permission is required"));loadCalendars();}
        if(notificationStatus!=null){boolean n=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;boolean fs=StrongAlertSupport.canUseFullScreen(this);notificationStatus.setText((n?"✓ ":"✕ ")+tr("الإشعارات ","Notifications ")+(n?tr("مسموحة","allowed"):tr("غير مسموحة","not allowed"))+"   •   "+(fs?"✓ ":"! ")+tr("شاشة كاملة","Full screen"));notificationStatus.setTextColor(n?primary:Color.rgb(180,55,55));}
        if(backupStatus!=null){long last=AppSettings.lastBackupTime(this);String when=last==0?tr("لا توجد نسخة بعد","No backup yet"):new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(last));backupStatus.setText((AppSettings.autoBackupEnabled(this)?tr("✓ النسخ التلقائي مفعّل","✓ Automatic backup enabled"):tr("النسخ التلقائي متوقف","Automatic backup disabled"))+" • "+when);}
    }

    private void loadCalendars(){
        if(calendarSpinner==null)return;calendars=CalendarIntegration.writableCalendars(this);ArrayList<String> labels=new ArrayList<>();for(CalendarIntegration.CalendarItem i:calendars)labels.add(i.toString());if(labels.isEmpty())labels.add(tr("لا توجد تقاويم قابلة للكتابة","No writable calendars found"));calendarSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));long selected=CalendarIntegration.selectedCalendarId(this);for(int i=0;i<calendars.size();i++)if(calendars.get(i).id==selected){calendarSpinner.setSelection(i);break;}}

    private void requestCalendarPermission(){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQ_CALENDAR);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grant){super.onRequestPermissionsResult(request,permissions,grant);if(request==REQ_CALENDAR){if(CalendarIntegration.hasPermission(this)){toast(tr("تم السماح بالوصول إلى التقويم","Calendar access granted"));refreshStatuses();}else toast(tr("لم يتم منح صلاحية التقويم","Calendar permission was not granted"));}}

    private void syncCalendarsNow(){
        if(!CalendarIntegration.hasPermission(this)){requestCalendarPermission();return;}if(CalendarIntegration.selectedCalendarId(this)<0){toast(tr("اختر تقويمًا أولًا","Choose a calendar first"));return;}CalendarIntegration.setEnabled(this,true);calendarSwitch.setChecked(true);toast(tr("جاري المزامنة...","Syncing..."));
        new Thread(()->{int n=CalendarIntegration.syncAll(this);runOnUiThread(()->toast(tr("تمت مزامنة ","Synced ")+n+tr(" مناسبة"," events")));},"calendar-sync").start();
    }

    private void chooseBackupFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,REQ_BACKUP_FOLDER);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==REQ_BACKUP_FOLDER&&result==RESULT_OK&&data!=null&&data.getData()!=null){try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getContentResolver().takePersistableUriPermission(data.getData(),flags);AppSettings.setAutoBackupTree(this,data.getData().toString());AppSettings.setAutoBackupEnabled(this,true);AutoBackupScheduler.schedule(this);toast(tr("تم اختيار المجلد وتفعيل النسخ التلقائي","Backup folder selected and automatic backup enabled"));refreshStatuses();}catch(Exception ex){toast(tr("تعذر حفظ صلاحية المجلد","Could not save folder access"));}}}

    private void runBackupNow(){if(AppSettings.autoBackupTree(this).isEmpty()){chooseBackupFolder();return;}toast(tr("جاري إنشاء النسخة...","Creating backup..."));new Thread(()->{try{AutoBackupManager.run(this);AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("ناجح","Success"));runOnUiThread(()->{toast(tr("تم إنشاء النسخة الاحتياطية","Backup created"));refreshStatuses();});}catch(Exception ex){runOnUiThread(()->toast(tr("فشل النسخ الاحتياطي","Backup failed")));}},"settings-backup").start();}

    private void showCategoryColors(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(5),dp(18),0);
        Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));box.addView(cat);
        Spinner col=new Spinner(this);col.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ColorPalette.labels(this,false)));box.addView(col);
        cat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){col.setSelection(ColorPalette.indexOf(ColorPalette.categoryHex(SettingsActivity.this,Categories.CODES[pos])));}public void onNothingSelected(AdapterView<?> p){}});
        new AlertDialog.Builder(this).setTitle(tr("لون الفئة","Category color")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->ColorPalette.setCategoryHex(this,Categories.CODES[cat.getSelectedItemPosition()],ColorPalette.HEX[col.getSelectedItemPosition()])).show();
    }

    private LinearLayout card(String title,String icon,String subtitle){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(15),dp(13),dp(15),dp(14));card.setBackground(round(Color.WHITE,22));card.setElevation(dp(2));root.addView(card,margin(-1,-2,0,0,0,dp(10)));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);
        TextView ico=text(icon,22,false);ico.setGravity(Gravity.CENTER);ico.setTextColor(accent);head.addView(ico,new LinearLayout.LayoutParams(dp(42),dp(42)));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);head.addView(names,new LinearLayout.LayoutParams(0,-2,1));TextView t=text(title,18,true);t.setTextColor(Color.rgb(31,42,55));names.addView(t);TextView s=text(subtitle,12,false);s.setTextColor(muted);names.addView(s);
        View line=new View(this);line.setBackgroundColor(Color.rgb(236,240,243));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(10),0,dp(9));card.addView(line,lp);return card;
    }

    private void animateCards(){for(int i=1;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(18));v.animate().alpha(1f).translationY(0f).setDuration(280).setStartDelay((i-1)*55L).start();}}
    private Button premiumButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(14);b.setTextColor(primary);GradientDrawable g=round(Color.rgb(241,247,246),14);g.setStroke(dp(1),Color.rgb(216,228,225));b.setBackground(g);b.setOnTouchListener((v,e)->{if(e.getAction()==MotionEvent.ACTION_DOWN)v.animate().scaleX(.98f).scaleY(.98f).setDuration(80).start();else if(e.getAction()==MotionEvent.ACTION_UP||e.getAction()==MotionEvent.ACTION_CANCEL)v.animate().scaleX(1f).scaleY(1f).setDuration(120).start();return false;});return b;}
    private Button iconButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(28);b.setTextColor(Color.WHITE);b.setBackground(round(Color.argb(35,255,255,255),14));b.setPadding(0,0,0,dp(4));return b;}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.rgb(31,42,55));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
