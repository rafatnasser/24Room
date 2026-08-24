package com.rafat.munasabati;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private LinearLayout list;
    private final int green = Color.rgb(27,94,32);
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.WHITE);
        if (Build.VERSION.SDK_INT >= 23) getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 90);
    }
    @Override protected void onResume() { super.onResume(); reload(); }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(16),dp(16),dp(16),dp(16)); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); root.setBackgroundColor(Color.rgb(246,247,248));
        TextView title = text("مناسباتي", 28, true); title.setTextColor(green); root.addView(title);
        TextView sub = text("كل مواعيدك في مكان واحد", 14, false); sub.setTextColor(Color.DKGRAY); root.addView(sub);
        Button add = new Button(this); add.setText("+  إضافة مناسبة"); add.setTextSize(17); add.setTextColor(Color.WHITE); add.setBackground(round(green,18)); LinearLayout.LayoutParams ap = lp(-1,dp(52)); ap.setMargins(0,dp(14),0,dp(10)); root.addView(add,ap);
        add.setOnClickListener(v -> startActivity(new Intent(this, EditEventActivity.class)));
        ScrollView scroll = new ScrollView(this); list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); scroll.addView(list,new ScrollView.LayoutParams(-1,-2)); root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }

    private void reload() {
        if (list == null) return; list.removeAllViews(); List<EventStore.Event> events = EventStore.load(this); long now=System.currentTimeMillis();
        if (events.isEmpty()) { TextView empty=text("لا توجد مناسبات محفوظة\nاضغط «إضافة مناسبة» لحفظ أول موعد لك",18,true); empty.setGravity(Gravity.CENTER); empty.setPadding(0,dp(80),0,0); list.addView(empty); return; }
        boolean head=false;
        for(EventStore.Event e:events) if(e.eventTime>=now){ if(!head){section("القادمة"); head=true;} addCard(e); }
        head=false;
        for(int i=events.size()-1;i>=0;i--){EventStore.Event e=events.get(i); if(e.eventTime<now){ if(!head){section("السابقة"); head=true;} addCard(e);} }
    }
    private void section(String s){ TextView t=text(s,20,true); t.setTextColor(green); t.setPadding(0,dp(14),0,dp(6)); list.addView(t); }
    private void addCard(EventStore.Event e){
        LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(12),dp(14),dp(12)); card.setBackground(round(Color.WHITE,18)); LinearLayout.LayoutParams cp=lp(-1,-2); cp.setMargins(0,0,0,dp(10)); list.addView(card,cp);
        TextView n=text(e.title,19,true); card.addView(n); TextView d=text(format(e.eventTime),15,false); d.setTextColor(Color.DKGRAY); card.addView(d);
        if(!e.details.trim().isEmpty()){TextView x=text(e.details,14,false); x.setPadding(0,dp(8),0,0); card.addView(x);}
        LinearLayout actions=new LinearLayout(this); actions.setGravity(Gravity.END); actions.setPadding(0,dp(8),0,0); card.addView(actions);
        if(!e.attachmentUri.isEmpty()){Button a=small("المرفق"); actions.addView(a); a.setOnClickListener(v->openAttachment(e));}
        if(!e.locationUrl.isEmpty()){Button m=small("الموقع"); actions.addView(m); m.setOnClickListener(v->openLocation(e.locationUrl));}
        Button edit=small("تعديل"); actions.addView(edit); edit.setOnClickListener(v->startActivity(new Intent(this,EditEventActivity.class).putExtra("event_id",e.id)));
        Button del=small("حذف"); actions.addView(del); del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف المناسبة").setMessage("هل تريد حذف «"+e.title+"»؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(x,w)->{ReminderScheduler.cancel(this,e.id); List<EventStore.Event> all=EventStore.load(this); all.removeIf(q->q.id==e.id); EventStore.save(this,all); reload();}).show());
    }
    private void openAttachment(EventStore.Event e){ try{Intent i=new Intent(Intent.ACTION_VIEW); i.setDataAndType(Uri.parse(e.attachmentUri),e.attachmentType.isEmpty()?"*/*":e.attachmentType); i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); startActivity(i);}catch(Exception ex){Toast.makeText(this,"لا يوجد تطبيق مناسب لفتح المرفق",Toast.LENGTH_SHORT).show();}}
    private void openLocation(String raw){try{Uri u=raw.startsWith("http")||raw.startsWith("geo:")?Uri.parse(raw):Uri.parse("geo:0,0?q="+Uri.encode(raw)); startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ex){Toast.makeText(this,"تعذر فتح الموقع",Toast.LENGTH_SHORT).show();}}
    private String format(long t){return new SimpleDateFormat("EEEE، d MMM yyyy • h:mm a",new Locale("ar")).format(new Date(t));}
    private Button small(String s){Button b=new Button(this); b.setText(s); b.setTextSize(13); b.setMinHeight(0); b.setMinWidth(0); b.setPadding(dp(10),0,dp(10),0); return b;}
    private TextView text(String s,int z,boolean bold){TextView t=new TextView(this); t.setText(s); t.setTextSize(z); t.setTextColor(Color.BLACK); if(bold)t.setTypeface(null,1); return t;}
    private GradientDrawable round(int color,int r){GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); return g;}
    private LinearLayout.LayoutParams lp(int w,int h){return new LinearLayout.LayoutParams(w,h);}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
}
