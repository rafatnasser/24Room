package com.rafat.munasabati;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.text.Normalizer;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout list;
    private EditText search;
    private Spinner categoryFilter, dayFilter;
    private Button dateFilter;
    private long selectedDate = 0L;
    private final int primary = Color.rgb(25, 91, 86);
    private final int accent = Color.rgb(208, 151, 56);
    private final int bg = Color.rgb(246, 248, 251);
    private final int muted = Color.rgb(91, 101, 115);

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(primary);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
    }
    @Override protected void onResume() { super.onResume(); reload(); }

    private String tr(String ar, String en){ return AppSettings.tr(this, ar, en); }
    private boolean ar(){ return AppSettings.isArabic(this); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(12),dp(14),dp(12));
        root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR); root.setBackgroundColor(bg);

        LinearLayout hero = new LinearLayout(this); hero.setOrientation(LinearLayout.VERTICAL); hero.setPadding(dp(18),dp(14),dp(18),dp(15)); hero.setBackground(round(primary,22));
        LinearLayout.LayoutParams hp=lp(-1,-2); hp.setMargins(0,0,0,dp(12)); root.addView(hero,hp);
        LinearLayout top = new LinearLayout(this); top.setGravity(Gravity.CENTER_VERTICAL); hero.addView(top,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout titles=new LinearLayout(this); titles.setOrientation(LinearLayout.VERTICAL); top.addView(titles,new LinearLayout.LayoutParams(0,-2,1));
        TextView title=text(tr("مناسباتي","Munasabati"),28,true); title.setTextColor(Color.WHITE); titles.addView(title);
        TextView sub=text(tr("مواعيدك، ذكرياتك، وتنبيهاتك في مكان واحد","Events, memories and reminders in one place"),13,false); sub.setTextColor(Color.rgb(224,239,237)); titles.addView(sub);
        Button settings=smallButton("⚙",Color.WHITE,primary); settings.setTextSize(20); top.addView(settings,new LinearLayout.LayoutParams(dp(48),dp(44))); settings.setOnClickListener(v->showSettings());

        Button add = new Button(this); add.setAllCaps(false); add.setText(tr("＋  إضافة مناسبة جديدة","＋  Add new event")); add.setTextSize(16); add.setTypeface(null,Typeface.BOLD); add.setTextColor(primary); add.setBackground(round(Color.WHITE,16));
        LinearLayout.LayoutParams ap=lp(-1,dp(52)); ap.setMargins(0,dp(14),0,0); hero.addView(add,ap); add.setOnClickListener(v->startActivity(new Intent(this, EditEventActivity.class)));

        LinearLayout searchCard=new LinearLayout(this); searchCard.setOrientation(LinearLayout.VERTICAL); searchCard.setPadding(dp(12),dp(10),dp(12),dp(10)); searchCard.setBackground(round(Color.WHITE,20));
        LinearLayout.LayoutParams scp=lp(-1,-2); scp.setMargins(0,0,0,dp(10)); root.addView(searchCard,scp);
        search=new EditText(this); search.setSingleLine(true); search.setTextSize(15); search.setHint(tr("🔎  ابحث بالاسم، الموقع، التاريخ...","🔎  Search name, location, date...")); search.setBackground(round(Color.rgb(244,247,249),14)); search.setPadding(dp(12),0,dp(12),0); searchCard.addView(search,new LinearLayout.LayoutParams(-1,dp(48)));
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int b,int c){reload();} public void afterTextChanged(Editable e){}});

        LinearLayout filters=new LinearLayout(this); filters.setPadding(0,dp(8),0,0); searchCard.addView(filters,new LinearLayout.LayoutParams(-1,-2));
        categoryFilter=new Spinner(this); categoryFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,Categories.labels(this,true))); filters.addView(categoryFilter,new LinearLayout.LayoutParams(0,dp(48),1));
        dayFilter=new Spinner(this); dayFilter.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,dayLabels())); filters.addView(dayFilter,new LinearLayout.LayoutParams(0,dp(48),1));
        AdapterView.OnItemSelectedListener lis=new AdapterView.OnItemSelectedListener(){public void onItemSelected(AdapterView<?> p,View v,int pos,long id){reload();}public void onNothingSelected(AdapterView<?> p){}};
        categoryFilter.setOnItemSelectedListener(lis); dayFilter.setOnItemSelectedListener(lis);

        LinearLayout filters2=new LinearLayout(this); filters2.setPadding(0,dp(4),0,0); searchCard.addView(filters2,new LinearLayout.LayoutParams(-1,-2));
        dateFilter=outlineButton(tr("📅  كل التواريخ","📅  All dates")); filters2.addView(dateFilter,new LinearLayout.LayoutParams(0,dp(46),1)); dateFilter.setOnClickListener(v->pickFilterDate());
        Button clear=outlineButton(tr("مسح الفلاتر","Clear filters")); LinearLayout.LayoutParams clp=new LinearLayout.LayoutParams(0,dp(46),1); clp.setMargins(dp(6),0,0,0); filters2.addView(clear,clp);
        clear.setOnClickListener(v->{search.setText("");categoryFilter.setSelection(0);dayFilter.setSelection(0);selectedDate=0;dateFilter.setText(tr("📅  كل التواريخ","📅  All dates"));reload();});

        ScrollView scroll=new ScrollView(this); list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR); scroll.addView(list,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }

    private String[] dayLabels(){
        return ar()?new String[]{"كل الأيام","الأحد","الاثنين","الثلاثاء","الأربعاء","الخميس","الجمعة","السبت"}:new String[]{"All days","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday","Saturday"};
    }

    private void pickFilterDate(){
        Calendar c=Calendar.getInstance(); if(selectedDate>0)c.setTimeInMillis(selectedDate);
        new DatePickerDialog(this,(v,y,m,d)->{Calendar n=Calendar.getInstance();n.set(y,m,d,12,0,0);n.set(Calendar.MILLISECOND,0);selectedDate=n.getTimeInMillis();dateFilter.setText(DateTools.gregorianShort(this,selectedDate));reload();},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void reload(){
        if(list==null)return; list.removeAllViews(); List<EventStore.Event> events=EventStore.load(this); long now=System.currentTimeMillis();
        events.sort((a,b)->Long.compare(displayTime(a,now),displayTime(b,now)));
        boolean any=false,upHead=false,pastHead=false;
        for(EventStore.Event e:events){ long t=displayTime(e,now); if(t>=now && matches(e,t)){ if(!upHead){section(tr("القادمة","Upcoming"));upHead=true;}addCard(e,t);any=true; } }
        for(int i=events.size()-1;i>=0;i--){EventStore.Event e=events.get(i);long t=displayTime(e,now);if(!Recurrence.isRecurring(e)&&t<now&&matches(e,t)){if(!pastHead){section(tr("السابقة","Past"));pastHead=true;}addCard(e,t);any=true;}}
        if(!any){TextView empty=text(tr("لا توجد نتائج مطابقة\nجرّب تغيير كلمات البحث أو الفلاتر","No matching events\nTry changing the search or filters"),17,true);empty.setGravity(Gravity.CENTER);empty.setTextColor(muted);empty.setPadding(dp(10),dp(70),dp(10),0);list.addView(empty);}
    }

    private long displayTime(EventStore.Event e,long now){ return Recurrence.isRecurring(e)?Recurrence.nextOccurrence(e,now):e.eventTime; }

    private boolean matches(EventStore.Event e,long shownTime){
        int catPos=categoryFilter==null?0:categoryFilter.getSelectedItemPosition(); if(catPos>0&&!Categories.CODES[catPos-1].equals(e.category))return false;
        int dayPos=dayFilter==null?0:dayFilter.getSelectedItemPosition(); if(dayPos>0 && DateTools.dayOfWeek(shownTime)!=dayPos)return false;
        if(selectedDate>0){Calendar a=Calendar.getInstance(),b=Calendar.getInstance();a.setTimeInMillis(selectedDate);b.setTimeInMillis(shownTime);if(a.get(Calendar.YEAR)!=b.get(Calendar.YEAR)||a.get(Calendar.DAY_OF_YEAR)!=b.get(Calendar.DAY_OF_YEAR))return false;}
        String q=search==null?"":normalize(search.getText().toString()); if(q.isEmpty())return true;
        String hay=e.title+" "+e.details+" "+e.locationName+" "+e.locationUrl+" "+Categories.arabicLabel(e.category)+" "+Categories.englishLabel(e.category)+" "+Recurrence.label(this,e.recurrence)+" "+DateTools.gregorian(this,shownTime,true)+" "+DateTools.hijri(this,shownTime)+" "+DateTools.hijriNumeric(this,shownTime)+" "+DateTools.dayArabic(shownTime)+" "+DateTools.dayEnglish(shownTime);
        return normalize(hay).contains(q);
    }
    private String normalize(String s){String x=Normalizer.normalize(s==null?"":s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);return x.replace('أ','ا').replace('إ','ا').replace('آ','ا').replace('ى','ي').replace('ة','ه');}

    private void section(String s){TextView t=text(s,20,true);t.setTextColor(primary);t.setPadding(dp(2),dp(10),dp(2),dp(7));list.addView(t);}

    private void addCard(EventStore.Event e,long shownTime){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(15),dp(13),dp(15),dp(12));card.setBackground(round(Color.WHITE,20));LinearLayout.LayoutParams cp=lp(-1,-2);cp.setMargins(0,0,0,dp(10));list.addView(card,cp);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head,new LinearLayout.LayoutParams(-1,-2));
        TextView n=text(e.title,19,true);n.setTextColor(Color.rgb(31,42,55));head.addView(n,new LinearLayout.LayoutParams(0,-2,1));
        TextView cat=text(Categories.label(this,e.category),12,true);cat.setTextColor(primary);cat.setGravity(Gravity.CENTER);cat.setPadding(dp(10),dp(5),dp(10),dp(5));cat.setBackground(round(Color.rgb(229,242,240),20));head.addView(cat);
        TextView g=text("📅  "+DateTools.gregorian(this,shownTime,true),14,false);g.setTextColor(Color.rgb(55,65,81));g.setPadding(0,dp(8),0,0);card.addView(g);
        TextView h=text("☾  "+DateTools.hijri(this,shownTime),14,false);h.setTextColor(accent);h.setPadding(0,dp(3),0,0);card.addView(h);
        if(Recurrence.isRecurring(e)){TextView r=text("↻  "+tr("يتكرر ","Repeats ")+Recurrence.label(this,e.recurrence),13,true);r.setTextColor(primary);r.setPadding(0,dp(4),0,0);card.addView(r);}
        if(!e.locationName.trim().isEmpty()){TextView l=text("📍  "+e.locationName,14,false);l.setTextColor(muted);l.setPadding(0,dp(5),0,0);card.addView(l);}
        if(!e.details.trim().isEmpty()){TextView x=text(e.details,14,false);x.setTextColor(muted);x.setPadding(0,dp(8),0,0);card.addView(x);}
        LinearLayout actions=new LinearLayout(this);actions.setGravity(Gravity.END);actions.setPadding(0,dp(10),0,0);card.addView(actions);
        if(!e.attachmentUri.isEmpty()){Button a=chip(tr("📎 مرفق","📎 Attachment"));actions.addView(a);a.setOnClickListener(v->openAttachment(e));}
        if(!e.locationUrl.isEmpty()||!e.locationName.isEmpty()){Button m=chip(tr("📍 الموقع","📍 Location"));actions.addView(m);m.setOnClickListener(v->openLocation(e.locationUrl.isEmpty()?e.locationName:e.locationUrl));}
        Button edit=chip(tr("تعديل","Edit"));actions.addView(edit);edit.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
        Button del=chip(tr("حذف","Delete"));actions.addView(del);del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle(tr("حذف المناسبة","Delete event")).setMessage(tr("هل تريد حذف «"+e.title+"»؟","Delete “"+e.title+"”?" )).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حذف","Delete"),(x,w)->{ReminderScheduler.cancel(this,e.id);List<EventStore.Event> all=EventStore.load(this);all.removeIf(q->q.id==e.id);EventStore.save(this,all);reload();}).show());
    }

    private void showSettings(){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(18),dp(4),dp(18),0);box.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);
        TextView l1=text(tr("لغة البرنامج","App language"),15,true);box.addView(l1);Spinner lang=new Spinner(this);lang.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"العربية","English"}));lang.setSelection(ar()?0:1);box.addView(lang);
        TextView l2=text(tr("تعديل التاريخ الهجري بالأيام","Hijri date day adjustment"),15,true);l2.setPadding(0,dp(12),0,0);box.addView(l2);Spinner offset=new Spinner(this);String[] opts=ar()?new String[]{"بدون تعديل","-1 يوم","-2 يوم","+1 يوم","+2 يوم"}:new String[]{"No adjustment","-1 day","-2 days","+1 day","+2 days"};int[] vals={0,-1,-2,1,2};offset.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,opts));int current=AppSettings.hijriOffset(this),pos=0;for(int i=0;i<vals.length;i++)if(vals[i]==current)pos=i;offset.setSelection(pos);box.addView(offset);
        new AlertDialog.Builder(this).setTitle(tr("الإعدادات","Settings")).setView(box).setNegativeButton(tr("إلغاء","Cancel"),null).setPositiveButton(tr("حفظ","Save"),(d,w)->{AppSettings.setLanguage(this,lang.getSelectedItemPosition()==0?"ar":"en");AppSettings.setHijriOffset(this,vals[offset.getSelectedItemPosition()]);recreate();}).show();
    }

    private void openAttachment(EventStore.Event e){try{Intent i=new Intent(Intent.ACTION_VIEW);i.setDataAndType(Uri.parse(e.attachmentUri),e.attachmentType.isEmpty()?"*/*":e.attachmentType);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(i);}catch(Exception ex){Toast.makeText(this,tr("لا يوجد تطبيق مناسب لفتح المرفق","No suitable app found to open the attachment"),Toast.LENGTH_SHORT).show();}}
    private void openLocation(String raw){try{Uri u=raw.startsWith("http")||raw.startsWith("geo:")?Uri.parse(raw):Uri.parse("geo:0,0?q="+Uri.encode(raw));startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){Toast.makeText(this,tr("تعذر فتح الموقع","Could not open location"),Toast.LENGTH_SHORT).show();}}

    private Button chip(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(12);b.setTextColor(primary);b.setBackground(round(Color.rgb(240,246,245),14));b.setMinHeight(0);b.setMinWidth(0);b.setPadding(dp(9),0,dp(9),0);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(38));p.setMargins(dp(3),0,dp(3),0);b.setLayoutParams(p);return b;}
    private Button outlineButton(String s){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextSize(13);b.setTextColor(primary);GradientDrawable g=round(Color.WHITE,12);g.setStroke(dp(1),Color.rgb(214,222,229));b.setBackground(g);return b;}
    private Button smallButton(String s,int textColor,int background){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(textColor);b.setBackground(round(background,14));b.setPadding(0,0,0,0);return b;}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(z);t.setTextColor(Color.BLACK);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(r));return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
