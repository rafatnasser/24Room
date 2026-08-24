package com.rafat.munasabati;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class EventStore {
    private static final String PREFS = "munasabati_events";
    private static final String KEY = "events";

    public static final class Event {
        public long id;
        public String title = "";
        public String details = "";
        public long eventTime;
        public int reminderMinutes = 30;
        public String attachmentUri = "";
        public String attachmentName = "";
        public String attachmentType = "";
        public String locationName = "";
        public String locationUrl = "";
    }

    public static List<Event> load(Context context) {
        ArrayList<Event> out = new ArrayList<>();
        String raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]");
        try {
            JSONArray a = new JSONArray(raw);
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                Event e = new Event();
                e.id = o.optLong("id");
                e.title = o.optString("title");
                e.details = o.optString("details");
                e.eventTime = o.optLong("eventTime");
                e.reminderMinutes = o.optInt("reminderMinutes", 30);
                e.attachmentUri = o.optString("attachmentUri");
                e.attachmentName = o.optString("attachmentName");
                e.attachmentType = o.optString("attachmentType");
                e.locationName = o.optString("locationName");
                e.locationUrl = o.optString("locationUrl");
                out.add(e);
            }
        } catch (Exception ignored) {}
        Collections.sort(out, Comparator.comparingLong(x -> x.eventTime));
        return out;
    }

    public static void save(Context context, List<Event> events) {
        JSONArray a = new JSONArray();
        for (Event e : events) {
            JSONObject o = new JSONObject();
            try {
                o.put("id", e.id); o.put("title", e.title); o.put("details", e.details);
                o.put("eventTime", e.eventTime); o.put("reminderMinutes", e.reminderMinutes);
                o.put("attachmentUri", e.attachmentUri); o.put("attachmentName", e.attachmentName);
                o.put("attachmentType", e.attachmentType); o.put("locationName", e.locationName);
                o.put("locationUrl", e.locationUrl); a.put(o);
            } catch (Exception ignored) {}
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, a.toString()).apply();
    }

    public static Event find(Context context, long id) {
        for (Event e : load(context)) if (e.id == id) return e;
        return null;
    }

    private EventStore() {}
}
