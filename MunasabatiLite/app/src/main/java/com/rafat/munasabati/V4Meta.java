package com.rafat.munasabati;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

public final class V4Meta {
    private static final String PREFS="munasabati_v4_meta";
    public static final String[] TAG_CODES={"family","work","friends","travel","personal"};
    public static final String[] TAG_AR={"العائلة","العمل","الأصدقاء","السفر","شخصي"};
    public static final String[] TAG_EN={"Family","Work","Friends","Travel","Personal"};

    public static final class Meta {
        public String tag="personal";
        public String timeZoneId=TimeZone.getDefault().getID();
        public boolean smartReminder=false;
        public String webUrl="";
        public String phone="";
        public String contactName="";
        public String voiceNoteUri="";
        public String locationReminder="none"; // none, arrive, leave
        public double locationLat=Double.NaN,locationLng=Double.NaN;
        public float locationRadius=250f;
        public boolean geofenceInside=false;
        public long createdAt=0L,lastSyncAt=0L,lastAlertAt=0L;
        public String guestsJson="[]";
    }

    public static Meta get(Context c,long id){
        Meta m=new Meta();String raw=c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(String.valueOf(id),"");
        if(raw.isEmpty())return m;try{JSONObject o=new JSONObject(raw);m.tag=o.optString("tag",m.tag);m.timeZoneId=o.optString("tz",m.timeZoneId);m.smartReminder=o.optBoolean("smart",false);m.webUrl=o.optString("web","");m.phone=o.optString("phone","");m.contactName=o.optString("contact","");m.voiceNoteUri=o.optString("voice","");m.locationReminder=o.optString("locType","none");m.locationLat=o.has("lat")?o.optDouble("lat"):Double.NaN;m.locationLng=o.has("lng")?o.optDouble("lng"):Double.NaN;m.locationRadius=(float)o.optDouble("radius",250);m.geofenceInside=o.optBoolean("inside",false);m.createdAt=o.optLong("created",0);m.lastSyncAt=o.optLong("lastSync",0);m.lastAlertAt=o.optLong("lastAlert",0);m.guestsJson=o.optString("guests","[]");}catch(Exception ignored){}return m;
    }
    public static void put(Context c,long id,Meta m){try{JSONObject o=new JSONObject();o.put("tag",m.tag);o.put("tz",m.timeZoneId);o.put("smart",m.smartReminder);o.put("web",m.webUrl);o.put("phone",m.phone);o.put("contact",m.contactName);o.put("voice",m.voiceNoteUri);o.put("locType",m.locationReminder);if(!Double.isNaN(m.locationLat))o.put("lat",m.locationLat);if(!Double.isNaN(m.locationLng))o.put("lng",m.locationLng);o.put("radius",m.locationRadius);o.put("inside",m.geofenceInside);o.put("created",m.createdAt);o.put("lastSync",m.lastSyncAt);o.put("lastAlert",m.lastAlertAt);o.put("guests",m.guestsJson);c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(String.valueOf(id),o.toString()).apply();}catch(Exception ignored){}}
    public static void remove(Context c,long id){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().remove(String.valueOf(id)).apply();}
    public static String tagLabel(Context c,String code){for(int i=0;i<TAG_CODES.length;i++)if(TAG_CODES[i].equals(code))return AppSettings.isArabic(c)?TAG_AR[i]:TAG_EN[i];return AppSettings.tr(c,"شخصي","Personal");}
    public static int tagIndex(String code){for(int i=0;i<TAG_CODES.length;i++)if(TAG_CODES[i].equals(code))return i;return 4;}
    public static String[] tagLabels(Context c){return (AppSettings.isArabic(c)?TAG_AR:TAG_EN).clone();}

    public static JSONArray guests(Meta m){try{return new JSONArray(m.guestsJson);}catch(Exception e){return new JSONArray();}}
    public static int[] guestCounts(Meta m){int yes=0,no=0,pending=0,invited=0;JSONArray a=guests(m);for(int i=0;i<a.length();i++){String s=a.optJSONObject(i)==null?"":a.optJSONObject(i).optString("status","invited");if("yes".equals(s))yes++;else if("no".equals(s))no++;else if("pending".equals(s))pending++;else invited++;}return new int[]{yes,no,pending,invited};}
    private V4Meta(){}
}
