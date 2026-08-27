package com.rafat.munasabati;

import android.content.Context;import org.json.*;

public final class SyncConflictStore{
 private static final String P="munasabati_sync_conflicts",K="items";
 public static JSONArray list(Context c){try{return new JSONArray(c.getSharedPreferences(P,0).getString(K,"[]"));}catch(Exception e){return new JSONArray();}}
 public static synchronized void add(Context c,EventStore.Event local,CalendarIntegration.ExternalEvent ext){try{JSONArray a=list(c);for(int i=a.length()-1;i>=0;i--){JSONObject x=a.optJSONObject(i);if(x!=null&&x.optLong("localId")==local.id)a.remove(i);}JSONObject o=new JSONObject();o.put("localId",local.id);o.put("externalId",ext.id);o.put("time",System.currentTimeMillis());o.put("localTitle",local.title);o.put("externalTitle",ext.title);o.put("localStart",local.eventTime);o.put("externalStart",ext.start);o.put("localLocation",local.locationName);o.put("externalLocation",ext.location);o.put("localDetails",local.details);o.put("externalDetails",ext.description);a.put(o);c.getSharedPreferences(P,0).edit().putString(K,a.toString()).apply();}catch(Exception ignored){}}
 public static synchronized void remove(Context c,int index){JSONArray a=list(c);if(index>=0&&index<a.length()){a.remove(index);c.getSharedPreferences(P,0).edit().putString(K,a.toString()).apply();}}
 public static synchronized void clear(Context c){c.getSharedPreferences(P,0).edit().putString(K,"[]").apply();}
 public static int count(Context c){return list(c).length();}
 private SyncConflictStore(){}
}
