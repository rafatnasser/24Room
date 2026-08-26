package com.rafat.munasabati;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StrongAlertReceiver extends BroadcastReceiver {
    public static final String ACTION_STOP="com.rafat.munasabati.STRONG_STOP";

    @Override public void onReceive(Context context,Intent intent){
        int notificationId=intent.getIntExtra("notification_id",0);
        if(ACTION_STOP.equals(intent.getAction())){
            NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
            nm.cancel(notificationId);
        }
    }
}
