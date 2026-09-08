package com.rafat.munasabati;

import android.app.Activity;
import android.app.NotificationManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class StrongAlertActivity extends Activity {
    private long eventId,occurrence;
    private int notificationId;
    private final int primary=Color.rgb(25,91,86),accent=Color.rgb(208,151,56),bg=Color.rgb(246,248,251);

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        if(Build.VERSION.SDK_INT>=27){setShowWhenLocked(true);setTurnScreenOn(true);}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED|
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON|
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);

        eventId=getIntent().getLongExtra("event_id",0L);
        occurrence=getIntent().getLongExtra("occurrence_time",0L);
        notificationId=getIntent().getIntExtra("notification_id",0);
        buildUi();
    }

    private void buildUi(){
        boolean ar=AppSettings.isArabic(this);
        EventStore.Event e=EventStore.find(this,eventId);
        String title=e==null?(ar?"مناسبة مهمة":"Important event"):e.title;
        String category=e==null?"":Categories.label(this,e.category);

        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);root.setPadding(dp(24),dp(36),dp(24),dp(28));root.setBackgroundColor(primary);
        TextView bell=text("🔔",54,true);bell.setGravity(Gravity.CENTER);bell.setTextColor(Color.WHITE);root.addView(bell);
        TextView label=text(ar?"تنبيه قوي":"STRONG ALERT",16,true);label.setGravity(Gravity.CENTER);label.setTextColor(Color.rgb(230,202,136));root.addView(label);
        TextView name=text(title,30,true);name.setGravity(Gravity.CENTER);name.setTextColor(Color.WHITE);name.setPadding(0,dp(16),0,dp(8));root.addView(name);
        if(!category.isEmpty()){TextView cat=text(category,16,true);cat.setGravity(Gravity.CENTER);cat.setTextColor(Color.rgb(221,238,236));root.addView(cat);}
        if(occurrence>0){TextView date=text(DateTools.gregorian(this,occurrence,true)+"\n"+DateTools.hijri(this,occurrence),16,false);date.setGravity(Gravity.CENTER);date.setTextColor(Color.WHITE);date.setPadding(0,dp(14),0,dp(12));root.addView(date);}
        if(e!=null&&!e.locationName.trim().isEmpty()){TextView loc=text("📍 "+e.locationName,15,false);loc.setGravity(Gravity.CENTER);loc.setTextColor(Color.rgb(221,238,236));root.addView(loc);}

        LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(14),dp(14),dp(14),dp(14));card.setBackground(round(Color.WHITE,20));
        LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.setMargins(0,dp(28),0,0);root.addView(card,cp);

        Button stop=button(ar?"إيقاف التنبيه":"Stop alert",Color.WHITE,Color.rgb(180,45,45),18);stop.setTypeface(null,Typeface.BOLD);card.addView(stop,new LinearLayout.LayoutParams(-1,dp(58)));stop.setOnClickListener(v->stopAndFinish());

        TextView snoozeLabel=text(ar?"تأجيل":"Snooze",14,true);snoozeLabel.setTextColor(primary);snoozeLabel.setGravity(Gravity.CENTER);snoozeLabel.setPadding(0,dp(12),0,dp(6));card.addView(snoozeLabel);
        LinearLayout row=new LinearLayout(this);card.addView(row,new LinearLayout.LayoutParams(-1,dp(52)));
        Button ten=button(ar?"10 دقائق":"10 min",primary,Color.rgb(235,244,243),13);Button hour=button(ar?"ساعة":"1 hour",primary,Color.rgb(235,244,243),13);Button tomorrow=button(ar?"غدًا":"Tomorrow",primary,Color.rgb(235,244,243),13);
        row.addView(ten,new LinearLayout.LayoutParams(0,-1,1));row.addView(hour,new LinearLayout.LayoutParams(0,-1,1));row.addView(tomorrow,new LinearLayout.LayoutParams(0,-1,1));
        ten.setOnClickListener(v->snooze(10*60000L,7));hour.setOnClickListener(v->snooze(60*60000L,8));tomorrow.setOnClickListener(v->snooze(24*60*60000L,9));

        setContentView(root);
    }

    private void snooze(long delay,int token){
        ReminderScheduler.scheduleSnooze(this,eventId,occurrence,delay,token);
        cancelNotification();finish();
    }
    private void stopAndFinish(){cancelNotification();finish();}
    private void cancelNotification(){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(notificationId);}
    @Override public void onBackPressed(){stopAndFinish();}

    private TextView text(String s,int size,boolean bold){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);if(bold)t.setTypeface(null,Typeface.BOLD);return t;}
    private Button button(String s,int textColor,int back,int size){Button b=new Button(this);b.setAllCaps(false);b.setText(s);b.setTextColor(textColor);b.setTextSize(size);b.setBackground(round(back,14));b.setMinWidth(0);return b;}
    private GradientDrawable round(int color,int radius){GradientDrawable g=new GradientDrawable();g.setColor(color);g.setCornerRadius(dp(radius));return g;}
    private int dp(int n){return(int)(n*getResources().getDisplayMetrics().density+.5f);}
}
