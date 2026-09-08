package com.rafat.munasabati;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

public final class CalendarSyncScheduler {
    private static final int REQUEST=88033;
    private static final long INTERVAL=6L*60L*60L*1000L;

    public static void schedule(Context c){
        cancel(c);if(!CalendarIntegration.enabled(c)||!CalendarIntegration.hasPermission(c))return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+INTERVAL,pending(c));
    }
    public static void cancel(Context c){((AlarmManager)c.getSystemService(Context.ALARM_SERVICE)).cancel(pending(c));}
    private static PendingIntent pending(Context c){return PendingIntent.getBroadcast(c,REQUEST,new Intent(c,CalendarSyncReceiver.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);}
    private CalendarSyncScheduler(){}
}
