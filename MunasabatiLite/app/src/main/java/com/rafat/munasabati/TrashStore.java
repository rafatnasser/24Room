package com.rafat.munasabati;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public final class TrashStore {
    private static final String PREF="munasabati_trash",KEY="items";
    public static void move(Context c,EventStore.Event e){if(e==null)return;try{JSONArray a=loadRaw(c),b=new JSONArray();JSONObject o=new JSONObject();o.put("deletedAt",System.currentTimeMillis());o.put("event",EventStore.toJsonObject(e,true));o.put("meta",metaJson(c,e.id));b.put(o);for(int i=0;i<a.length();i++)b.put(a.get(i));c.getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply();V4Log.history(c,e.id,"delete","Moved to trash");}catch(Exception ignored){}}
    private static JSONObject metaJson(Context c,long id){V4Meta.Meta m=V4Meta.get(c,id);try{JSONObject o=new JSONObject();o.put("tag",m.tag);o.put("tz",m.timeZoneId);o.put("smart",m.smartReminder);o.put("web",m.webUrl);o.put("phone",m.phone);o.put("contact",m.contactName);o.put("voice",m.voiceNoteUri);o.put("locType",m.locationReminder);if(!Double.isNaN(m.locationLat))o.put("lat",m.locationLat);if(!Double.isNaN(m.locationLng))o.put("lng",m.locationLng);o.put("radius",m.locationRadius);o.put("guests",m.guestsJson);return o;}catch(Exception e){return new JSONObject();}}
    public static JSONArray items(Context c){purge(c);return loadRaw(c);}
    public static boolean restore(Context c,int index){try{JSONArray a=loadRaw(c);JSONObject x=a.getJSONObject(index);EventStore.Event e=EventStore.fromJsonObject(x.getJSONObject("event"));List<EventStore.Event> all=new ArrayList<>(EventStore.load(c));all.removeIf(v->v.id==e.id);all.add(e);EventStore.save(c,all);JSONObject mo=x.optJSONObject("meta");if(mo!=null){V4Meta.Meta m=new V4Meta.Meta();m.tag=mo.optString("tag","personal");m.timeZoneId=mo.optString("tz",TimeZone.getDefault().getID());m.smartReminder=mo.optBoolean("smart");m.webUrl=mo.optString("web","");m.phone=mo.optString("phone","");m.contactName=mo.optString("contact","");m.voiceNoteUri=mo.optString("voice","");m.locationReminder=mo.optString("locType","none");m.locationLat=mo.has("lat")?mo.optDouble("lat"):Double.NaN;m.locationLng=mo.has("lng")?mo.optDouble("lng"):Double.NaN;m.locationRadius=(float)mo.optDouble("radius",250);m.guestsJson=mo.optString("guests","[]");V4Meta.put(c,e.id,m);}removeAt(c,index);ReminderScheduler.schedule(c,e);V4Log.history(c,e.id,"restore","Restored from trash");return true;}catch(Exception ex){V4Log.error(c,"trash",ex.toString());return false;}}
    public static void removeAt(Context c,int index){try{JSONArray a=loadRaw(c),b=new JSONArray();for(int i=0;i<a.length();i++)if(i!=index)b.put(a.get(i));c.getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply();}catch(Exception ignored){}}
    public static void purge(Context c){try{long cutoff=System.currentTimeMillis()-30L*86400000L;JSONArray a=loadRaw(c),b=new JSONArray();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&o.optLong("deletedAt")>=cutoff)b.put(o);}c.getSharedPreferences(PREF,0).edit().putString(KEY,b.toString()).apply();}catch(Exception ignored){}}
    private static JSONArray loadRaw(Context c){try{return new JSONArray(c.getSharedPreferences(PREF,0).getString(KEY,"[]"));}catch(Exception e){return new JSONArray();}}
    private TrashStore(){}
}
