package com.rafat.munasabati;

import android.app.*;import android.content.*;

public final class ReminderEscalation{
 private static final String ACTION_FIRE="com.rafat.munasabati.ESCALATE",ACTION_ACK="com.rafat.munasabati.ESCALATE_ACK";
 private static int req(long eventId,int notificationId){return 730000+(int)((eventId^(eventId>>>32)^notificationId)&0x1fffffff);}
 public static void schedule(Context c,long eventId,long occurrence,int notificationId,long delay){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,ReminderEscalationReceiver.class).setAction(ACTION_FIRE).putExtra("event_id",eventId).putExtra("occurrence_time",occurrence).putExtra("notification_id",notificationId);PendingIntent p=PendingIntent.getBroadcast(c,req(eventId,notificationId),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);long at=System.currentTimeMillis()+Math.max(60000L,delay);try{if(android.os.Build.VERSION.SDK_INT>=23)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,at,p);else am.set(AlarmManager.RTC_WAKEUP,at,p);}catch(Exception ignored){}}
 public static void ack(Context c,long eventId,int notificationId){AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,ReminderEscalationReceiver.class).setAction(ACTION_FIRE);PendingIntent p=PendingIntent.getBroadcast(c,req(eventId,notificationId),i,PendingIntent.FLAG_NO_CREATE|PendingIntent.FLAG_IMMUTABLE);if(p!=null){am.cancel(p);p.cancel();}V4Log.history(c,eventId,"reminder_ack","notification="+notificationId);}
 public static PendingIntent ackIntent(Context c,long eventId,int notificationId){Intent i=new Intent(c,ReminderEscalationReceiver.class).setAction(ACTION_ACK).putExtra("event_id",eventId).putExtra("notification_id",notificationId);return PendingIntent.getBroadcast(c,req(eventId,notificationId)+1,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
 public static boolean isFire(String a){return ACTION_FIRE.equals(a);}public static boolean isAck(String a){return ACTION_ACK.equals(a);}private ReminderEscalation(){}
}
