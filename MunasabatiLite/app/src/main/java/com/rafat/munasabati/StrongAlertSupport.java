package com.rafat.munasabati;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class StrongAlertSupport {
    public static final String CHANNEL_ID="strong_event_alerts_v1";

    public static void ensureChannel(Context context){
        if(Build.VERSION.SDK_INT<26)return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean ar=AppSettings.isArabic(context);
        NotificationChannel ch=new NotificationChannel(
                CHANNEL_ID,
                ar?"التنبيهات القوية":"Strong alerts",
                NotificationManager.IMPORTANCE_HIGH
        );
        ch.setDescription(ar?"تنبيه قوي بصوت منبّه متكرر واهتزاز وظهور على شاشة القفل":"Strong alarm-style alert with sound, vibration and lock-screen visibility");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0,700,250,700,250,1200});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        ch.enableLights(true);
        ch.setLightColor(Color.rgb(208,151,56));
        Uri sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if(sound==null)sound=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        AudioAttributes audio=new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ch.setSound(sound,audio);
        nm.createNotificationChannel(ch);
    }

    public static boolean canUseFullScreen(Context context){
        if(Build.VERSION.SDK_INT<34)return true;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        return nm.canUseFullScreenIntent();
    }

    public static void openFullScreenSettings(Context context){
        try{
            Intent i;
            if(Build.VERSION.SDK_INT>=34){
                i=new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:"+context.getPackageName()));
            }else{
                i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE,context.getPackageName());
            }
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }catch(Exception ignored){}
    }

    private StrongAlertSupport(){}
}
