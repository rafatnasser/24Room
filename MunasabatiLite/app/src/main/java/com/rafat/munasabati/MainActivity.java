package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.text.*;
import android.view.*;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.text.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_BACKUP=501,REQ_RESTORE=502,REQ_EXPORT_JSON=503,REQ_IMPORT_JSON=504;
    private LinearLayout contentHost;
    private EditText search;
    private Spinner categoryFilter,dayFilter;
    private Button dateFilter,listModeBtn,calendarModeBtn;
    private long selectedDate=0L;
    private boolean calendarMode=false;
    private Calendar shownMonth=Calendar.getInstance();
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(246,248,251),muted=Color.rgb(91,101,115);

    @Override public void onCreate(Bundle b){
        super.onCreate(b);getWindow().setStatusBarColor(primary);buildUi();
        if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},90);
    }
    @Override protected void onResume(){super.onResume();render();}
    private String tr(String ar,String en){return AppSettings.tr(this,ar,en);}
    private boolean ar(){return AppSettings.isArabic(this);}

    private void buildUi(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(13),dp(10),dp(13),dp(10));root.setBackgroundColor(bg);root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(18),dp(14),dp(18),dp(14));hero.setBackground(round(primary,24));root.addView(hero,margin(-1,-2,0,0,0,dp(10)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("مناسباتي","Munasabati"),29,true);title.setTextColor(Color.WHITE);titles.addView(title);
        TextView sub=text(tr("كل مناسباتك وذكرياتك في مكان واحد","Your events and memories in one place"),13,false);sub.setTextColor(Color.rgb(220,238,235));titles.addView(sub);
        Button settings=button("⚙",Color.WHITE,Color.TRANSPARENT,16);settings.setTextSize(21);top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(44)));settings.setOnClickListener(v->showSettings());

        LinearLayout heroActions=new LinearLayout(this);heroActions.setPadding(0,dp(12),0,0);hero.addView(heroActions);
        Button add=button(tr("＋  مناسبة جديدة","＋  New event"),primary,Color.WHITE,16);add.setTypeface(null,Typeface.BOLD);heroActions.addView(add,new LinearLayout.LayoutParams(0,dp(52),1));add.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class)));
        Button tools=button("☁",Color.WHITE,Color.rgb(38,112,105),16);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(dp(56),dp(52));tlp.setMargins(dp(7),0,0,0);heroActions.addView(tools,tlp);tools.setOnClickListener(v->showDataTools());

        LinearLayout modes=new LinearLayout(this);modes.setBackground(round(Color.WHITE,18));modes.setPadding(dp(5),dp(5),dp(5),dp(5));root.addView(modes,margin(-1,dp(50),0,0,0,dp(8)));
        listModeBtn=button(tr("☷  القائمة","☷  List"),primary,Color.rgb(229,242,240),14);calendarModeBtn=button(tr("▦  التقويم","▦  Calendar"),muted,Color.TRANSPARENT,14);
        modes.addView(listModeBtn,new LinearLayout.LayoutParams(0,-1,1));modes.addView(calendarModeBtn,new LinearLayout.LayoutParams(0,-1,1));
        listModeBtn.setOnClickListener(v->{calendarMode=false;updateModeButtons();render();});
        calendarModeBtn.setOnClickListener(v->{calendarMode=true;updateModeButtons();render();});

        LinearLayout searchCard=new LinearLayout(this);searchCard.setOrientation(LinearLayout.VERTICAL);searchCard.setPadding(dp(10),dp(9),dp(10),dp(9));searchCard.setBackground(round(Color.WHITE,18));root.addView(searchCard,margin(-1,-2,0,0,0,dp(8)));
        search=new EditText(this);search.setSingleLine(true);search.setTextSize(15);search.setHint(tr("🔎  ابحث بالاسم، الفئة، التاريخ، الموقع...","🔎  Search name, category, date, location..."));search.setBackground(round(Color.rgb(244,247,249),13));search.setPadding(dp(12),0,dp(12),0);searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){render();}public void afterTextChanged(Editable e){}});

        LinearLayout f1=new LinearLayout(this);f1.setPadding(0,dp(6),0,0);searchCard.addView(f1);
        categoryFilter=new Spinner(this);categoryFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,true)));f1.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(46),1));
        dayFilter=new Spinner(this);dayFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,dayLabels()));f1.addView(dayFilter,new LinearLayout.LayoutParams(0,dp(46),1));
        AdapterView.OnItemSelectedListener filterListener=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){render();}public void onNothingSelected(AdapterView<?> p){}};
        categoryFilter.setOnItemSelectedListener(filterListener);dayFilter.setOnItemSelectedListener(filterListener);

        LinearLayout f2=new LinearLayout(this);f2.setPadding(0,dp(4),0,0);searchCard.addView(f2);
        dateFilter=outlineButton(tr("📅  كل التواريخ","📅  All dates"));f2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(44),1));dateFilter.setOnClickListener(v->pickFilterDate());
        Button clear=outlineButton(tr("مسح","Clear"));LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(0,dp(44),1);cp.setMargins(dp(6),0,0,0);f2.addView(clear,cp);clear.setOnClickListener(v->{search.setText("");categoryFilter.setSelection(0);dayFilter.setSelection(0);selectedDate=0;dateFilter.setText(tr("📅  كل التواريخ","📅  All dates"));render();});

        ScrollView scroll=new ScrollView(this);contentHost=new LinearLayout(this);contentHost.setOrientation(LinearLayout.VERTICAL);contentHost.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(contentHost,new ScrollView.LayoutParams(-1,-2));root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);updateModeButtons();render();
    }

    private void updateModeButtons(){
        if(listModeBtn==null)return;
        listModeBtn.setTextColor(calendarMode?muted:primary);listModeBtn.setBackground(round(calendarMode?Color.TRANSPARENT:Color.rgb(229,242,240),13));
        calendarModeBtn.setTextColor(calendarMode?primary:muted);calendarModeBtn.setBackground(round(calendarMode?Color.rgb(229,242,240):Color.TRANSPARENT,13));
    }

    private void render(){if(contentHost==null)return;contentHost.removeAllViews();if(calendarMode)renderCalendar();else renderList();}

    private void renderList(){
        List<EventStore.Event> events=EventStore.load(this);long now=System.currentTimeMillis();
        events.sort((a,b)->{
            if(a.pinned!=b.pinned)return a.pinned?-1:1;if(a.favorite!=b.favorite)return a.favorite?-1:1;
            return Long.compare(displayTime(a,now),displayTime(b,now));
        });
        boolean any=false,up=false,past=false;
        for(EventStore.Event e:events){long t=displayTime(e,now);if(t>=now&&matches(e,t)){if(!up){section(tr("القادمة","Upcoming"));up=true;}addCard(e,t);any=true;}}
        for(EventStore.Event e:events){long t=displayTime(e,now);if(!Recurrence.isRecurring(e)&&t<now&&matches(e,t)){if(!past){section(tr("السابقة","Past"));past=true;}addCard(e,t);any=true;}}
        if(!any){TextView empty=text(tr("لا توجد مناسبات مطابقة\nأضف مناسبة جديدة أو غيّر خيارات البحث","No matching events\nAdd a new event or change the filters"),16,true);empty.setTextColor(muted);empty.setGravity(Gravity.CENTER);empty.setPadding(dp(10),dp(55),dp(10),0);contentHost.addView(empty);}
    }

    private void renderCalendar(){
        LinearLayout nav=new LinearLayout(this);nav.setGravity(Gravity.CENTER_VERTICAL);nav.setPadding(dp(4),dp(6),dp(4),dp(8));contentHost.addView(nav);
        Button prev=chip("‹");Button next=chip("›");TextView month=text(monthTitle(),20,true);month.setTextColor(primary);month.setGravity(Gravity.CENTER);
        nav.addView(prev,new LinearLayout.LayoutParams(dp(48),dp(42)));nav.addView(month,new LinearLayout.LayoutParams(0,dp(42),1));nav.addView(next,new LinearLayout.LayoutParams(dp(48),dp(42)));
        prev.setOnClickListener(v->{shownMonth.add(Calendar.MONTH,-1);render();});next.setOnClickListener(v->{shownMonth.add(Calendar.MONTH,1);render();});

        GridLayout grid=new GridLayout(this);grid.setColumnCount(7);grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);contentHost.addView(grid,new LinearLayout.LayoutParams(-1,-2));
        String[] days=ar()?new String[]{"أح","إث","ثل","أر","خم","جم","سب"}:new String[]{"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        for(String d:days){TextView h=text(d,12,true);h.setTextColor(muted);h.setGravity(Gravity.CENTER);grid.addView(h,cellParams());}

        Calendar first=(Calendar)shownMonth.clone();first.set(Calendar.DAY_OF_MONTH,1);zeroTime(first);
        int offset=first.get(Calendar.DAY_OF_WEEK)-Calendar.SUNDAY;int max=first.getActualMaximum(Calendar.DAY_OF_MONTH);
        for(int i=0;i<offset;i++){TextView blank=new TextView(this);grid.addView(blank,cellParams());}
        for(int day=1;day<=max;day++){
            Calendar d=(Calendar)first.clone();d.set(Calendar.DAY_OF_MONTH,day);long start=d.getTimeInMillis();Calendar en=(Calendar)d.clone();en.add(Calendar.DAY_OF_MONTH,1);long end=en.getTimeInMillis();
            List<EventStore.Event> matches=eventsOnDay(start,end);String label=String.valueOf(day);if(!matches.isEmpty())label+="\n• "+matches.size();
            if(matches.stream().anyMatch(x->x.favorite))label+=" ⭐";
            TextView cell=text(label,13,!matches.isEmpty());cell.setGravity(Gravity.CENTER);cell.setPadding(dp(2),dp(7),dp(2),dp(7));
            int c=matches.isEmpty()?Color.WHITE:ColorPalette.soft(ColorPalette.color(this,matches.get(0)));cell.setBackground(round(c,12));
            LinearLayout.LayoutParams unused=null;grid.addView(cell,cellParams());
            final long selected=start;cell.setOnClickListener(v->{selectedDate=selected;dateFilter.setText(DateTools.gregorianShort(this,selected));calendarMode=false;updateModeButtons();render();});
        }
        TextView tip=text(tr("اضغط على أي يوم لعرض مناسباته في القائمة.","Tap a day to show its events in the list."),12,false);tip.setTextColor(muted);tip.setGravity(Gravity.CENTER);tip.setPadding(0,dp(12),0,dp(8));contentHost.addView(tip);
    }

    private GridLayout.LayoutParams cellParams(){GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(58);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(2),dp(2),dp(2),dp(2));return p;}
    private List<EventStore.Event> eventsOnDay(long start,long end){ArrayList<EventStore.Event> out=new ArrayList<>();for(EventStore.Event e:EventStore.load(this)){long t=Recurrence.firstOccurrenceBetween(e,start,end);if(t>=0&&matchesBasic(e,t))out.add(e);}return out;}
    private String monthTitle(){Locale l=ar()?new Locale("ar"):Locale.ENGLISH;return new SimpleDateFormat("MMMM yyyy",l).format(shownMonth.getTime());}

    private long displayTime(EventStore.Event e,long now){return Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,now):e.eventTime;}
    private boolean matches(EventStore.Event e,long t){return matchesBasic(e,t)&&matchesDate(e,t);}
    private boolean matchesBasic(EventStore.Event e,long t){
        int cat=categoryFilter==null?0:categoryFilter.getSelectedItemPosition();if(cat>0&&!Categories.CODES[cat-1].equals(e.category))return false;
        int day=dayFilter==null?0:dayFilter.getSelectedItemPosition();if(day>0&&DateTools.dayOfWeek(t)!=day)return false;
        String q=search==null?"":normalize(search.getText().toString());if(q.isEmpty())return true;
        String hay=e.title+" "+e.details+" "+e.locationName+" "+Categories.arabicLabel(e.category)+" "+Categories.englishLabel(e.category)+" "+DateTools.gregorian(this,t,true)+" "+DateTools.hijri(this,t)+" "+DateTools.dayArabic(t)+" "+DateTools.dayEnglish(t);
        return normalize(hay).contains(q);
    }
    private boolean matchesDate(EventStore.Event e,long t){
        if(selectedDate<=0)return true;Calendar a=Calendar.getInstance(),b=Calendar.getInstance();a.setTimeInMillis(selectedDate);b.setTimeInMillis(t);
        return a.get(Calendar.YEAR)==b.get(Calendar.YEAR)&&a.get(Calendar.DAY_OF_YEAR)==b.get(Calendar.DAY_OF_YEAR);
    }
    private String normalize(String s){String x=java.text.Normalizer.normalize(s==null?"":s,java.text.Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);return x.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ة','ه');}

    private void addCard(EventStore.Event e,long shownTime){
        int base=ColorPalette.color(this,e),soft=ColorPalette.soft(base);
        LinearLayout outer=new LinearLayout(this);outer.setOrientation(LinearLayout.HORIZONTAL);outer.setBackground(round(soft,20));outer.setPadding(0,0,0,0);contentHost.addView(outer,margin(-1,-2,0,0,0,dp(9)));
        View stripe=new View(this);stripe.setBackgroundColor(base);outer.addView(stripe,new LinearLayout.LayoutParams(dp(6),-1));
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(13),dp(11),dp(13),dp(10));outer.addView(card,new LinearLayout.LayoutParams(0,-2,1));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);
        TextView icon=text(Categories.icon(e.category),22,false);head.addView(icon,new LinearLayout.LayoutParams(dp(38),-2));
        LinearLayout names=new LinearLayout(this);names.setOrientation(LinearLayout.VERTICAL);head.addView(names,new LinearLayout.LayoutParams(0,-2,1));
        TextView n=text(e.title,18,true);n.setTextColor(Color.rgb(31,42,55));names.addView(n);
        TextView cat=text(Categories.label(this,e.category),12,true);cat.setTextColor(base);names.addView(cat);
        Button star=chip(e.favorite?"★":"☆");star.setTextColor(e.favorite?accent:muted);head.addView(star,new LinearLayout.LayoutParams(dp(43),dp(40)));star.setOnClickListener(v->{toggleFavorite(e);});
        Button pin=chip(e.pinned?"📌":"⌖");head.addView(pin,new LinearLayout.LayoutParams(dp(43),dp(40)));pin.setOnClickListener(v->{togglePinned(e);});

        TextView g=text("📅  "+DateTools.gregorian(this,shownTime,true),14,false);g.setTextColor(Color.rgb(55,65,81));g.setPadding(0,dp(7),0,0);card.addView(g);
        TextView h=text("☾  "+DateTools.hijri(this,shownTime),13,false);h.setTextColor(base);h.setPadding(0,dp(2),0,0);card.addView(h);
        if(Recurrence.isRecurring(e)){TextView rr=text("↻  "+tr("يتكرر ","Repeats ")+Recurrence.label(this,e.recurrence),12,true);rr.setTextColor(base);rr.setPadding(0,dp(3),0,0);card.addView(rr);}
        if(!e.locationName.trim().isEmpty()){TextView l=text("📍  "+e.locationName,13,false);l.setTextColor(muted);l.setPadding(0,dp(4),0,0);card.addView(l);}
        if(!e.details.trim().isEmpty()){TextView d=text(e.details,13,false);d.setTextColor(muted);d.setPadding(0,dp(6),0,0);card.addView(d);}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);actions.setPadding(0,dp(7),0,0);card.addView(actions);
        if(!e.attachmentUri.isEmpty()){Button a=chip("📎");actions.addView(a);a.setOnClickListener(v->openAttachment(e));}
        if(!e.locationUrl.isEmpty()||!e.locationName.isEmpty()){Button m=chip("📍");actions.addView(m);m.setOnClickListener(v->openLocation(e.locationUrl.isEmpty()?e.locationName:e.locationUrl));}
        Button edit=chip(tr("تعديل","Edit"));actions.addView(edit);edit.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
        Button del=chip(tr("حذف","Delete"));actions.addView(del);del.setOnClickListener(v->confirmDelete(e));
    }

    private void toggleFavorite(EventStore.Event e){List<EventStore.Event> all=EventStore.load(this);for(EventStore.Event x:all)if(x.id==e.id)x.favorite=!x.favorite;EventStore.save(this,all);render();}
    private void togglePinned(EventStore.Event e){List<EventStore.Event> all=EventStore.load(this);for(EventStore.Event x:all)if(x.id==e.id)x.pinned=!x.pinned;EventStore.save(this,all);render();}
    private void confirmDelete(EventStore.Event e){new AlertDialog.Builder(this).setTitle(tr("حذف المناسبة","Delete event")).setMessage(tr("هل تريد حذف «"+e.title+"»؟","Delete “"+e.title+"”?" )).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حذف","Delete"),(d,w)->{ReminderScheduler.cancel(this,e.id);List<EventStore.Event> all=EventStore.load(this);all.removeIf(x->x.id==e.id);EventStore.save(this,all);render();}).show();}

    private void showSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(3),dp(18),0);box.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        box.addView(fieldLabel(tr("لغة البرنامج","App language")));Spinner lang=new Spinner(this);lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"العربية","English"}));lang.setSelection(ar()?0:1);box.addView(lang);
        box.addView(fieldLabel(tr("تعديل التاريخ الهجري","Hijri date adjustment")));Spinner offset=new Spinner(this);String[] opts=ar()?new String[]{"بدون تعديل","-1 يوم","-2 يوم","+1 يوم","+2 يوم"}:new String[]{"No adjustment","-1 day","-2 days","+1 day","+2 days"};int[] vals={0,-1,-2,1,2};offset.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));int cur=AppSettings.hijriOffset(this),pos=0;for(int i=0;i<vals.length;i++)if(vals[i]==cur)pos=i;offset.setSelection(pos);box.addView(offset);
        Button colors=outlineButton(tr("🎨  ألوان الفئات","🎨  Category colors"));box.addView(colors,margin(-1,dp(48),0,dp(12),0,0));colors.setOnClickListener(v->showCategoryColors());
        Button data=outlineButton(tr("☁  النسخ الاحتياطي والاستيراد","☁  Backup & import"));box.addView(data,margin(-1,dp(48),0,dp(7),0,0));data.setOnClickListener(v->showDataTools());
        new AlertDialog.Builder(this).setTitle(tr("الإعدادات","Settings")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{AppSettings.setLanguage(this,lang.getSelectedItemPosition()==0?"ar":"en");AppSettings.setHijriOffset(this,vals[offset.getSelectedItemPosition()]);recreate();}).show();
    }

    private void showCategoryColors(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(5),dp(18),0);
        Spinner cat=new Spinner(this);cat.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,false)));box.addView(cat);
        Spinner col=new Spinner(this);col.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ColorPalette.labels(this,false)));box.addView(col);
        cat.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){String h=ColorPalette.categoryHex(MainActivity.this,Categories.CODES[pos]);col.setSelection(ColorPalette.indexOf(h));}public void onNothingSelected(AdapterView<?> p){}});
        new AlertDialog.Builder(this).setTitle(tr("لون الفئة","Category color")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{ColorPalette.setCategoryHex(this,Categories.CODES[cat.getSelectedItemPosition()],ColorPalette.HEX[col.getSelectedItemPosition()]);render();}).show();
    }

    private void showDataTools(){
        String[] items=ar()?new String[]{"نسخة احتياطية كاملة (مع المرفقات)","استعادة نسخة احتياطية","تصدير JSON","استيراد JSON"}:new String[]{"Full backup (with attachments)","Restore backup","Export JSON","Import JSON"};
        new AlertDialog.Builder(this).setTitle(tr("البيانات والنسخ الاحتياطي","Data & backup")).setItems(items,(d,which)->{
            if(which==0)createDocument("application/zip","Munasabati_Backup_"+dateStamp()+".munasabati",REQ_BACKUP);
            else if(which==1)openDocument(new String[]{"application/zip","application/octet-stream","*/*"},REQ_RESTORE);
            else if(which==2)createDocument("application/json","Munasabati_Events_"+dateStamp()+".json",REQ_EXPORT_JSON);
            else openDocument(new String[]{"application/json","text/plain","*/*"},REQ_IMPORT_JSON);
        }).show();
    }
    private void createDocument(String type,String name,int req){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.putExtra(Intent.EXTRA_TITLE,name);startActivityForResult(i,req);}
    private void openDocument(String[] types,int req){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,types);startActivityForResult(i,req);}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null)return;Uri uri=data.getData();
        try{
            if(request==REQ_BACKUP){try(OutputStream out=getContentResolver().openOutputStream(uri)){int n=BackupManager.createBackup(this,out);toast(tr("تم إنشاء النسخة الاحتياطية بنجاح. المرفقات: ","Backup created successfully. Attachments: ")+n);}}
            else if(request==REQ_RESTORE){try(InputStream in=getContentResolver().openInputStream(uri)){int n=BackupManager.restoreBackup(this,in);toast(tr("تمت استعادة ","Restored ")+n+tr(" مناسبة"," events"));recreate();}}
            else if(request==REQ_EXPORT_JSON){try(OutputStream out=getContentResolver().openOutputStream(uri)){out.write(EventStore.exportJson(this,true).toString(2).getBytes("UTF-8"));toast(tr("تم تصدير ملف JSON","JSON exported"));}}
            else if(request==REQ_IMPORT_JSON){try(InputStream in=getContentResolver().openInputStream(uri)){String raw=readAll(in);int n=EventStore.importJson(this,new JSONObject(raw));toast(tr("تم استيراد ","Imported ")+n+tr(" مناسبة"," events"));recreate();}}
        }catch(Exception ex){new AlertDialog.Builder(this).setTitle(tr("تعذر إكمال العملية","Operation failed")).setMessage(ex.getMessage()==null?ex.toString():ex.getMessage()).setPositiveButton("OK",null).show();}
    }

    private void pickFilterDate(){Calendar c=Calendar.getInstance();if(selectedDate>0)c.setTimeInMillis(selectedDate);new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.set(y,m,d,0,0,0);n.set(Calendar.MILLISECOND,0);selectedDate=n.getTimeInMillis();dateFilter.setText(DateTools.gregorianShort(this,selectedDate));render();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();}
    private String[] dayLabels(){return ar()?new String[]{"كل الأيام","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"}:new String[]{"All days","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};}
    private void section(String s){TextView t=text(s,19,true);t.setTextColor(primary);t.setPadding(dp(2),dp(8),dp(2),dp(6));contentHost.addView(t);}
    private void openAttachment(EventStore.Event e){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(e.attachmentUri),e.attachmentType.isEmpty()?"*/*":e.attachmentType);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception ex){toast(tr("تعذر فتح المرفق","Could not open attachment"));}}
    private void openLocation(String raw){try{Uri u=raw.startsWith("http")||raw.startsWith("geo:")?Uri.parse(raw):Uri.parse("geo:0,0?q="+Uri.encode(raw));startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){toast(tr("تعذر فتح الموقع","Could not open location"));}}
    private String dateStamp(){return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date());}
    private String readAll(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1)b.write(x,0,n);return b.toString("UTF-8");}
    private void zeroTime(Calendar c){c.set(Calendar.HOUR_OF_DAY,0);c.set(Calendar.MINUTE,0);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    private TextView fieldLabel(String s){TextView t=text(s,14,true);t.setTextColor(Color.rgb(55,65,81));t.setPadding(0,dp(10),0,dp(3));return t;}
    private Button chip(String s){return button(s,primary,Color.argb(22,25,91,86),12);}
    private Button outlineButton(String s){Button b=button(s,primary,Color.WHITE,13);GradientDrawable g=round(Color.WHITE,12);g.setStroke(dp(1),Color.rgb(214,222,229));b.setBackground(g);return b;}
    private Button button(String s,int textColor,int back,int size){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(textColor);b.setTextSize(size);b.setPadding(dp(8),0,dp(8),0);b.setMinWidth(0);b.setMinHeight(0);b.setBackground(round(back,13));return b;}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.BLACK);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams margin(int w,int h,int l,int t,int r,int b){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(w,h);p.setMargins(l,t,r,b);return p;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
