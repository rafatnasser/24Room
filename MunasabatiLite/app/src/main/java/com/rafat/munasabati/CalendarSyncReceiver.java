package com.rafat.munasabati;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CalendarSyncReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        final PendingResult pending=goAsync();
        new Thread(()->{try{if(CalendarIntegration.enabled(context)&&CalendarIntegration.hasPermission(context))CalendarIntegration.syncBidirectional(context);}finally{CalendarSyncScheduler.schedule(context);pending.finish();}},"munasabati-calendar-sync").start();
    }
}
