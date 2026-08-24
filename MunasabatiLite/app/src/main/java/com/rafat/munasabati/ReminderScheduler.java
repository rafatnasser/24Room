package com.rafat.munasabati;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

public final class ReminderScheduler {
    public static void schedule(Context context, EventStore.Event e) {
        if (e == null) return;
        long now = System.currentTimeMillis();
        long occurrence = Recurrence.nextReminderOccurrence(e, now);
        long triggerAt = occurrence - Math.max(0, e.reminderMinutes) * 60000L;

        if (!Recurrence.isRecurring(e) && triggerAt <= now) return;
        if (Recurrence.isRecurring(e) && triggerAt <= now) {
            occurrence = Recurrence.nextOccurrence(e, now + Math.max(0, e.reminderMinutes) * 60000L + 1000L);
            triggerAt = occurrence - Math.max(0, e.reminderMinutes) * 60000L;
        }
        if (triggerAt <= now) return;

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pi = pendingIntent(context, e.id, e.title, occurrence);
        if (Build.VERSION.SDK_INT >= 31 && am.canScheduleExactAlarms()) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context, long id) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent(context, id, "", 0L));
    }

    private static PendingIntent pendingIntent(Context context, long id, String title, long occurrenceTime) {
        Intent i = new Intent(context, ReminderReceiver.class);
        i.putExtra("event_id", id);
        i.putExtra("title", title);
        i.putExtra("occurrence_time", occurrenceTime);
        return PendingIntent.getBroadcast(context, Long.valueOf(id).hashCode(), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private ReminderScheduler() {}
}
