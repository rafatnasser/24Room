package com.rafat.munasabati;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class EventStore {
    private static final String PREFS="munasabati_events";
    private static final String KEY="events";

    public static final class Event {
        public long id;
        public String title="";
        public String category="none";
        public String recurrence=Recurrence.NONE;
        public String details="";
        public long eventTime;
        public int reminderMinutes=30;
        public String attachmentUri="";
        public String attachmentName="";
        public String attachmentType="";
        public String locationName="";
        public String locationUrl="";
        public String color="";
        public boolean favorite=false;
        public boolean pinned=false;
    }

    public static List<Event> load(Context context){
        ArrayList<Event> out=new ArrayList<>();
        String raw=context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(KEY,"[]");
        try{
            JSONArray a=new JSONArray(raw);
            for(int i=0;i<a.length();i++)out.add(fromJsonObject(a.getJSONObject(i)));
        }catch(Exception ignored){}
        Collections.sort(out,Comparator.comparingLong(x->x.eventTime));return out;
    }

    public static void save(Context context,List<Event> events){
        JSONArray a=new JSONArray();for(Event e:events)a.put(toJsonObject(e,true));
        context.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(KEY,a.toString()).apply();
    }

    public static Event find(Context context,long id){for(Event e:load(context))if(e.id==id)return e;return null;}

    public static JSONObject toJsonObject(Event e,boolean includeAttachmentUri){
        JSONObject o=new JSONObject();try{
            o.put("id",e.id);o.put("title",e.title);o.put("category",e.category);o.put("recurrence",e.recurrence);
            o.put("details",e.details);o.put("eventTime",e.eventTime);o.put("reminderMinutes",e.reminderMinutes);
            o.put("attachmentUri",includeAttachmentUri?e.attachmentUri:"");o.put("attachmentName",e.attachmentName);o.put("attachmentType",e.attachmentType);
            o.put("locationName",e.locationName);o.put("locationUrl",e.locationUrl);o.put("color",e.color);
            o.put("favorite",e.favorite);o.put("pinned",e.pinned);
        }catch(Exception ignored){}return o;
    }

    public static Event fromJsonObject(JSONObject o){
        Event e=new Event();
        e.id=o.optLong("id",System.currentTimeMillis());e.title=o.optString("title","");
        e.category=o.optString("category","none");e.recurrence=o.optString("recurrence",Recurrence.autoForCategory(e.category));
        e.details=o.optString("details","");e.eventTime=o.optLong("eventTime",System.currentTimeMillis());
        e.reminderMinutes=o.optInt("reminderMinutes",30);e.attachmentUri=o.optString("attachmentUri","");
        e.attachmentName=o.optString("attachmentName","");e.attachmentType=o.optString("attachmentType","");
        e.locationName=o.optString("locationName","");e.locationUrl=o.optString("locationUrl","");
        e.color=o.optString("color","");e.favorite=o.optBoolean("favorite",false);e.pinned=o.optBoolean("pinned",false);
        return e;
    }

    public static JSONObject exportJson(Context c,boolean portable){
        JSONObject root=new JSONObject();try{
            root.put("format","Munasabati");root.put("version",3);
            root.put("language",AppSettings.language(c));root.put("hijriOffset",AppSettings.hijriOffset(c));
            root.put("categoryColors",ColorPalette.exportSettings(c));
            JSONArray a=new JSONArray();for(Event e:load(c))a.put(toJsonObject(e,!portable));root.put("events",a);
        }catch(Exception ignored){}return root;
    }

    public static int importJson(Context c,JSONObject root){
        JSONArray a=root.optJSONArray("events");if(a==null)return 0;
        ArrayList<Event> events=new ArrayList<>();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null)events.add(fromJsonObject(o));}
        save(c,events);
        String lang=root.optString("language","");if("ar".equals(lang)||"en".equals(lang))AppSettings.setLanguage(c,lang);
        if(root.has("hijriOffset"))AppSettings.setHijriOffset(c,root.optInt("hijriOffset",0));
        ColorPalette.importSettings(c,root.optJSONObject("categoryColors"));
        for(Event e:events)ReminderScheduler.schedule(c,e);
        return events.size();
    }
    private EventStore(){}
}
