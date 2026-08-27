package com.rafat.munasabati;

import android.app.*;import android.content.*;import java.util.*;

public final class AhlBaytReminderScheduler {
 private static final int BASE=48700;
 public static void schedule(Context c){cancel(c);if(!AhlBaytCalendar.remindersEnabled(c))return;AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);List<AhlBaytCalendar.Occurrence> list=AhlBaytCalendar.next(c,40);int i=0;for(AhlBaytCalendar.Occurrence o:list){Calendar x=Calendar.getInstance();x.setTimeInMillis(o.time);x.set(Calendar.HOUR_OF_DAY,8);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);if(x.getTimeInMillis()<=System.currentTimeMillis())continue;Intent in=new Intent(c,AhlBaytReminderReceiver.class).putExtra("index",AhlBaytCalendar.allOccasions().indexOf(o.occasion)).putExtra("time",o.time);PendingIntent pi=PendingIntent.getBroadcast(c,BASE+i++,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);try{if(android.os.Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);else am.set(AlarmManager.RTC_WAKEUP,x.getTimeInMillis(),pi);}catch(Exception ignored){}}}
 public static void cancel(Context c){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);for(int i=0;i<50;i++){PendingIntent pi=PendingIntent.getBroadcast(c,BASE+i,new Intent(c,AhlBaytReminderReceiver.class),PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(pi!=null)am.cancel(pi);}}
 private AhlBaytReminderScheduler(){}
}
