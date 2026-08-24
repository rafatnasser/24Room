package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_BACKUP=501,REQ_RESTORE=502,REQ_EXPORT_JSON=503,REQ_IMPORT_JSON=504,REQ_AUTO_FOLDER=505;
    private static final int MODE_TODAY=0,MODE_LIST=1,MODE_CALENDAR=2,MODE_FAVORITES=3;

    private LinearLayout contentHost;
    private EditText search;
    private Spinner categoryFilter,dayFilter;
    private Button dateFilter,todayBtn,listBtn,calendarBtn,favBtn;
    private int mode=MODE_TODAY;
    private long selectedDate=0L;
    private Calendar shownMonth=Calendar.getInstance();
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(246,248,251),muted=Color.rgb(91,101,115);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},90);
        AutoBackupScheduler.schedule(this);
    }
    @Override protected void onResume(){super.onResume();render();}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(12),dp(9),dp(12),dp(9));root.setBackgroundColor(bg);root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(17),dp(13),dp(17),dp(14));hero.setBackground(round(primary,24));root.addView(hero,margin(-1,-2,0,0,0,dp(9)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("مناسباتي","Munasabati"),28,true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("اليوم، القادم، والذكريات المهمة","Today, upcoming events and important memories"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        Button settings=button("⚙",Color.WHITE,Color.TRANSPARENT,16);settings.setTextSize(21);top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(44)));settings.setOnClickListener(v->showSettings());

        LinearLayout heroActions=new LinearLayout(this);heroActions.setPadding(0,dp(11),0,0);hero.addView(heroActions);
        Button add=button(tr("＋  مناسبة جديدة","＋  New event"),primary,Color.WHITE,16);add.setTypeface(null,Typeface.BOLD);heroActions.addView(add,new LinearLayout.LayoutParams(0,dp(50),1));add.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class)));
        Button tools=button("☁",Color.WHITE,Color.rgb(38,112,105),16);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(dp(55),dp(50));tlp.setMargins(dp(7),0,0,0);heroActions.addView(tools,tlp);tools.setOnClickListener(v->showDataTools());

        LinearLayout modes=new LinearLayout(this);modes.setBackground(round(Color.WHITE,18));modes.setPadding(dp(4),dp(4),dp(4),dp(4));root.addView(modes,margin(-1,dp(48),0,0,0,dp(7)));
        todayBtn=modeButton(tr("اليوم","Today"));listBtn=modeButton(tr("القائمة","List"));calendarBtn=modeButton(tr("التقويم","Calendar"));favBtn=modeButton("⭐");
        modes.addView(todayBtn,new LinearLayout.LayoutParams(0,-1,1));modes.addView(listBtn,new LinearLayout.LayoutParams(0,-1,1));modes.addView(calendarBtn,new LinearLayout.LayoutParams(0,-1,1));modes.addView(favBtn,new LinearLayout.LayoutParams(0,-1,1));
        todayBtn.setOnClickListener(v->setMode(MODE_TODAY));listBtn.setOnClickListener(v->setMode(MODE_LIST));calendarBtn.setOnClickListener(v->setMode(MODE_CALENDAR));favBtn.setOnClickListener(v->setMode(MODE_FAVORITES));

        LinearLayout searchCard=new LinearLayout(this);searchCard.setOrientation(LinearLayout.VERTICAL);searchCard.setPadding(dp(10),dp(8),dp(10),dp(8));searchCard.setBackground(round(Color.WHITE,18));root.addView(searchCard,margin(-1,-2,0,0,0,dp(7)));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(14);search.setHint(tr("🔎  ابحث بالاسم، الفئة، التاريخ، الموقع...","🔎  Search name, category, date, location..."));search.setBackground(round(Color.rgb(244,247,249),13));search.setPadding(dp(11),0,dp(11),0);searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(44)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render();}public void afterTextChanged(Editable e){}});

        LinearLayout f1=new LinearLayout(this);f1.setPadding(0,dp(5),0,0);searchCard.addView(f1);
        categoryFilter=new Spinner(this);categoryFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,true)));f1.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(44),1));
        dayFilter=new Spinner(this);dayFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,dayLabels()));f1.addView(dayFilter,new LinearLayout.LayoutParams(0,dp(44),1));
        AdapterView.OnItemSelectedListener filterListener=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}public void onNothingSelected(AdapterView<?> p){}};
        categoryFilter.setOnItemSelectedListener(filterListener);dayFilter.setOnItemSelectedListener(filterListener);

        LinearLayout f2=new LinearLayout(this);f2.setPadding(0,dp(3),0,0);searchCard.addView(f2);
        dateFilter=outlineButton(tr("📅  كل التواريخ","📅  All dates"));f2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(42),1));dateFilter.setOnClickListener(v->pickFilterDate());
        Button clear=outlineButton(tr("مسح","Clear"));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(42),1);cp.setMargins(dp(6),0,0,0);f2.addView(clear,cp);clear.setOnClickListener(v->{search.setText("");categoryFilter.setSelection(0);dayFilter.setSelection(0);selectedDate=0;dateFilter.setText(tr("📅  كل التواريخ","📅  All dates"));render();});

        ScrollView scroll=new ScrollView(this);contentHost=new LinearLayout(this);contentHost.setOrientation(LinearLayout.VERTICAL);contentHost.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(contentHost,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);updateModeButtons();render();
    }

    private Button modeButton(String s){Button b=button(s,muted,Color.TRANSPARENT,12);b.setTypeface(null,Typeface.BOLD);return b;}
    private void setMode(int m){mode=m;updateModeButtons();render();}
    private void updateModeButtons(){
        if(todayBtn==null)return;
        Button[] bs={todayBtn,listBtn,calendarBtn,favBtn};for(int i=0;i<bs.length;i++){boolean on=mode==i;bs[i].setTextColor(on?primary:muted);bs[i].setBackground(round(on?Color.rgb(229,242,240):Color.TRANSPARENT,12));}
    }

    private void render(){
        if(contentHost==null)return;contentHost.removeAllViews();
        if(mode==MODE_TODAY)renderToday();
        else if(mode==MODE_CALENDAR)renderCalendar();
        else if(mode==MODE_FAVORITES)renderFavorites();
        else renderList();
    }

    private void renderToday(){
        Calendar start=Calendar.getInstance();zeroTime(start);
        Calendar tomorrow=(Calendar)start.clone();tomorrow.add(Calendar.DAY_OF_YEAR,1);
        Calendar dayAfter=(Calendar)start.clone();dayAfter.add(Calendar.DAY_OF_YEAR,2);
        Calendar weekEnd=(Calendar)start.clone();int untilSaturday=(Calendar.SATURDAY-start.get(Calendar.DAY_OF_WEEK)+7)%7;weekEnd.add(Calendar.DAY_OF_YEAR,untilSaturday+1);
        Calendar monthEnd=(Calendar)start.clone();monthEnd.set(Calendar.DAY_OF_MONTH,1);monthEnd.add(Calendar.MONTH,1);
        long todayStart=start.getTimeInMillis(),tomorrowStart=tomorrow.getTimeInMillis(),afterTomorrow=dayAfter.getTimeInMillis();
        long weekStop=Math.min(weekEnd.getTimeInMillis(),monthEnd.getTimeInMillis()),monthStop=monthEnd.getTimeInMillis();

        boolean any=false;
        any|=renderRange(tr("اليوم","Today"),todayStart,tomorrowStart);
        any|=renderRange(tr("غدًا","Tomorrow"),tomorrowStart,afterTomorrow);
        if(weekStop>afterTomorrow)any|=renderRange(tr("هذا الأسبوع","This week"),afterTomorrow,weekStop);
        long monthFrom=Math.max(afterTomorrow,weekStop);if(monthStop>monthFrom)any|=renderRange(tr("هذا الشهر","This month"),monthFrom,monthStop);
        if(!any)empty(tr("لا توجد مناسبات قريبة مطابقة للبحث.","No nearby events match your filters."));
    }

    private boolean renderRange(String heading,long start,long end){
        ArrayList<Occurrence> rows=new ArrayList<>();
        for(EventStore.Event e:EventStore.load(this)){
            long t=Recurrence.firstOccurrenceBetween(e,start,end);
            if(t>=0&&matchesBasic(e,t))rows.add(new Occurrence(e,t));
        }
        rows.sort((a,b)->{if(a.e.pinned!=b.e.pinned)return a.e.pinned?-1:1;if(a.e.favorite!=b.e.favorite)return a.e.favorite?-1:1;return Long.compare(a.time,b.time);});
        if(rows.isEmpty())return false;section(heading);for(Occurrence o:rows)addCard(o.e,o.time);return true;
    }

    private void renderList(){
        List<EventStore.Event> events=EventStore.load(this);long now=System.currentTimeMillis();
        events.sort((a,b)->{if(a.pinned!=b.pinned)return a.pinned?-1:1;if(a.favorite!=b.favorite)return a.favorite?-1:1;return Long.compare(displayTime(a,now),displayTime(b,now));});
        boolean any=false,up=false,past=false;
        for(EventStore.Event e:events){long t=displayTime(e,now);if(t>=now&&matches(e,t)){if(!up){section(tr("القادمة","Upcoming"));up=true;}addCard(e,t);any=true;}}
        for(EventStore.Event e:events){long t=e.eventTime;if(!Recurrence.isRecurring(e)&&t<now&&matches(e,t)){if(!past){section(tr("السابقة","Past"));past=true;}addCard(e,t);any=true;}}
        if(!any)empty(tr("لا توجد مناسبات مطابقة.","No matching events."));
    }

    private void renderFavorites(){
        List<EventStore.Event> events=EventStore.load(this);long now=System.currentTimeMillis();
        events.sort((a,b)->{if(a.pinned!=b.pinned)return a.pinned?-1:1;return Long.compare(displayTime(a,now),displayTime(b,now));});
        boolean any=false;section(tr("⭐ المفضلة","⭐ Favorites"));
        for(EventStore.Event e:events){if(!e.favorite)continue;long t=displayTime(e,now);if(matches(e,t)){addCard(e,t);any=true;}}
        if(!any)empty(tr("لا توجد مناسبات في المفضلة.","No favorite events yet."));
    }

    private void renderCalendar(){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(dp(3),dp(5),dp(3),dp(7));contentHost.addView(nav);
        Button prev=chip("‹"),next=chip("›");TextView month=text(monthTitle(),20,true);month.setTextColor(primary);month.setGravity(Gravity.CENTER);
        nav.addView(prev,new LinearLayout.LayoutParams(dp(46),dp(40)));nav.addView(month,new LinearLayout.LayoutParams(0,dp(40),1));nav.addView(next,new LinearLayout.LayoutParams(dp(46),dp(40)));
        prev.setOnClickListener(v->{shownMonth.add(Calendar.MONTH,-1);render();});next.setOnClickListener(v->{shownMonth.add(Calendar.MONTH,1);render();});

        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);contentHost.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        String[] days=ar()?new String[]{"أح","إث","ثل","أر","خم","جم","سب"}:new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for(String d:days){TextView h=text(d,11,true);h.setTextColor(muted);h.setGravity(Gravity.CENTER);grid.addView(h,cellParams(dp(28)));}

        Calendar first=(Calendar)shownMonth.clone();first.set(Calendar.DAY_OF_MONTH,1);zeroTime(first);
        int offset=first.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY,max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<offset;i++){LinearLayout blank=new LinearLayout(this);grid.addView(blank,cellParams(dp(64)));}

        for(int day=1;day<=max;day++){
            Calendar d=(Calendar)first.clone();d.set(Calendar.DAY_OF_MONTH,day);long start=d.getTimeInMillis();Calendar en=(Calendar)d.clone();en.add(Calendar.DAY_OF_MONTH,1);long end=en.getTimeInMillis();
            List<Occurrence> matches=occurrencesOnDay(start,end);
            LinearLayout cell=new LinearLayout(this);cell.setOrientation(LinearLayout.VERTICAL);cell.setGravity(Gravity.CENTER);cell.setPadding(dp(2),dp(4),dp(2),dp(4));
            boolean today=sameDay(start,System.currentTimeMillis());cell.setBackground(round(today?Color.rgb(229,242,240):Color.WHITE,12));
            TextView dayText=text(String.valueOf(day),13,!matches.isEmpty()||today);dayText.setGravity(Gravity.CENTER);dayText.setTextColor(today?primary:Color.rgb(45,55,68));cell.addView(dayText,new LinearLayout.LayoutParams(-1,0,1));

            LinearLayout dots=new LinearLayout(this);dots.setGravity(Gravity.CENTER);cell.addView(dots,new LinearLayout.LayoutParams(-1,dp(14)));
            int shown=Math.min(4,matches.size());for(int i=0;i<shown;i++){View dot=new View(this);GradientDrawable dg=new GradientDrawable();dg.setShape(GradientDrawable.OVAL);dg.setColor(ColorPalette.color(this,matches.get(i).e));dot.setBackground(dg);LinearLayout.LayoutParams dpv=new LinearLayout.LayoutParams(dp(7),dp(7));dpv.setMargins(dp(2),0,dp(2),0);dots.addView(dot,dpv);}
            if(matches.size()>4){TextView plus=text("+",9,true);plus.setTextColor(muted);dots.addView(plus);}
            grid.addView(cell,cellParams(dp(64)));
            final long selected=start;cell.setOnClickListener(v->{selectedDate=selected;dateFilter.setText(DateTools.gregorianShort(this,selected));setMode(MODE_LIST);});
        }
        TextView tip=text(tr("النقاط الملوّنة تمثل فئات المناسبات. اضغط على اليوم لعرض تفاصيله.","Colored dots represent event categories. Tap a day to view details."),12,false);tip.setTextColor(muted);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(11),0,dp(7));contentHost.addView(tip);
    }

    private List<Occurrence> occurrencesOnDay(long start,long end){
        ArrayList<Occurrence> out=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=Recurrence.firstOccurrenceBetween(e,start,end);if(t>=0&&matchesBasic(e,t))out.add(new Occurrence(e,t));}
        out.sort(Comparator.comparingLong(x->x.time));return out;
    }

    private long displayTime(EventStore.Event e,long now){return Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,now):e.eventTime;}
    private boolean matches(EventStore.Event e,long t){return matchesBasic(e,t)&&matchesDate(t);}
    private boolean matchesBasic(EventStore.Event e,long t){
        int cat=categoryFilter==null?0:categoryFilter.getSelectedItemPosition();if(cat>0&&!Categories.CODES[cat-1].equals(e.category))return false;
        int day=dayFilter==null?0:dayFilter.getSelectedItemPosition();if(day>0&&DateTools.dayOfWeek(t)!=day)return false;
        String q=search==null?"":normalize(search.getText().toString());if(q.isEmpty())return true;
        String hay=e.title+" "+e.details+" "+e.locationName+" "+e.locationUrl+" "+Categories.arabicLabel(e.category)+" "+Categories.englishLabel(e.category)+" "+DateTools.gregorian(this,t,true)+" "+DateTools.hijri(this,t)+" "+DateTools.dayArabic(t)+" "+DateTools.dayEnglish(t);
        return normalize(hay).contains(q);
    }
    private boolean matchesDate(long t){if(selectedDate<=0)return true;return sameDay(selectedDate,t);}
    private boolean sameDay(long a,long b){Calendar x=Calendar.getInstance(),y=Calendar.getInstance();x.setTimeInMillis(a);y.setTimeInMillis(b);return x.get(Calendar.YEAR)==y.get(Calendar.YEAR)&&x.get(Calendar.DAY_OF_YEAR)==y.get(Calendar.DAY_OF_YEAR);}
    private String normalize(String s){String x=java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);return x.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ة','ه');}

    private void addCard(EventStore.Event e,long shownTime){
        int base=ColorPalette.color(this,e),soft=ColorPalette.soft(base);
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setBackground(round(soft,20));contentHost.addView(outer,margin(-1,-2,0,0,0,dp(8)));
        View stripe=new View(this);stripe.setBackgroundColor(base);outer.addView(stripe,new LinearLayout.LayoutParams(dp(6),-1));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(12),dp(10),dp(12),dp(9));outer.addView(card,new LinearLayout.LayoutParams(0,-2,1));

        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);
        TextView icon=text(Categories.icon(e.category),21,false);head.addView(icon,new LinearLayout.LayoutParams(dp(36),-2));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);head.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView n=text(e.title,18,true);n.setTextColor(Color.rgb(31,42,55));names.addView(n);
        LinearLayout meta=new LinearLayout(this);meta.setGravity(Gravity.CENTER_VERTICAL);names.addView(meta);
        TextView cat=text(Categories.label(this,e.category),12,true);cat.setTextColor(base);meta.addView(cat);
        TextView remain=text("  •  "+relativeText(shownTime),12,true);remain.setTextColor(Color.rgb(72,82,94));meta.addView(remain);

        Button star=chip(e.favorite?"★":"☆");star.setTextColor(e.favorite?accent:muted);head.addView(star,new LinearLayout.LayoutParams(dp(42),dp(38)));star.setOnClickListener(v->toggleFavorite(e));
        Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(42),dp(38)));pin.setOnClickListener(v->togglePinned(e));

        TextView g=text("📅  "+DateTools.gregorian(this,shownTime,true),13,false);g.setTextColor(Color.rgb(55,65,81));g.setPadding(0,dp(6),0,0);card.addView(g);
        TextView h=text("☾  "+DateTools.hijri(this,shownTime),13,false);h.setTextColor(base);h.setPadding(0,dp(2),0,0);card.addView(h);
        if(Recurrence.isRecurring(e)){TextView rr=text("↻  "+tr("يتكرر ","Repeats ")+Recurrence.label(this,e.recurrence),12,true);rr.setTextColor(base);rr.setPadding(0,dp(2),0,0);card.addView(rr);}
        if(!e.locationName.trim().isEmpty()){TextView l=text("📍  "+e.locationName,13,false);l.setTextColor(muted);l.setPadding(0,dp(3),0,0);card.addView(l);}
        if(!e.details.trim().isEmpty()){TextView d=text(e.details,13,false);d.setTextColor(muted);d.setPadding(0,dp(5),0,0);card.addView(d);}

        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);actions.setPadding(0,dp(6),0,0);card.addView(actions);
        Button share=chip(tr("مشاركة","Share"));actions.addView(share);share.setOnClickListener(v->shareEvent(e,shownTime));
        if(!e.attachmentUri.isEmpty()){Button a=chip("📎");actions.addView(a);a.setOnClickListener(v->openAttachment(e));}
        if(!e.locationUrl.isEmpty()||!e.locationName.isEmpty()){Button m=chip("📍");actions.addView(m);m.setOnClickListener(v->openLocation(e.locationUrl.isEmpty()?e.locationName:e.locationUrl));}
        Button edit=chip(tr("تعديل","Edit"));actions.addView(edit);edit.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
        Button del=chip(tr("حذف","Delete"));actions.addView(del);del.setOnClickListener(v->confirmDelete(e));
    }

    private String relativeText(long when){
        long diff=when-System.currentTimeMillis(),abs=Math.abs(diff);
        if(abs<60000L)return tr("الآن","Now");
        boolean future=diff>0;
        if(abs<3600000L){long n=Math.max(1,Math.round(abs/60000.0));return future?tr("بعد "+n+" دقيقة","in "+n+" min"):tr("منذ "+n+" دقيقة",n+" min ago");}
        if(abs<86400000L){long n=Math.max(1,Math.round(abs/3600000.0));return future?tr("بعد "+n+" ساعة","in "+n+" hr"):tr("منذ "+n+" ساعة",n+" hr ago");}
        long n=Math.max(1,(long)Math.ceil(abs/86400000.0));return future?tr("متبقي "+n+" يوم",n+" days left"):tr("منذ "+n+" يوم",n+" days ago");
    }

    private void shareEvent(EventStore.Event e,long time){
        StringBuilder s=new StringBuilder();s.append(e.title).append("\n").append(Categories.label(this,e.category)).append("\n");
        s.append(DateTools.gregorian(this,time,true)).append("\n").append(DateTools.hijri(this,time));
        if(!e.locationName.isEmpty())s.append("\n📍 ").append(e.locationName);
        if(!e.locationUrl.isEmpty())s.append("\n").append(e.locationUrl);
        Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_SUBJECT,e.title);i.putExtra(Intent.EXTRA_TEXT,s.toString());
        startActivity(Intent.createChooser(i,tr("مشاركة المناسبة عبر","Share event via")));
    }

    private void toggleFavorite(EventStore.Event e){List<EventStore.Event> all=EventStore.load(this);for(EventStore.Event x:all)if(x.id==e.id)x.favorite=!x.favorite;EventStore.save(this,all);render();}
    private void togglePinned(EventStore.Event e){List<EventStore.Event> all=EventStore.load(this);for(EventStore.Event x:all)if(x.id==e.id)x.pinned=!x.pinned;EventStore.save(this,all);render();}
    private void confirmDelete(EventStore.Event e){new AlertDialog.Builder(this).setTitle(tr("حذف المناسبة","Delete event")).setMessage(tr("هل تريد حذف «"+e.title+"»؟","Delete “"+e.title+"”?" )).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حذف","Delete"),(d,w)->{ReminderScheduler.cancel(this,e.id);List<EventStore.Event> all=EventStore.load(this);all.removeIf(x->x.id==e.id);EventStore.save(this,all);render();}).show();}

    private void showSettings(){
        String[] items=ar()?new String[]{"اللغة والتاريخ الهجري","🎨 ألوان الفئات","☁ النسخ الاحتياطي التلقائي","ℹ حول التطبيق"}:new String[]{"Language & Hijri date","🎨 Category colors","☁ Automatic backup","ℹ About"};
        new AlertDialog.Builder(this).setTitle(tr("الإعدادات","Settings")).setItems(items,(d,which)->{
            if(which==0)showLanguageSettings();else if(which==1)showCategoryColors();else if(which==2)showAutoBackupSettings();else showAbout();
        }).show();
    }

    private void showLanguageSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(3),dp(18),0);
        box.addView(fieldLabel(tr("لغة البرنامج","App language")));Spinner lang=new Spinner(this);lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"العربية","English"}));lang.setSelection(ar()?0:1);box.addView(lang);
        box.addView(fieldLabel(tr("تعديل التاريخ الهجري","Hijri date adjustment")));Spinner offset=new Spinner(this);String[] opts=ar()?new String[]{"بدون تعديل","-1 يوم","-2 يوم","+1 يوم","+2 يوم"}:new String[]{"No adjustment","-1 day","-2 days","+1 day","+2 days"};int[] vals={0,-1,-2,1,2};offset.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));int cur=AppSettings.hijriOffset(this),pos=0;for(int i=0;i<vals.length;i++)if(vals[i]==cur)pos=i;offset.setSelection(pos);box.addView(offset);
        new AlertDialog.Builder(this).setTitle(tr("اللغة والتاريخ","Language & date")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{AppSettings.setLanguage(this,lang.getSelectedItemPosition()==0?"ar":"en");AppSettings.setHijriOffset(this,vals[offset.getSelectedItemPosition()]);recreate();}).show();
    }

    private void showCategoryColors(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(5),dp(18),0);
        Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));box.addView(cat);
        Spinner col=new Spinner(this);col.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ColorPalette.labels(this,false)));box.addView(col);
        cat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){col.setSelection(ColorPalette.indexOf(ColorPalette.categoryHex(MainActivity.this,Categories.CODES[pos])));}public void onNothingSelected(AdapterView<?> p){}});
        new AlertDialog.Builder(this).setTitle(tr("لون الفئة","Category color")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{ColorPalette.setCategoryHex(this,Categories.CODES[cat.getSelectedItemPosition()],ColorPalette.HEX[col.getSelectedItemPosition()]);render();}).show();
    }

    private void showAutoBackupSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(4),dp(18),0);
        CheckBox enabled=new CheckBox(this);enabled.setText(tr("تفعيل النسخ الاحتياطي التلقائي","Enable automatic backup"));enabled.setChecked(AppSettings.autoBackupEnabled(this));box.addView(enabled);
        TextView folder=text(AppSettings.autoBackupTree(this).isEmpty()?tr("لم يتم اختيار مجلد بعد","No folder selected yet"):tr("✓ تم اختيار مجلد النسخ الاحتياطي","✓ Backup folder selected"),13,true);folder.setTextColor(AppSettings.autoBackupTree(this).isEmpty()?muted:primary);box.addView(folder);
        Button choose=outlineButton(tr("اختيار / تغيير المجلد","Choose / change folder"));box.addView(choose,margin(-1,dp(46),0,dp(6),0,dp(5)));choose.setOnClickListener(v->chooseAutoBackupFolder());

        box.addView(fieldLabel(tr("التكرار","Frequency")));Spinner freq=new Spinner(this);String[] fl=ar()?new String[]{"يومي","أسبوعي","كل 30 يوم"}:new String[]{"Daily","Weekly","Every 30 days"};int[] fv={1,7,30};freq.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,fl));int fp=AppSettings.autoBackupDays(this)==1?0:(AppSettings.autoBackupDays(this)==30?2:1);freq.setSelection(fp);box.addView(freq);
        box.addView(fieldLabel(tr("عدد النسخ المحتفظ بها","Backups to keep")));Spinner keep=new Spinner(this);String[] kl={"3","5","10"};int[] kv={3,5,10};keep.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,kl));int kp=AppSettings.autoBackupKeep(this)==3?0:(AppSettings.autoBackupKeep(this)==10?2:1);keep.setSelection(kp);box.addView(keep);
        Button now=outlineButton(tr("إنشاء نسخة الآن","Back up now"));box.addView(now,margin(-1,dp(46),0,dp(7),0,0));now.setOnClickListener(v->runAutoBackupNow());

        new AlertDialog.Builder(this).setTitle(tr("النسخ الاحتياطي التلقائي","Automatic backup")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{
            AppSettings.setAutoBackupEnabled(this,enabled.isChecked());AppSettings.setAutoBackupDays(this,fv[freq.getSelectedItemPosition()]);AppSettings.setAutoBackupKeep(this,kv[keep.getSelectedItemPosition()]);
            AutoBackupScheduler.schedule(this);toast(tr("تم حفظ إعدادات النسخ التلقائي","Automatic backup settings saved"));
        }).show();
    }

    private void chooseAutoBackupFolder(){
        Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION|Intent.FLAG_GRANT_PREFIX_URI_PERMISSION);startActivityForResult(i,REQ_AUTO_FOLDER);
    }

    private void runAutoBackupNow(){
        if(AppSettings.autoBackupTree(this).isEmpty()){toast(tr("اختر مجلد النسخ الاحتياطي أولًا","Choose a backup folder first"));return;}
        toast(tr("جاري إنشاء النسخة الاحتياطية...","Creating backup..."));
        new Thread(()->{
            try{AutoBackupManager.run(this);AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("ناجح","Success"));runOnUiThread(()->toast(tr("تم إنشاء النسخة الاحتياطية","Backup created")));}
            catch(Exception ex){AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("فشل: ","Failed: ")+(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage()));runOnUiThread(()->toast(tr("فشل النسخ الاحتياطي","Backup failed")));}
        },"manual-auto-backup").start();
    }

    private void showAbout(){
        String version="?";try{PackageInfo p=getPackageManager().getPackageInfo(getPackageName(),0);version=p.versionName;}catch(Exception ignored){}
        boolean notifications=Build.VERSION.SDK_INT<33||checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
        AlarmManager am=(AlarmManager)getSystemService(ALARM_SERVICE);boolean exact=Build.VERSION.SDK_INT<31||am.canScheduleExactAlarms();
        String last=AppSettings.lastBackupTime(this)==0?tr("لا يوجد","None"):new SimpleDateFormat("yyyy-MM-dd HH:mm",ar()?new Locale("ar"):Locale.ENGLISH).format(new Date(AppSettings.lastBackupTime(this)));
        StringBuilder s=new StringBuilder();
        s.append(tr("الإصدار: ","Version: ")).append(version).append("\n\n");
        s.append(tr("الإشعارات: ","Notifications: ")).append(notifications?tr("مسموحة ✓","Allowed ✓"):tr("غير مسموحة ✕","Not allowed ✕")).append("\n");
        s.append(tr("التنبيهات الدقيقة: ","Exact alarms: ")).append(exact?tr("متاحة ✓","Available ✓"):tr("غير متاحة — يستخدم التطبيق تنبيهًا تقريبيًا","Unavailable — app uses approximate alarms")).append("\n\n");
        s.append(tr("النسخ التلقائي: ","Automatic backup: ")).append(AppSettings.autoBackupEnabled(this)?tr("مفعّل","Enabled"):tr("متوقف","Disabled")).append("\n");
        s.append(tr("مجلد النسخ: ","Backup folder: ")).append(AppSettings.autoBackupTree(this).isEmpty()?tr("غير محدد","Not selected"):tr("محدد ✓","Selected ✓")).append("\n");
        s.append(tr("آخر Backup: ","Last backup: ")).append(last);
        if(!AppSettings.lastBackupStatus(this).isEmpty())s.append("\n").append(tr("الحالة: ","Status: ")).append(AppSettings.lastBackupStatus(this));
        new AlertDialog.Builder(this).setTitle(tr("حول مناسبـاتي","About Munasabati")).setMessage(s.toString()).setPositiveButton(tr("إغلاق","Close"),null).show();
    }

    private void showDataTools(){
        String[] items=ar()?new String[]{"نسخة احتياطية كاملة (مع المرفقات)","استعادة نسخة احتياطية","تصدير JSON","استيراد JSON","النسخ الاحتياطي التلقائي"}:new String[]{"Full backup (with attachments)","Restore backup","Export JSON","Import JSON","Automatic backup"};
        new AlertDialog.Builder(this).setTitle(tr("البيانات والنسخ الاحتياطي","Data & backup")).setItems(items,(d,which)->{
            if(which==0)createDocument("application/zip","Munasabati_Backup_"+dateStamp()+".munasabati",REQ_BACKUP);
            else if(which==1)openDocument(new String[]{"application/zip","application/octet-stream","*/*"},REQ_RESTORE);
            else if(which==2)createDocument("application/json","Munasabati_Events_"+dateStamp()+".json",REQ_EXPORT_JSON);
            else if(which==3)openDocument(new String[]{"application/json","text/plain","*/*"},REQ_IMPORT_JSON);
            else showAutoBackupSettings();
        }).show();
    }

    private void createDocument(String type,String name,int req){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,req);}
    private void openDocument(String[] types,int req){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,types);startActivityForResult(i,req);}

    @Override protected void onActivityResult(int request,int result,Intent data){
        super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        try{
            if(request==REQ_AUTO_FOLDER){
                int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);getContentResolver().takePersistableUriPermission(uri,flags);
                AppSettings.setAutoBackupTree(this,uri.toString());AppSettings.setAutoBackupEnabled(this,true);AutoBackupScheduler.schedule(this);toast(tr("تم اختيار المجلد وتفعيل النسخ التلقائي","Folder selected and automatic backup enabled"));return;
            }
            if(request==REQ_BACKUP){try(OutputStream out=getContentResolver().openOutputStream(uri)){int n=BackupManager.createBackup(this,out);AppSettings.setLastBackup(this,System.currentTimeMillis(),tr("نسخة يدوية ناجحة","Manual backup successful"));toast(tr("تم إنشاء النسخة الاحتياطية. المرفقات: ","Backup created. Attachments: ")+n);}}
            else if(request==REQ_RESTORE){try(InputStream in=getContentResolver().openInputStream(uri)){int n=BackupManager.restoreBackup(this,in);toast(tr("تمت استعادة ","Restored ")+n+tr(" مناسبة"," events"));recreate();}}
            else if(request==REQ_EXPORT_JSON){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(EventStore.exportJson(this,true).toString(2).getBytes("UTF-8"));toast(tr("تم تصدير ملف JSON","JSON exported"));}}
            else if(request==REQ_IMPORT_JSON){try(InputStream in=getContentResolver().openInputStream(uri)){int n=EventStore.importJson(this,new JSONObject(readAll(in)));toast(tr("تم استيراد ","Imported ")+n+tr(" مناسبة"," events"));recreate();}}
        }catch(Exception ex){new AlertDialog.Builder(this).setTitle(tr("تعذر إكمال العملية","Operation failed")).setMessage(ex.getMessage()==null?ex.toString():ex.getMessage()).setPositiveButton("OK",null).show();}
    }

    private void pickFilterDate(){Calendar c=Calendar.getInstance();if(selectedDate>0)c.setTimeInMillis(selectedDate);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.set(y,m,d,0,0,0);n.set(Calendar.MILLISECOND,0);selectedDate=n.getTimeInMillis();dateFilter.setText(DateTools.gregorianShort(this,selectedDate));if(mode==MODE_TODAY)setMode(MODE_LIST);else render();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private String[] dayLabels(){return ar()?new String[]{"كل الأيام","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"}:new String[]{"All days","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};}
    private String monthTitle(){Locale l=ar()?new Locale("ar"):Locale.ENGLISH;return new SimpleDateFormat("MMMM yyyy",l).format(shownMonth.getTime());}
    private void section(String s){TextView t=text(s,18,true);t.setTextColor(primary);t.setPadding(dp(2),dp(8),dp(2),dp(5));contentHost.addView(t);}
    private void empty(String s){TextView e=text(s,15,true);e.setTextColor(muted);e.setGravity(Gravity.CENTER);e.setPadding(dp(10),dp(36),dp(10),dp(20));contentHost.addView(e);}

    private void openAttachment(EventStore.Event e){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(e.attachmentUri),e.attachmentType.isEmpty()?"*/*":e.attachmentType);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception ex){toast(tr("تعذر فتح المرفق","Could not open attachment"));}}
    private void openLocation(String raw){try{Uri u=raw.startsWith("http")||raw.startsWith("geo:")?Uri.parse(raw):Uri.parse("geo:0,0?q="+Uri.encode(raw));startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){toast(tr("تعذر فتح الموقع","Could not open location"));}}

    private GridLayout.LayoutParams cellParams(int height){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=height;p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}
    private String dateStamp(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    private String readAll(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}
    private void zeroTime(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private TextView fieldLabel(String s){TextView t=text(s,14,true);t.setTextColor(Color.rgb(55,65,81));t.setPadding(0,dp(9),0,dp(3));return t;}
    private Button chip(String s){return button(s,primary,Color.argb(22,25,91,86),12);}
    private Button outlineButton(String s){Button b=button(s,primary,Color.WHITE,13);GradientDrawable g=round(Color.WHITE,12);g.setStroke(dp(1),Color.rgb(214,222,229));b.setBackground(g);return b;}
    private Button button(String s,int textColor,int back,int size){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(textColor);b.setTextSize(size);b.setPadding(dp(7),0,dp(7),0);b.setMinWidth(0);b.setMinHeight(0);b.setBackground(round(back,13));return b;}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.BLACK);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
    private static final class Occurrence{final EventStore.Event e;final long time;Occurrence(EventStore.Event event,long t){e=event;time=t;}}
}
