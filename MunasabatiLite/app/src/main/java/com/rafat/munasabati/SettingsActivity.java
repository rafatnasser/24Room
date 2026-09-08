package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
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
    private Switch calendarSwitch,backupSwitch;
    private List<CalendarIntegration.CalendarItem> calendars=new ArrayList<>();

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();}
    @Override protected void onResume(){super.onResume();refreshStatuses();}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(17),dp(18),dp(18));hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=iconButton("‹");top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(42)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("الإعدادات","Settings"),28,true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("تجربة أكثر أناقة وتحكمًا وربطًا","A cleaner, smarter and more connected experience"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        TextView premium=text("MUNASABATI  •  PREMIUM  •  v3.3",11,true);premium.setTextColor(Color.rgb(255,241,196));premium.setPadding(0,dp(10),0,0);hero.addView(premium);

        addAppearanceCard();addCalendarCard();addNotificationCard();addBackupCard();addAboutCard();
        setContentView(scroll);animateCards();
    }

    private void addAppearanceCard(){
        LinearLayout card=card(tr("المظهر واللغة","Appearance & language"),"✦",tr("لغة التطبيق، التاريخ الهجري وألوان الفئات","Language, Hijri date and category colors"));
        LinearLayout row=new LinearLayout(this);row.setGravity(Gravity.CENTER_VERTICAL);card.addView(row);
        TextView l=text(tr("لغة البرنامج","App language"),14,true);row.addView(l,new LinearLayout.LayoutParams(0,-2,1));
        Spinner lang=new Spinner(this);lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"العربية","English"}));lang.setSelection(ar()?0:1);row.addView(lang,new LinearLayout.LayoutParams(dp(132),dp(48)));
        lang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}String next=pos==0?"ar":"en";if(!next.equals(AppSettings.language(SettingsActivity.this))){AppSettings.setLanguage(SettingsActivity.this,next);recreate();}}public void onNothingSelected(AdapterView<?> p){}});

        TextView h=text(tr("تعديل التاريخ الهجري","Hijri date adjustment"),14,true);h.setPadding(0,dp(10),0,dp(4));card.addView(h);
        Spinner offset=new Spinner(this);String[] opts=ar()?new String[]{"بدون تعديل","-1 يوم","-2 يوم","+1 يوم","+2 يوم"}:new String[]{"No adjustment","-1 day","-2 days","+1 day","+2 days"};int[] vals={0,-1,-2,1,2};offset.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));int current=AppSettings.hijriOffset(this),sel=0;for(int i=0;i<vals.length;i++)if(vals[i]==current)sel=i;offset.setSelection(sel);card.addView(offset,new LinearLayout.LayoutParams(-1,dp(48)));
        offset.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean first=true;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}AppSettings.setHijriOffset(SettingsActivity.this,vals[pos]);}public void onNothingSelected(AdapterView<?> p){}});
        Button colors=premiumButton(tr("🎨  تخصيص ألوان الفئات","🎨  Customize category colors"));card.addView(colors,margin(-1,dp(50),0,dp(8),0,0));colors.setOnClickListener(v->showCategoryColors());
    }

    private void addCalendarCard(){
        LinearLayout card=card(tr("المزامنة والتقويمات","Calendar synchronization"),"↔",tr("Google Calendar و Outlook / Microsoft بمزامنة ثنائية","Two-way Google Calendar & Outlook / Microsoft sync"));
        calendarStatus=text("",13,true);calendarStatus.setTextColor(primary);calendarStatus.setPadding(0,0,0,dp(8));card.addView(calendarStatus);
        Button permission=outlineButton(tr("السماح بالوصول إلى التقويم","Allow calendar access"));card.addView(permission,new LinearLayout.LayoutParams(-1,dp(48)));permission.setOnClickListener(v->requestCalendarPermission());

        TextView target=text(tr("التقويم الافتراضي للمناسبات الجديدة","Default calendar for new synced events"),14,true);target.setPadding(0,dp(10),0,dp(4));card.addView(target);
        calendarSpinner=new Spinner(this);card.addView(calendarSpinner,new LinearLayout.LayoutParams(-1,dp(54)));
        calendarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean ready=false;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!ready){ready=true;return;}if(pos>=0&&pos<calendars.size()){CalendarIntegration.selectCalendar(SettingsActivity.this,calendars.get(pos));refreshStatuses();}}public void onNothingSelected(AdapterView<?> p){}});

        calendarSwitch=new Switch(this);calendarSwitch.setText(tr("تشغيل المزامنة الثنائية التلقائية للمناسبات المفعّل لها Sync","Run automatic two-way sync for events with Sync enabled"));calendarSwitch.setTextSize(14);calendarSwitch.setChecked(CalendarIntegration.enabled(this));card.addView(calendarSwitch);
        calendarSwitch.setOnCheckedChangeListener((b,checked)->{if(checked&&!CalendarIntegration.hasPermission(this)){b.setChecked(false);requestCalendarPermission();return;}if(checked&&CalendarIntegration.selectedCalendarId(this)<0){b.setChecked(false);toast(tr("اختر تقويمًا أولًا","Choose a calendar first"));return;}CalendarIntegration.setEnabled(this,checked);if(checked)CalendarSyncScheduler.schedule(this);else CalendarSyncScheduler.cancel(this);});

        Button center=premiumButton(tr("↔  فتح مركز المزامنة","↔  Open Sync Center"));card.addView(center,margin(-1,dp(52),0,dp(10),0,0));center.setOnClickListener(v->startActivity(new Intent(this,SyncCenterActivity.class)));
        TextView note=text(tr("يمكنك من داخل كل مناسبة تفعيل أو إيقاف «مزامنة هذه المناسبة» بشكل مستقل.","Each event has its own “Sync this event” switch, independent of the global background sync setting."),12,false);note.setTextColor(muted);note.setPadding(0,dp(7),0,0);card.addView(note);
    }

    private void addNotificationCard(){
        LinearLayout card=card(tr("الإشعارات والتنبيهات","Notifications & alerts"),"🔔",tr("الصوت، شاشة القفل والتنبيه القوي","Sound, lock screen and Strong Alert"));
        notificationStatus=text("",13,true);notificationStatus.setPadding(0,0,0,dp(8));card.addView(notificationStatus);
        Button normal=outlineButton(tr("إعدادات إشعارات المناسبات","Event notification settings"));card.addView(normal,new LinearLayout.LayoutParams(-1,dp(48)));normal.setOnClickListener(v->NotificationSupport.openChannelSettings(this));
        Button strong=outlineButton(tr("إعدادات التنبيه القوي وشاشة القفل","Strong Alert & lock-screen settings"));card.addView(strong,margin(-1,dp(48),0,dp(7),0,0));strong.setOnClickListener(v->StrongAlertSupport.openFullScreenSettings(this));
    }

    private void addBackupCard(){
        LinearLayout card=card(tr("النسخ الاحتياطي","Backup"),"☁",tr("حماية المناسبات والمرفقات بنسخ تلقائية","Protect events and attachments automatically"));
        backupStatus=text("",13,true);backupStatus.setTextColor(primary);backupStatus.setPadding(0,0,0,dp(6));card.addView(backupStatus);
        backupSwitch=new Switch(this);backupSwitch.setText(tr("تفعيل النسخ الاحتياطي التلقائي","Enable automatic backup"));backupSwitch.setChecked(AppSettings.autoBackupEnabled(this));card.addView(backupSwitch);

        LinearLayout row=new LinearLayout(this);row.setPadding(0,dp(6),0,0);card.addView(row);
        Spinner freq=new Spinner(this);String[] fl=ar()?new String[]{"يومي","أسبوعي","كل 30 يوم"}:new String[]{"Daily","Weekly","Every 30 days"};int[] fv={1,7,30};freq.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,fl));int fp=AppSettings.autoBackupDays(this)==1?0:(AppSettings.autoBackupDays(this)==30?2:1);freq.setSelection(fp);row.addView(freq,new LinearLayout.LayoutParams(0,dp(50),1));
        Spinner keep=new Spinner(this);String[] kl=ar()?new String[]{"آخر 3 نسخ","آخر 5 نسخ","آخر 10 نسخ"}:new String[]{"Keep 3","Keep 5","Keep 10"};int[] kv={3,5,10};keep.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,kl));int kp=AppSettings.autoBackupKeep(this)==3?0:(AppSettings.autoBackupKeep(this)==10?2:1);keep.setSelection(kp);row.addView(keep,new LinearLayout.LayoutParams(0,dp(50),1));
        backupSwitch.setOnCheckedChangeListener((b,checked)->{AppSettings.setAutoBackupEnabled(this,checked);AppSettings.setAutoBackupDays(this,fv[freq.getSelectedItemPosition()]);AppSettings.setAutoBackupKeep(this,kv[keep.getSelectedItemPosition()]);AutoBackupScheduler.schedule(this);refreshStatuses();});
        freq.setOnItemSelectedListener(new SimpleSelection(){public void selected(int pos){AppSettings.setAutoBackupDays(SettingsActivity.this,fv[pos]);AutoBackupScheduler.schedule(SettingsActivity.this);}});keep.setOnItemSelectedListener(new SimpleSelection(){public void selected(int pos){AppSettings.setAutoBackupKeep(SettingsActivity.this,kv[pos]);}});

        Button folder=outlineButton(tr("اختيار / تغيير مجلد النسخ","Choose / change backup folder"));card.addView(folder,margin(-1,dp(48),0,dp(8),0,0));folder.setOnClickListener(v->chooseBackupFolder());
        Button now=premiumButton(tr("إنشاء Backup الآن","Create backup now"));card.addView(now,margin(-1,dp(50),0,dp(7),0,0));now.setOnClickListener(v->runBackupNow());
    }

    private void addAboutCard(){
        LinearLayout card=card(tr("حول مناسبـاتي","About Munasabati"),"ⓘ",tr("الإصدار وحالة التطبيق","Version and app status"));
        String version="?";try{PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);version=p.versionName;}catch(Exception ignored){}
        TextView v=text(tr("الإصدار ","Version ")+version,16,true);v.setTextColor(primary);card.addView(v);
        TextView info=text(tr("منظم مناسبات شخصي بالتاريخين الهجري والميلادي، مع تنبيهات متقدمة، Backup ومزامنة تقويم ثنائية.","Personal event organizer with Hijri/Gregorian dates, advanced alerts, backup and two-way calendar sync."),13,false);info.setTextColor(muted);info.setPadding(0,dp(5),0,0);card.addView(info);
    }

    private void refreshStatuses(){
        if(calendarStatus!=null){boolean p=CalendarIntegration.hasPermission(this);String label=CalendarIntegration.selectedCalendarLabel(this);calendarStatus.setText(p?(label.isEmpty()?tr("✓ صلاحية التقويم متاحة — اختر تقويمًا","✓ Calendar access granted — choose a calendar"):tr("✓ التقويم: ","✓ Calendar: ")+label):tr("يلزم السماح بقراءة وكتابة التقويم","Calendar read/write permission is required"));loadCalendars();if(calendarSwitch!=null)calendarSwitch.setChecked(CalendarIntegration.enabled(this));}
        if(notificationStatus!=null){boolean n=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;boolean fs=StrongAlertSupport.canUseFullScreen(this);notificationStatus.setText((n?"✓ ":"✕ ")+tr("الإشعارات ","Notifications ")+(n?tr("مسموحة","allowed"):tr("غير مسموحة","not allowed"))+"   •   "+(fs?"✓ ":"! ")+tr("شاشة كاملة","Full screen"));notificationStatus.setTextColor(n?primary:Color.rgb(180,55,55));}
        if(backupStatus!=null){long last=AppSettings.lastBackupTime(this);String when=last==0?tr("لا توجد نسخة بعد","No backup yet"):new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.getDefault()).format(new Date(last));backupStatus.setText((AppSettings.autoBackupEnabled(this)?tr("✓ النسخ التلقائي مفعّل","✓ Automatic backup enabled"):tr("النسخ التلقائي متوقف","Automatic backup disabled"))+" • "+when);if(backupSwitch!=null)backupSwitch.setChecked(AppSettings.autoBackupEnabled(this));}
    }

    private void loadCalendars(){if(calendarSpinner==null)return;calendars=CalendarIntegration.writableCalendars(this);ArrayList<String> labels=new ArrayList<>();for(CalendarIntegration.CalendarItem i:calendars)labels.add(i.toString());if(labels.isEmpty())labels.add(tr("لا توجد تقاويم قابلة للكتابة","No writable calendars found"));calendarSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));long selected=CalendarIntegration.selectedCalendarId(this);for(int i=0;i<calendars.size();i++)if(calendars.get(i).id==selected){calendarSpinner.setSelection(i);break;}}
    private void requestCalendarPermission(){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQ_CALENDAR);}
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] grant){super.onRequestPermissionsResult(request,permissions,grant);if(request==REQ_CALENDAR){if(CalendarIntegration.hasPermission(this)){CalendarIntegration.setEnabled(this,true);CalendarSyncScheduler.schedule(this);refreshStatuses();toast(tr("تم السماح بالوصول إلى التقويم","Calendar access granted"));}else toast(tr("لم يتم منح صلاحية التقويم","Calendar permission was not granted"));}}

    private void chooseBackupFolder(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,REQ_BACKUP_FOLDER);}
    @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==REQ_BACKUP_FOLDER&&result==RESULT_OK&&data!=null&&data.getData()!=null){try{int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getContentResolver().takePersistableUriPermission(data.getData(),flags);AppSettings.setAutoBackupTree(this,data.getData().toString());AppSettings.setAutoBackupEnabled(this,true);AutoBackupScheduler.schedule(this);refreshStatuses();toast(tr("تم اختيار المجلد وتفعيل النسخ التلقائي","Backup folder selected and automatic backup enabled"));}catch(Exception ex){toast(tr("تعذر حفظ صلاحية المجلد","Could not save folder access"));}}}
    private void runBackupNow(){if(AppSettings.autoBackupTree(this).isEmpty()){chooseBackupFolder();return;}toast(tr("جاري إنشاء النسخة...","Creating backup..."));new Thread(()->{try{AutoBackupManager.run(this);AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("ناجح","Success"));runOnUiThread(()->{refreshStatuses();toast(tr("تم إنشاء النسخة الاحتياطية","Backup created"));});}catch(Exception ex){AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("فشل","Failed"));runOnUiThread(()->toast(tr("فشل النسخ الاحتياطي","Backup failed")));}},"settings-backup").start();}

    private void showCategoryColors(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(5),dp(18),0);Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));box.addView(cat);Spinner col=new Spinner(this);col.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ColorPalette.labels(this,false)));box.addView(col);cat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){col.setSelection(ColorPalette.indexOf(ColorPalette.categoryHex(SettingsActivity.this,Categories.CODES[pos])));}public void onNothingSelected(AdapterView<?> p){}});new AlertDialog.Builder(this).setTitle(tr("لون الفئة","Category color")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->ColorPalette.setCategoryHex(this,Categories.CODES[cat.getSelectedItemPosition()],ColorPalette.HEX[col.getSelectedItemPosition()])).show();}

    private LinearLayout card(String title,String icon,String subtitle){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(15));c.setBackground(round(Color.WHITE,21));root.addView(c,margin(-1,-2,0,0,0,dp(10)));LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);c.addView(head);TextView i=text(icon,21,true);i.setTextColor(accent);i.setGravity(Gravity.CENTER);i.setBackground(round(Color.rgb(255,248,229),13));head.addView(i,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(dp(10),0,dp(10),0);head.addView(tt,tp);TextView t=text(title,18,true);tt.addView(t);TextView s=text(subtitle,12,false);s.setTextColor(muted);tt.addView(s);View line=new View(this);line.setBackgroundColor(Color.rgb(239,242,245));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(12),0,dp(12));c.addView(line,lp);return c;}
    private void animateCards(){for(int i=0;i<root.getChildCount();i++){View v=root.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(12));v.animate().alpha(1f).translationY(0f).setDuration(260).setStartDelay(i*55L).start();}}
    private Button premiumButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackground(round(primary,14));b.setOnTouchListener((v,e)->{if(e.getAction()==0)v.animate().scaleX(.985f).scaleY(.985f).setDuration(70).start();else if(e.getAction()==1||e.getAction()==3)v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();return false;});return b;}
    private Button outlineButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTextColor(primary);GradientDrawable d=round(Color.WHITE,14);d.setStroke(dp(1),Color.rgb(211,222,220));b.setBackground(d);return b;}
    private Button iconButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(26);b.setTextColor(Color.WHITE);b.setBackground(round(Color.argb(25,255,255,255),13));return b;}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(31,42,55));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}

    private abstract class SimpleSelection implements AdapterView.OnItemSelectedListener {private boolean first=true;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(first){first=false;return;}selected(pos);}public void onNothingSelected(AdapterView<?> p){}public abstract void selected(int pos);}
}
