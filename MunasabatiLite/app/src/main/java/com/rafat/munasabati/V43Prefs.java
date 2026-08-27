package com.rafat.munasabati;

import android.content.Context;

public final class V43Prefs {
    private static final String P="munasabati_v43";
    public static long autoLockMs(Context c){return c.getSharedPreferences(P,0).getLong("auto_lock_ms",60000L);}
    public static void setAutoLockMs(Context c,long v){c.getSharedPreferences(P,0).edit().putLong("auto_lock_ms",Math.max(0,v)).apply();}
    public static boolean hideNotificationContent(Context c){return c.getSharedPreferences(P,0).getBoolean("hide_notification_content",false);}
    public static void setHideNotificationContent(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean("hide_notification_content",v).apply();}
    public static boolean hideWidgetTitles(Context c){return c.getSharedPreferences(P,0).getBoolean("hide_widget_titles",false);}
    public static void setHideWidgetTitles(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean("hide_widget_titles",v).apply();WidgetUpdater.updateAll(c);ExtraWidgetUpdater.updateAll(c);}
    public static boolean protectAttachments(Context c){return c.getSharedPreferences(P,0).getBoolean("protect_attachments",false);}
    public static void setProtectAttachments(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean("protect_attachments",v).apply();}
    public static boolean protectExports(Context c){return c.getSharedPreferences(P,0).getBoolean("protect_exports",false);}
    public static void setProtectExports(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean("protect_exports",v).apply();}
    public static long lastBackground(Context c){return c.getSharedPreferences(P,0).getLong("last_background",0L);}
    public static void setLastBackground(Context c,long t){c.getSharedPreferences(P,0).edit().putLong("last_background",t).apply();}
    public static String alertProfile(Context c,long eventId){return c.getSharedPreferences(P,0).getString("alert_"+eventId,"normal");}
    public static void setAlertProfile(Context c,long eventId,String p){c.getSharedPreferences(P,0).edit().putString("alert_"+eventId,p==null?"normal":p).apply();}
    public static String privateTitle(Context c){return AppSettings.tr(c,"مناسبة خاصة","Private event");}
    private V43Prefs(){}
}
