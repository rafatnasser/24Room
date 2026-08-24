package com.rafat.munasabati;

import android.content.Context;
import java.util.Calendar;

public final class Recurrence {
    public static final String NONE = "none";
    public static final String WEEKLY = "weekly";
    public static final String MONTHLY = "monthly";
    public static final String YEARLY = "yearly";
    public static final String[] CODES = {NONE, WEEKLY, MONTHLY, YEARLY};

    public static String label(Context c, String code) {
        if (WEEKLY.equals(code)) return AppSettings.tr(c, "أسبوعي", "Weekly");
        if (MONTHLY.equals(code)) return AppSettings.tr(c, "شهري", "Monthly");
        if (YEARLY.equals(code)) return AppSettings.tr(c, "سنوي", "Yearly");
        return AppSettings.tr(c, "بدون تكرار", "No repeat");
    }

    public static String[] labels(Context c) {
        return new String[]{
                label(c, NONE), label(c, WEEKLY), label(c, MONTHLY), label(c, YEARLY)
        };
    }

    public static int indexOf(String code) {
        for (int i = 0; i < CODES.length; i++) if (CODES[i].equals(code)) return i;
        return 0;
    }

    public static boolean isRecurring(EventStore.Event e) {
        return e != null && e.recurrence != null && !NONE.equals(e.recurrence);
    }

    public static long nextOccurrence(EventStore.Event e, long atOrAfter) {
        if (e == null) return 0L;
        if (!isRecurring(e)) return e.eventTime;

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(e.eventTime);
        if (c.getTimeInMillis() >= atOrAfter) return c.getTimeInMillis();

        int guard = 0;
        while (c.getTimeInMillis() < atOrAfter && guard++ < 10000) {
            if (WEEKLY.equals(e.recurrence)) c.add(Calendar.WEEK_OF_YEAR, 1);
            else if (MONTHLY.equals(e.recurrence)) c.add(Calendar.MONTH, 1);
            else if (YEARLY.equals(e.recurrence)) c.add(Calendar.YEAR, 1);
            else break;
        }
        return c.getTimeInMillis();
    }

    public static long nextReminderOccurrence(EventStore.Event e, long now) {
        long lead = Math.max(0, e.reminderMinutes) * 60000L;
        if (!isRecurring(e)) return e.eventTime;
        return nextOccurrence(e, now + lead + 1000L);
    }

    public static String autoForCategory(String category) {
        if ("weekly_habit".equals(category)) return WEEKLY;
        if ("anniversary".equals(category)) return YEARLY;
        return NONE;
    }

    private Recurrence() {}
}
