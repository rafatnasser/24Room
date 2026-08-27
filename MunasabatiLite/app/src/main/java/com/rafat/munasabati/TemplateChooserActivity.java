package com.rafat.munasabati;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.*;
import android.widget.*;

public class TemplateChooserActivity extends Activity{
    private int primary,accent=Color.rgb(208,151,56);private LinearLayout root;private GridLayout grid;
    private boolean ar(){return AppSettings.isArabic(this);}private String tr(String a,String e){return AppSettings.tr(this,a,e);}private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}private GradientDrawable bg(int c,int r){GradientDrawable g=new GradientDrawable();g.setColor(c);g.setCornerRadius(dp(r));return g;}

    @Override public void onCreate(Bundle b){
        super.onCreate(b);V4Theme.apply(this);primary=V4Theme.primary(this);getWindow().setStatusBarColor(primary);
        ScrollView s=new ScrollView(this);s.setFillViewport(true);s.setBackgroundColor(ModernUi.screenBackground(this));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);s.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(17),dp(15),dp(17),dp(17));hero.setBackground(bg(primary,26));hero.setElevation(dp(5));root.addView(hero,new LinearLayout.LayoutParams(-1,-2));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=new Button(this);back.setAllCaps(false);back.setText("‹");back.setTextSize(25);back.setTextColor(Color.WHITE);back.setBackground(bg(Color.argb(28,255,255,255),14));top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(44)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,-2,1);tlp.setMargins(dp(10),0,0,0);top.addView(titles,tlp);
        TextView h=t(tr("ابدأ بقالب ذكي","Start with a smart template"),26,true);h.setTextColor(Color.WHITE);titles.addView(h);
        TextView sub=t(tr("اختصر الوقت ثم عدّل ما تحتاجه فقط","Save time, then customize only what you need"),13,false);sub.setTextColor(Color.rgb(222,239,236));titles.addView(sub);
        TextView tip=t(tr("✦ القالب يضبط الفئة والتكرار والتنبيهات تلقائيًا","✦ Templates preconfigure category, recurrence and reminders"),11,true);tip.setTextColor(Color.rgb(255,239,191));tip.setPadding(0,dp(10),0,0);hero.addView(tip);

        TextView section=t(tr("اختر نوع المناسبة","Choose event type"),18,true);section.setTextColor(ModernUi.ink(this));section.setPadding(dp(2),dp(18),dp(2),dp(9));root.addView(section);
        grid=new GridLayout(this);grid.setColumnCount(2);grid.setAlignmentMode(GridLayout.ALIGN_BOUNDS);root.addView(grid,new LinearLayout.LayoutParams(-1,-2));

        add("＋",tr("مناسبة فارغة","Blank event"),tr("ابدأ من الصفر","Start from scratch"),"none","none","30");
        add("🎂",tr("عيد ميلاد","Birthday"),tr("سنوي • أسبوع + يوم","Yearly • week + day"),"birthday","yearly","10080,1440");
        add("💍",tr("زواج","Wedding"),tr("تنبيهات قبل الموعد","Helpful reminders"),"marriage","none","1440,180");
        add("✈",tr("سفر","Travel"),tr("يوم + 3 ساعات","Day + 3 hours"),"travel","none","1440,180");
        add("💼",tr("اجتماع","Meeting"),tr("ساعة + 15 دقيقة","Hour + 15 min"),"meeting","none","60,15");
        add("🌙",tr("عشاء","Dinner"),tr("تنبيهات اجتماعية","Social reminders"),"dinner","none","180,60");
        add("🩺",tr("موعد طبي","Medical appointment"),tr("يوم + ساعة","Day + hour"),"medical","none","1440,60");
        setContentView(s);
    }

    private void add(String icon,String name,String subtitle,String cat,String rec,String rem){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setGravity(Gravity.CENTER_HORIZONTAL);card.setPadding(dp(13),dp(13),dp(13),dp(12));card.setBackground(bg(ModernUi.surface(this),22));card.setElevation(dp(2));card.setClickable(true);card.setFocusable(true);
        TextView i=t(icon,28,false);i.setGravity(Gravity.CENTER);i.setBackground(bg(Color.rgb(255,248,229),17));i.setTextColor(accent);card.addView(i,new LinearLayout.LayoutParams(dp(58),dp(58)));
        TextView n=t(name,15,true);n.setTextColor(ModernUi.ink(this));n.setGravity(Gravity.CENTER);n.setPadding(0,dp(9),0,0);card.addView(n);
        TextView d=t(subtitle,11,false);d.setTextColor(ModernUi.muted(this));d.setGravity(Gravity.CENTER);d.setMaxLines(2);d.setPadding(0,dp(3),0,0);card.addView(d);
        GridLayout.LayoutParams p=new GridLayout.LayoutParams();p.width=0;p.height=dp(145);p.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);p.setMargins(dp(4),dp(4),dp(4),dp(4));grid.addView(card,p);
        card.setOnClickListener(v->{Intent intent=new Intent(this,EditEventActivity.class);intent.putExtra("template_category",cat);intent.putExtra("template_recurrence",rec);intent.putExtra("template_reminders",rem);intent.putExtra("template_name",name);startActivity(intent);finish();});
    }
    private TextView t(String x,int s,boolean bold){TextView v=new TextView(this);v.setText(x);v.setTextSize(s);if(bold)v.setTypeface(null,Typeface.BOLD);return v;}
}
