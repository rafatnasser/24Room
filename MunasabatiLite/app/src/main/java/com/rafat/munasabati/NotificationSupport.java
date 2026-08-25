package com.rafat.munasabati;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class NotificationSupport {
    public static final String CHANNEL_ID="event_reminders_v2";

    public static void ensureChannel(Context context){
        if(Build.VERSION.SDK_INT<26)return;
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        boolean ar=AppSettings.isArabic(context);
        NotificationChannel ch=new NotificationChannel(
                CHANNEL_ID,
                ar?"تنبيهات المناسبات المهمة":"Important event reminders",
                NotificationManager.IMPORTANCE_HIGH
        );
        ch.setDescription(ar?"تنبيهات بصوت واهتزاز وظهور على شاشة القفل":"Audible event reminders with vibration and lock-screen visibility");
        ch.enableVibration(true);
        ch.setVibrationPattern(new long[]{0,350,180,350});
        ch.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        ch.setShowBadge(true);
        ch.enableLights(true);
        ch.setLightColor(Color.rgb(25,91,86));
        Uri sound=Settings.System.DEFAULT_NOTIFICATION_URI;
        AudioAttributes audio=new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        ch.setSound(sound,audio);
        nm.createNotificationChannel(ch);
    }

    public static void openChannelSettings(Context context){
        ensureChannel(context);
        Intent i;
        if(Build.VERSION.SDK_INT>=26){
            i=new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,context.getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID,CHANNEL_ID);
        }else{
            i=new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE,context.getPackageName());
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }

    private NotificationSupport(){}
}
