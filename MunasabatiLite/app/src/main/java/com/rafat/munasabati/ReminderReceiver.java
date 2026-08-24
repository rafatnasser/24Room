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
    private static final String CHANNEL="event_reminders";
    @Override public void onReceive(Context context,Intent intent){
        boolean ar=AppSettings.isArabic(context);String channelName=ar?"تذكيرات المناسبات":"Event reminders";String desc=ar?"تنبيهات المناسبات المحفوظة في تطبيق مناسبتي":"Reminders for events saved in Munasabati";
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);if(Build.VERSION.SDK_INT>=26){NotificationChannel ch=new NotificationChannel(CHANNEL,channelName,NotificationManager.IMPORTANCE_HIGH);ch.setDescription(desc);nm.createNotificationChannel(ch);}if(Build.VERSION.SDK_INT>=33&&context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
        long id=intent.getLongExtra("event_id",0L);EventStore.Event e=EventStore.find(context,id);String title=e!=null?e.title:intent.getStringExtra("title");if(title==null||title.trim().isEmpty())title=ar?"لديك مناسبة قادمة":"You have an upcoming event";
        Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);PendingIntent content=PendingIntent.getActivity(context,Long.valueOf(id).hashCode(),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,CHANNEL):new Notification.Builder(context);String body=e==null?title:Categories.label(context,e.category)+" • "+DateTools.time(context,e.eventTime);
        Notification n=b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(ar?"تذكير بالمناسبة":"Event reminder").setContentText(body).setStyle(new Notification.BigTextStyle().bigText(title+"\n"+body)).setContentIntent(content).setAutoCancel(true).build();nm.notify(Long.valueOf(id).hashCode(),n);
    }
}
