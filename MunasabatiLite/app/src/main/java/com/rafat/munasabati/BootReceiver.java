package com.rafat.munasabati;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){
            for(EventStore.Event e:EventStore.load(context))ReminderScheduler.schedule(context,e);
            AutoBackupScheduler.schedule(context);
        }
    }
}
