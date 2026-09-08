package com.rafat.munasabati;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import java.util.Calendar;

public final class AutoBackupScheduler {
    private static final int REQUEST=77031;

    public static void schedule(Context c){
        cancel(c);
        if(!AppSettings.autoBackupEnabled(c)||AppSettings.autoBackupTree(c).isEmpty())return;
        Calendar next=Calendar.getInstance();
        next.add(Calendar.DAY_OF_YEAR,AppSettings.autoBackupDays(c));
        next.set(Calendar.HOUR_OF_DAY,3);next.set(Calendar.MINUTE,15);next.set(Calendar.SECOND,0);next.set(Calendar.MILLISECOND,0);
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,next.getTimeInMillis(),pending(c));
    }

    public static void cancel(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);am.cancel(pending(c));
    }

    private static PendingIntent pending(Context c){
        Intent i=new Intent(c,AutoBackupReceiver.class);
        return PendingIntent.getBroadcast(c,REQUEST,i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private AutoBackupScheduler(){}
}
