package com.rafat.munasabati;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class SyncCenterActivity extends Activity {
    private static final int REQ_CALENDAR=770;
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(244,247,249),muted=Color.rgb(91,101,115);
    private LinearLayout root,body;
    private Spinner calendarSpinner;
    private List<CalendarIntegration.CalendarItem> calendars=new ArrayList<>();

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();showSkeleton();body.postDelayed(this::renderBody,180);}
    @Override protected void onResume(){super.onResume();CalendarSyncScheduler.schedule(this);}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(bg);
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(16),dp(18),dp(18));hero.setBackground(round(primary,26));root.addView(hero,margin(-1,-2,0,0,0,dp(12)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=iconButton("‹");top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(42)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("مركز المزامنة","Sync Center"),27,true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("Google Calendar و Outlook / Microsoft","Google Calendar & Outlook / Microsoft"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        TextView mode=text(tr("↔ مزامنة ثنائية الاتجاه","↔ Two-way synchronization"),12,true);mode.setTextColor(Color.rgb(255,241,196));mode.setPadding(0,dp(10),0,0);hero.addView(mode);

        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,-2));
        setContentView(scroll);
        hero.setAlpha(0f);hero.setTranslationY(dp(-8));hero.animate().alpha(1f).translationY(0f).setDuration(320).start();
    }

    private void renderBody(){
        body.removeAllViews();
        addConnectionCard();addActionsCard();addStatsCard();addErrorsCard();
        for(int i=0;i<body.getChildCount();i++){View v=body.getChildAt(i);v.setAlpha(0f);v.setTranslationY(dp(10));v.animate().alpha(1f).translationY(0f).setDuration(240).setStartDelay(i*55L).start();}
    }

    private void addConnectionCard(){
        LinearLayout card=card(tr("التقويم المتصل","Connected calendar"),"◫",tr("اختر التقويم الذي تريد ربطه بالمناسبات","Choose the calendar to link with your events"));
        if(!CalendarIntegration.hasPermission(this)){
            TextView warn=text(tr("يلزم السماح بقراءة وكتابة التقويم.","Calendar read/write access is required."),13,true);warn.setTextColor(Color.rgb(180,55,55));card.addView(warn);
            Button allow=premiumButton(tr("السماح بالوصول إلى التقويم","Allow calendar access"));card.addView(allow,margin(-1,dp(50),0,dp(9),0,0));allow.setOnClickListener(v->requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQ_CALENDAR));return;
        }
        calendars=CalendarIntegration.writableCalendars(this);ArrayList<String> labels=new ArrayList<>();for(CalendarIntegration.CalendarItem c:calendars)labels.add(c.toString());if(labels.isEmpty())labels.add(tr("لا توجد تقاويم قابلة للكتابة","No writable calendars found"));
        calendarSpinner=new Spinner(this);calendarSpinner.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,labels));card.addView(calendarSpinner,new LinearLayout.LayoutParams(-1,dp(54)));
        long selected=CalendarIntegration.selectedCalendarId(this);for(int i=0;i<calendars.size();i++)if(calendars.get(i).id==selected){calendarSpinner.setSelection(i);break;}
        calendarSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){boolean ready=false;public void onItemSelected(AdapterView<?> p,View v,int pos,long id){if(!ready){ready=true;return;}if(pos>=0&&pos<calendars.size()){CalendarIntegration.selectCalendar(SyncCenterActivity.this,calendars.get(pos));CalendarIntegration.setEnabled(SyncCenterActivity.this,true);CalendarSyncScheduler.schedule(SyncCenterActivity.this);}}public void onNothingSelected(AdapterView<?> p){}});
        String label=CalendarIntegration.selectedCalendarLabel(this);TextView connected=text(label.isEmpty()?tr("اختر تقويمًا للبدء","Select a calendar to begin"):tr("متصل بـ: ","Connected to: ")+label,12,true);connected.setTextColor(primary);connected.setPadding(0,dp(7),0,0);card.addView(connected);
    }

    private void addActionsCard(){
        LinearLayout card=card(tr("إجراءات المزامنة","Sync actions"),"↻",tr("مزامنة التعديلات أو استيراد أحداث من التقويم","Synchronize changes or import calendar events"));
        Button sync=premiumButton(tr("↔  مزامنة الآن","↔  Sync now"));card.addView(sync,new LinearLayout.LayoutParams(-1,dp(52)));sync.setOnClickListener(v->syncNow());
        Button imp=outlineButton(tr("＋  استيراد أحداث Google / Outlook","＋  Import Google / Outlook events"));card.addView(imp,margin(-1,dp(52),0,dp(8),0,0));imp.setOnClickListener(v->loadImportEvents());
        Button settings=outlineButton(tr("⚙  إعدادات الربط والتقويم","⚙  Calendar connection settings"));card.addView(settings,margin(-1,dp(48),0,dp(7),0,0));settings.setOnClickListener(v->startActivity(new Intent(this,SettingsActivity.class)));
    }

    private void addStatsCard(){
        LinearLayout card=card(tr("آخر مزامنة","Last synchronization"),"✓",tr("ملخص آخر عملية مزامنة ثنائية","Summary of the latest two-way sync"));
        long last=CalendarIntegration.lastSyncTime(this);String when=last==0?tr("لم تتم مزامنة بعد","No sync yet"):new SimpleDateFormat("yyyy-MM-dd  HH:mm",ar()?new Locale("ar"):Locale.ENGLISH).format(new Date(last));
        TextView time=text(when,15,true);time.setTextColor(primary);card.addView(time);
        String summary=CalendarIntegration.lastSyncSummary(this);if(!summary.isEmpty()){TextView s=text(summary,13,false);s.setTextColor(muted);s.setPadding(0,dp(4),0,dp(10));card.addView(s);}
        GridLayout grid=new GridLayout(this);grid.setColumnCount(2);card.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        stat(grid,tr("مرفوع","Pushed"),CalendarIntegration.lastPushed(this),"↑");stat(grid,tr("مسحوب","Pulled"),CalendarIntegration.lastPulled(this),"↓");stat(grid,tr("محذوف","Deleted"),CalendarIntegration.lastDeleted(this),"×");stat(grid,tr("مستورد","Imported"),CalendarIntegration.lastImported(this),"＋");
        if(CalendarIntegration.lastConflicts(this)>0){TextView conflict=text(tr("⚠ التعارضات: ","⚠ Conflicts: ")+CalendarIntegration.lastConflicts(this),13,true);conflict.setTextColor(Color.rgb(180,100,25));conflict.setPadding(0,dp(10),0,0);card.addView(conflict);}
    }

    private void addErrorsCard(){
        String errors=CalendarIntegration.lastSyncErrors(this);LinearLayout card=card(tr("التشخيص","Diagnostics"),errors.isEmpty()?"✓":"!",tr("الأخطاء أو التعارضات التي تحتاج انتباهك","Errors or conflicts that need attention"));
        TextView e=text(errors.isEmpty()?tr("لا توجد أخطاء في آخر مزامنة.","No errors in the last synchronization."):errors,13,errors.isEmpty());e.setTextColor(errors.isEmpty()?primary:Color.rgb(165,65,50));e.setLineSpacing(0,1.15f);card.addView(e);
    }

    private void syncNow(){
        if(!CalendarIntegration.hasPermission(this)){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQ_CALENDAR);return;}
        if(CalendarIntegration.selectedCalendarId(this)<=0){toast(tr("اختر تقويمًا أولًا","Choose a calendar first"));return;}
        CalendarIntegration.setEnabled(this,true);showSkeleton();
        new Thread(()->{CalendarIntegration.SyncResult r=CalendarIntegration.syncBidirectional(this);runOnUiThread(()->{renderBody();toast(r.hasErrors()?tr("اكتملت المزامنة مع ملاحظات","Sync completed with notes"):tr("اكتملت المزامنة بنجاح","Synchronization completed"));});},"manual-calendar-sync").start();
    }

    private void loadImportEvents(){
        if(!CalendarIntegration.hasPermission(this)){requestPermissions(new String[]{Manifest.permission.READ_CALENDAR,Manifest.permission.WRITE_CALENDAR},REQ_CALENDAR);return;}
        long cal=CalendarIntegration.selectedCalendarId(this);if(calendarSpinner!=null&&calendarSpinner.getSelectedItemPosition()>=0&&calendarSpinner.getSelectedItemPosition()<calendars.size()){CalendarIntegration.CalendarItem item=calendars.get(calendarSpinner.getSelectedItemPosition());CalendarIntegration.selectCalendar(this,item);cal=item.id;}
        if(cal<=0){toast(tr("اختر تقويمًا أولًا","Choose a calendar first"));return;}
        showSkeleton();final long calendarId=cal;
        new Thread(()->{Calendar from=Calendar.getInstance();from.add(Calendar.DAY_OF_YEAR,-30);Calendar to=Calendar.getInstance();to.add(Calendar.DAY_OF_YEAR,365);List<CalendarIntegration.ExternalEvent> events=CalendarIntegration.listExternalEvents(this,calendarId,from.getTimeInMillis(),to.getTimeInMillis());runOnUiThread(()->{renderBody();showImportDialog(events);});},"calendar-import-load").start();
    }

    private void showImportDialog(List<CalendarIntegration.ExternalEvent> events){
        HashSet<Long> linked=new HashSet<>();for(EventStore.Event e:EventStore.load(this))if(e.calendarEventId>0)linked.add(e.calendarEventId);ArrayList<CalendarIntegration.ExternalEvent> available=new ArrayList<>();for(CalendarIntegration.ExternalEvent e:events)if(!linked.contains(e.id))available.add(e);
        if(available.isEmpty()){toast(tr("لا توجد أحداث جديدة متاحة للاستيراد","No new events are available to import"));return;}
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(4),dp(16),0);box.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        TextView catLabel=text(tr("الفئة التي ستُسند للأحداث المستوردة","Category for imported events"),13,true);box.addView(catLabel);
        Spinner category=new Spinner(this);category.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));box.addView(category,new LinearLayout.LayoutParams(-1,dp(50)));
        CheckBox all=new CheckBox(this);all.setText(tr("تحديد الكل","Select all"));all.setTypeface(null,Typeface.BOLD);box.addView(all);
        ScrollView scroll=new ScrollView(this);LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);scroll.addView(list);box.addView(scroll,new LinearLayout.LayoutParams(-1,dp(360)));
        ArrayList<CheckBox> checks=new ArrayList<>();SimpleDateFormat df=new SimpleDateFormat("dd MMM yyyy • HH:mm",ar()?new Locale("ar"):Locale.ENGLISH);
        for(CalendarIntegration.ExternalEvent event:available){CheckBox cb=new CheckBox(this);cb.setText(df.format(new Date(event.start))+"\n"+event.title);cb.setTag(event.id);cb.setPadding(0,dp(5),0,dp(5));list.addView(cb);checks.add(cb);}
        all.setOnCheckedChangeListener((b,checked)->{for(CheckBox cb:checks)cb.setChecked(checked);});
        new AlertDialog.Builder(this).setTitle(tr("استيراد من التقويم","Import from calendar")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("استيراد","Import"),(d,w)->{
            ArrayList<Long> ids=new ArrayList<>();for(CheckBox cb:checks)if(cb.isChecked())ids.add((Long)cb.getTag());if(ids.isEmpty()){toast(tr("لم تحدد أي أحداث","No events selected"));return;}String code=Categories.CODES[category.getSelectedItemPosition()];showSkeleton();new Thread(()->{CalendarIntegration.SyncResult r=CalendarIntegration.importExternalEvents(this,ids,code);runOnUiThread(()->{renderBody();toast(tr("تم استيراد ","Imported ")+r.imported+tr(" مناسبة"," events"));});},"calendar-import").start();
        }).show();
    }

    private void showSkeleton(){
        body.removeAllViews();for(int i=0;i<4;i++){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackground(round(Color.WHITE,20));body.addView(card,margin(-1,dp(112),0,0,0,dp(9)));for(int j=0;j<3;j++){View bar=new View(this);bar.setBackground(round(Color.rgb(225,230,234),8));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(j==0?dp(150):-1,j==0?dp(18):dp(13));p.setMargins(0,0,0,dp(9));card.addView(bar,p);ObjectAnimator a=ObjectAnimator.ofFloat(bar,"alpha",0.35f,0.82f);a.setDuration(650);a.setRepeatMode(ValueAnimator.REVERSE);a.setRepeatCount(2);a.setStartDelay((i*70L)+(j*45L));a.start();}}}

    @Override public void onRequestPermissionsResult(int req,String[] p,int[] g){super.onRequestPermissionsResult(req,p,g);if(req==REQ_CALENDAR){if(CalendarIntegration.hasPermission(this)){CalendarIntegration.setEnabled(this,true);CalendarSyncScheduler.schedule(this);renderBody();}else toast(tr("لم يتم منح صلاحية التقويم","Calendar permission was not granted"));}}

    private LinearLayout card(String title,String icon,String subtitle){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(14),dp(16),dp(15));c.setBackground(round(Color.WHITE,21));rootCard(c);LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);c.addView(head);TextView i=text(icon,21,true);i.setTextColor(accent);i.setGravity(Gravity.CENTER);i.setBackground(round(Color.rgb(255,248,229),13));head.addView(i,new LinearLayout.LayoutParams(dp(44),dp(44)));LinearLayout tt=new LinearLayout(this);tt.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(dp(10),0,dp(10),0);head.addView(tt,tp);TextView t=text(title,18,true);t.setTextColor(Color.rgb(31,42,55));tt.addView(t);TextView s=text(subtitle,12,false);s.setTextColor(muted);tt.addView(s);View line=new View(this);line.setBackgroundColor(Color.rgb(239,242,245));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(1));lp.setMargins(0,dp(12),0,dp(12));c.addView(line,lp);return c;}
    private void rootCard(View c){body.addView(c,margin(-1,-2,0,0,0,dp(10)));}
    private void stat(GridLayout g,String label,int value,String icon){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(12),dp(10),dp(12),dp(10));box.setBackground(round(Color.rgb(247,249,250),14));TextView v=text(icon+"  "+value,19,true);v.setTextColor(primary);box.addView(v);TextView l=text(label,12,false);l.setTextColor(muted);box.addView(l);GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(3),dp(3),dp(3),dp(3));g.addView(box,p);}
    private Button premiumButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(14);b.setTypeface(null,Typeface.BOLD);b.setTextColor(Color.WHITE);b.setBackground(round(primary,14));b.setOnTouchListener((v,e)->{if(e.getAction()==0)v.animate().scaleX(.985f).scaleY(.985f).setDuration(70).start();else if(e.getAction()==1||e.getAction()==3)v.animate().scaleX(1f).scaleY(1f).setDuration(110).start();return false;});return b;}
    private Button outlineButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTextColor(primary);GradientDrawable d=round(Color.WHITE,14);d.setStroke(dp(1),Color.rgb(211,222,220));b.setBackground(d);return b;}
    private Button iconButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(26);b.setTextColor(Color.WHITE);b.setBackground(round(Color.argb(25,255,255,255),13));return b;}
    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(31,42,55));if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
