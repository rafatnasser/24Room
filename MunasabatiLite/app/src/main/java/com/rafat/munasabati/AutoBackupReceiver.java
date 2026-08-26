package com.rafat.munasabati;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class AutoBackupReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent){
        final PendingResult pending=goAsync();
        new Thread(()->{
            try{
                AutoBackupManager.run(context);
                AppSettings.setLastBackup(context,System.currentTimeMillis(),AppSettings.tr(context,"ناجح","Success"));
            }catch(Exception ex){
                AppSettings.setLastBackup(context,System.currentTimeMillis(),AppSettings.tr(context,"فشل: ","Failed: ")+(ex.getMessage()==null?ex.getClass().getSimpleName():ex.getMessage()));
            }finally{
                AutoBackupScheduler.schedule(context);pending.finish();
            }
        },"munasabati-auto-backup").start();
    }
}
