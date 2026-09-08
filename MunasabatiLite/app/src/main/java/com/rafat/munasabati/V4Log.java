package com.rafat.munasabati;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;

public final class V4Log {
    private static final String PREF="munasabati_v4_log",ERR="errors",HIST="history";
    public static void error(Context c,String type,String message){append(c,ERR,0,type,message);}
    public static void history(Context c,long eventId,String action,String detail){append(c,HIST,eventId,action,detail);}
    private static synchronized void append(Context c,String key,long id,String type,String msg){try{String raw=c.getSharedPreferences(PREF,0).getString(key,"[]");JSONArray a=new JSONArray(raw),b=new JSONArray();JSONObject o=new JSONObject();o.put("time",System.currentTimeMillis());o.put("eventId",id);o.put("type",type);o.put("message",msg==null?"":msg);b.put(o);for(int i=0;i<a.length()&&i<99;i++)b.put(a.get(i));c.getSharedPreferences(PREF,0).edit().putString(key,b.toString()).apply();}catch(Exception ignored){}}
    public static JSONArray errors(Context c){return array(c,ERR);}
    public static JSONArray history(Context c,long eventId){JSONArray all=array(c,HIST),out=new JSONArray();for(int i=0;i<all.length();i++){JSONObject o=all.optJSONObject(i);if(o!=null&&(eventId<=0||o.optLong("eventId")==eventId))out.put(o);}return out;}
    private static JSONArray array(Context c,String k){try{return new JSONArray(c.getSharedPreferences(PREF,0).getString(k,"[]"));}catch(Exception e){return new JSONArray();}}
    public static void clearErrors(Context c){c.getSharedPreferences(PREF,0).edit().putString(ERR,"[]").apply();}
    private V4Log(){}
}
