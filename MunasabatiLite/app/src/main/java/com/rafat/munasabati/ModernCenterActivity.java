package com.rafat.munasabati;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** Modern card-grid front end for the existing v4 tools and diagnostics. */
public class ModernCenterActivity extends V4CenterActivity {
    private GridLayout menu;
    private int accent=Color.rgb(208,151,56);

    @Override public void onCreate(Bundle b){
        // Intentionally do not call V4CenterActivity.onCreate(): we reuse its feature methods, not its old list UI.
        V4Theme.apply(this);primary=V4Theme.primary(this);getWindow().setStatusBarColor(primary);
        ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(ModernUi.screenBackground(this));
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(14),dp(12),dp(14),dp(28));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);

        LinearLayout hero=new LinearLayout(this);hero.setOrientation(LinearLayout.VERTICAL);hero.setPadding(dp(17),dp(15),dp(17),dp(17));hero.setBackground(bg(primary,27));hero.setElevation(dp(5));root.addView(hero,p(-1,-2,0,0,0,dp(13)));
        LinearLayout top=new LinearLayout(this);top.setGravity(Gravity.CENTER_VERTICAL);hero.addView(top);
        Button back=new Button(this);back.setAllCaps(false);back.setText("‹");back.setTextSize(25);back.setTextColor(Color.WHITE);back.setBackground(bg(Color.argb(28,255,255,255),14));top.addView(back,new LinearLayout.LayoutParams(dp(46),dp(44)));back.setOnClickListener(v->finish());
        LinearLayout titles=new LinearLayout(this);titles.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(0,-2,1);tp.setMargins(dp(10),0,0,0);top.addView(titles,tp);
        TextView h=t(tr("مركز التجربة","Experience Center"),27,true);h.setTextColor(Color.WHITE);titles.addView(h);
        TextView sub=t(tr("كل أدوات مناسبـاتي في مكان واحد","All Munasabati tools in one calm space"),13,false);sub.setTextColor(Color.rgb(222,239,236));titles.addView(sub);
        TextView badge=t(tr("✦ صحة • تنظيم • خصوصية • تخصيص","✦ Health • Organize • Privacy • Personalize"),11,true);badge.setTextColor(Color.rgb(255,239,191));badge.setPadding(0,dp(10),0,0);hero.addView(badge);

        TextView choose=t(tr("ماذا تريد أن تفعل؟","What would you like to do?"),18,true);choose.setTextColor(ModernUi.ink(this));choose.setPadding(dp(2),dp(5),dp(2),dp(8));root.addView(choose);
        menu=new GridLayout(this);menu.setColumnCount(2);menu.setAlignmentMode(GridLayout.ALIGN_BOUNDS);root.addView(menu,new LinearLayout.LayoutParams(-1,-2));

        tile("🩺",tr("صحة التطبيق","App health"),tr("الإشعارات والبطارية والخدمات","Notifications, battery & services"),this::health);
        tile("📊",tr("الإحصائيات","Dashboard"),tr("ملخص بصري لمناسباتك","A visual snapshot of your events"),this::dashboard);
        tile("🔎",tr("البحث الذكي","Smart search"),tr("ابحث بعبارات طبيعية","Search with natural phrases"),this::smartSearch);
        tile("🗺",tr("خريطة المناسبات","Events map"),tr("شاهد مواقع مناسباتك","See event locations"),this::map);
        tile("🗑",tr("سلة المحذوفات","Trash"),tr("استعادة خلال 30 يومًا","Restore within 30 days"),this::trash);
        tile("🕘",tr("السجل","History"),tr("آخر التعديلات والأنشطة","Recent changes & activity"),this::history);
        tile("🎨",tr("المظهر والثيمات","Themes"),tr("ألوان ووضع فاتح أو داكن","Colors, light & dark modes"),this::themes);
        tile("🔐",tr("الخصوصية والقفل","Privacy"),tr("بصمة، وجه أو PIN","Biometrics, face or PIN"),this::security);
        tile("↗",tr("المشاركة المتقدمة","Advanced sharing"),tr("بطاقة، QR وملف ICS","Card, QR and ICS"),this::share);
        tile("▦",tr("Widgets","Widgets"),tr("اختصارات جميلة للشاشة","Beautiful home shortcuts"),this::widgets);
        setContentView(scroll);
    }

    private void tile(String icon,String title,String subtitle,Runnable action){
        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(13),dp(13),dp(13),dp(12));card.setBackground(bg(ModernUi.surface(this),22));card.setElevation(dp(2));card.setClickable(true);card.setFocusable(true);
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);card.addView(head);
        TextView i=t(icon,25,false);i.setGravity(Gravity.CENTER);i.setTextColor(accent);i.setBackground(bg(Color.rgb(255,248,229),16));head.addView(i,new LinearLayout.LayoutParams(dp(54),dp(54)));
        TextView arrow=t(ar()?"‹":"›",20,true);arrow.setTextColor(ModernUi.muted(this));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(0,-2,1);arrow.setGravity(ar()?Gravity.START:Gravity.END);head.addView(arrow,ap);
        TextView name=t(title,15,true);name.setTextColor(ModernUi.ink(this));name.setPadding(0,dp(9),0,0);card.addView(name);
        TextView desc=t(subtitle,11,false);desc.setTextColor(ModernUi.muted(this));desc.setMaxLines(2);desc.setPadding(0,dp(3),0,0);card.addView(desc);
        GridLayout.LayoutParams gp=new GridLayout.LayoutParams();gp.width=0;gp.height=dp(142);gp.columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f);gp.setMargins(dp(4),dp(4),dp(4),dp(4));menu.addView(card,gp);card.setOnClickListener(v->action.run());
    }
}
