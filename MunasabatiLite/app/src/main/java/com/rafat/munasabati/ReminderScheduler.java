package com.rafat.munasabati;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.util.List;

public final class ReminderScheduler {
    public static final String ACTION_FIRE="com.rafat.munasabati.FIRE_REMINDER";
    public static final String ACTION_SNOOZE="com.rafat.munasabati.SNOOZE_REMINDER";

    public static void schedule(Context context,EventStore.Event e){
        if(e==null)return;
        CalendarIntegration.upsertAndPersist(context,e);
        List<Integer> reminders=EventStore.reminders(e);
        for(int i=0;i<reminders.size();i++)scheduleIndex(context,e,i,reminders.get(i),System.currentTimeMillis());
    }

    public static void scheduleIndex(Context context,EventStore.Event e,int index,int minutes,long now){
        long occurrence;
        if(Recurrence.isRecurring(e))occurrence=Recurrence.nextOccurrence(e,now+Math.max(0,minutes)*60000L+1000L);
        else occurrence=e.eventTime;
        long triggerAt=occurrence-Math.max(0,minutes)*60000L;
        if(triggerAt<=now)return;
        scheduleAt(context,pendingIntent(context,e.id,index,e.title,occurrence,minutes,false,0),triggerAt);
    }

    public static void scheduleSnooze(Context context,long eventId,long occurrence,long delayMillis,int token){
        EventStore.Event e=EventStore.find(context,eventId);String title=e==null?"":e.title;
        PendingIntent pi=pendingIntent(context,eventId,100+token,title,occurrence,0,true,token);
        scheduleAt(context,pi,System.currentTimeMillis()+Math.max(60000L,delayMillis));
    }

    public static void cancel(Context context,long id){
        CalendarIntegration.deleteLinkedEvent(context,id);
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        for(int i=0;i<20;i++)am.cancel(pendingIntent(context,id,i,"",0L,0,false,0));
        for(int i=0;i<10;i++)am.cancel(pendingIntent(context,id,100+i,"",0L,0,true,i));
    }

    private static void scheduleAt(Context context,PendingIntent pi,long triggerAt){
        AlarmManager am=(AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        if(Build.VERSION.SDK_INT>=31&&am.canScheduleExactAlarms())am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAt,pi);
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,triggerAt,pi);
    }

    public static PendingIntent snoozeAction(Context context,long id,long occurrence,long delayMillis,int notificationId,int token){
        Intent i=new Intent(context,ReminderReceiver.class).setAction(ACTION_SNOOZE);
        i.putExtra("event_id",id);i.putExtra("occurrence_time",occurrence);i.putExtra("snooze_delay",delayMillis);
        i.putExtra("notification_id",notificationId);i.putExtra("snooze_token",token);
        return PendingIntent.getBroadcast(context,requestCode(id,500+token),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent pendingIntent(Context context,long id,int index,String title,long occurrenceTime,int minutes,boolean snooze,int token){
        Intent i=new Intent(context,ReminderReceiver.class).setAction(ACTION_FIRE);
        i.putExtra("event_id",id);i.putExtra("title",title);i.putExtra("occurrence_time",occurrenceTime);
        i.putExtra("reminder_index",index);i.putExtra("reminder_minutes",minutes);i.putExtra("is_snooze",snooze);i.putExtra("snooze_token",token);
        return PendingIntent.getBroadcast(context,requestCode(id,index),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }

    private static int requestCode(long id,int index){return 31*(int)(id^(id>>>32))+index;}
    private ReminderScheduler(){}
}
