package com.rafat.munasabati;
import android.app.*;import android.content.*;import android.os.Build;
public final class LocationReminderScheduler{
 public static void schedule(Context c){boolean any=false;for(EventStore.Event e:EventStore.load(c)){V4Meta.Meta m=V4Meta.get(c,e.id);if(!"none".equals(m.locationReminder)&&!Double.isNaN(m.locationLat)&&!Double.isNaN(m.locationLng)){any=true;break;}}AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);Intent i=new Intent(c,LocationReminderReceiver.class);PendingIntent pi=PendingIntent.getBroadcast(c,88441,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);am.cancel(pi);if(any)am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+15*60*1000L,pi);}
 private LocationReminderScheduler(){}
}
