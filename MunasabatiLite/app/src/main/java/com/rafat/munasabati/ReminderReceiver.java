package com.rafat.munasabati;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String CHANNEL = "event_reminders";
    @Override public void onReceive(Context context, Intent intent) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel(CHANNEL, "تذكيرات المناسبات", NotificationManager.IMPORTANCE_HIGH);
            ch.setDescription("تنبيهات المناسبات المحفوظة في تطبيق مناسبتي");
            nm.createNotificationChannel(ch);
        }
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        long id = intent.getLongExtra("event_id", 0L);
        String title = intent.getStringExtra("title");
        if (title == null || title.trim().isEmpty()) title = "لديك مناسبة قادمة";
        Intent open = new Intent(context, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent content = PendingIntent.getActivity(context, Long.valueOf(id).hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(context, CHANNEL) : new Notification.Builder(context);
        Notification n = b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("تذكير بالمناسبة")
                .setContentText(title).setContentIntent(content).setAutoCancel(true).build();
        nm.notify(Long.valueOf(id).hashCode(), n);
    }
}
