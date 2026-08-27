package com.rafat.munasabati;

import android.content.*;

public class ReminderEscalationReceiver extends BroadcastReceiver{
 @Override public void onReceive(Context c,Intent in){long id=in.getLongExtra("event_id",0L);int notificationId=in.getIntExtra("notification_id",0);if(ReminderEscalation.isAck(in.getAction())){ReminderEscalation.ack(c,id,notificationId);return;}if(!ReminderEscalation.isFire(in.getAction()))return;EventStore.Event e=EventStore.find(c,id);if(e==null||!"important".equals(V43Prefs.alertProfile(c,id)))return;Intent r=new Intent(c,ReminderReceiver.class).putExtra("event_id",id).putExtra("occurrence_time",in.getLongExtra("occurrence_time",e.eventTime)).putExtra("reminder_index",991).putExtra("reminder_minutes",0).putExtra("force_strong",true).putExtra("is_escalation",true);c.sendBroadcast(r);V4Log.history(c,id,"escalation","Strong Alert after no acknowledgement");}
}
