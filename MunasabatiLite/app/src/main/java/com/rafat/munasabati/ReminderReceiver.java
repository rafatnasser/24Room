package com.rafat.munasabati;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import java.util.List;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        String action=intent.getAction();
        if(ReminderScheduler.ACTION_SNOOZE.equals(action)){
            long id=intent.getLongExtra("event_id",0L),occ=intent.getLongExtra("occurrence_time",0L);
            long delay=intent.getLongExtra("snooze_delay",600000L);
            int notif=intent.getIntExtra("notification_id",0),token=intent.getIntExtra("snooze_token",0);
            ReminderScheduler.scheduleSnooze(context,id,occ,delay,token);
            ((NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE)).cancel(notif);
            return;
        }

        boolean ar=AppSettings.isArabic(context);
        NotificationSupport.ensureChannel(context);
        StrongAlertSupport.ensureChannel(context);
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        long id=intent.getLongExtra("event_id",0L),occurrence=intent.getLongExtra("occurrence_time",0L);
        int reminderIndex=intent.getIntExtra("reminder_index",0),minutes=intent.getIntExtra("reminder_minutes",0);
        boolean snooze=intent.getBooleanExtra("is_snooze",false);
        EventStore.Event e=EventStore.find(context,id);
        String title=e!=null?e.title:intent.getStringExtra("title");
        if(title==null||title.trim().isEmpty())title=ar?"لديك مناسبة قادمة":"You have an upcoming event";
        if(occurrence<=0&&e!=null)occurrence=e.eventTime;

        boolean strong=false;
        if(e!=null&&e.strongAlert){
            List<Integer> rs=EventStore.reminders(e);int closest=rs.isEmpty()?minutes:rs.get(rs.size()-1);
            strong=snooze||minutes==closest;
        }

        int notificationId=31*(int)(id^(id>>>32))+(snooze?900+intent.getIntExtra("snooze_token",0):reminderIndex);
        if(Build.VERSION.SDK_INT<33||context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED){
            Intent open=new Intent(context,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent content=PendingIntent.getActivity(context,(int)(id^(id>>>32)),open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            String body=e==null?title:Categories.label(context,e.category)+" • "+DateTools.gregorian(context,occurrence,true);
            if(snooze)body+=(ar?" • تنبيه مؤجل":" • Snoozed reminder");

            Notification.Builder b;
            if(strong){
                Intent alert=new Intent(context,StrongAlertActivity.class)
                        .putExtra("event_id",id).putExtra("occurrence_time",occurrence).putExtra("notification_id",notificationId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                PendingIntent full=PendingIntent.getActivity(context,notificationId+8000,alert,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

                Intent stopIntent=new Intent(context,StrongAlertReceiver.class).setAction(StrongAlertReceiver.ACTION_STOP).putExtra("notification_id",notificationId);
                PendingIntent stop=PendingIntent.getBroadcast(context,notificationId+9000,stopIntent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

                b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,StrongAlertSupport.CHANNEL_ID):new Notification.Builder(context);
                b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(ar?"🔔 تنبيه قوي — "+title:"🔔 Strong alert — "+title)
                        .setContentText(body)
                        .setStyle(new Notification.BigTextStyle().bigText(title+"\n"+body))
                        .setContentIntent(full)
                        .setFullScreenIntent(full,true)
                        .setAutoCancel(false)
                        .setOngoing(true)
                        .setCategory(Notification.CATEGORY_ALARM)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setWhen(System.currentTimeMillis()).setShowWhen(true);
                b.addAction(new Notification.Action.Builder(0,ar?"إيقاف":"Stop",stop).build());
                b.addAction(new Notification.Action.Builder(0,ar?"10 دقائق":"10 min",ReminderScheduler.snoozeAction(context,id,occurrence,10*60000L,notificationId,1)).build());
                b.addAction(new Notification.Action.Builder(0,ar?"ساعة":"1 hour",ReminderScheduler.snoozeAction(context,id,occurrence,60*60000L,notificationId,2)).build());
                Notification n=b.build();
                n.flags|=Notification.FLAG_INSISTENT|Notification.FLAG_ONGOING_EVENT|Notification.FLAG_NO_CLEAR;
                nm.notify(notificationId,n);
            }else{
                b=Build.VERSION.SDK_INT>=26?new Notification.Builder(context,NotificationSupport.CHANNEL_ID):new Notification.Builder(context);
                b.setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                        .setContentTitle(ar?"تذكير بالمناسبة":"Event reminder")
                        .setContentText(body)
                        .setStyle(new Notification.BigTextStyle().bigText(title+"\n"+body))
                        .setContentIntent(content)
                        .setAutoCancel(true)
                        .setCategory(Notification.CATEGORY_EVENT)
                        .setVisibility(Notification.VISIBILITY_PUBLIC)
                        .setPriority(Notification.PRIORITY_MAX)
                        .setDefaults(Notification.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis()).setShowWhen(true);
                b.addAction(new Notification.Action.Builder(0,ar?"10 دقائق":"10 min",ReminderScheduler.snoozeAction(context,id,occurrence,10*60000L,notificationId,1)).build());
                b.addAction(new Notification.Action.Builder(0,ar?"ساعة":"1 hour",ReminderScheduler.snoozeAction(context,id,occurrence,60*60000L,notificationId,2)).build());
                b.addAction(new Notification.Action.Builder(0,ar?"غدًا":"Tomorrow",ReminderScheduler.snoozeAction(context,id,occurrence,24*60*60000L,notificationId,3)).build());
                nm.notify(notificationId,b.build());
            }
        }

        if(e!=null&&!snooze&&Recurrence.isRecurring(e))ReminderScheduler.scheduleIndex(context,e,reminderIndex,minutes,System.currentTimeMillis()+1000L);
    }
}
