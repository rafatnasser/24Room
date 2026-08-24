package com.rafat.munasabati;

import android.content.Context;

public final class AppSettings {
    private static final String PREFS="munasabati_settings";
    private static final String LANG="language";
    private static final String HIJRI_OFFSET="hijri_offset";
    private static final String AUTO_BACKUP_ENABLED="auto_backup_enabled";
    private static final String AUTO_BACKUP_TREE="auto_backup_tree";
    private static final String AUTO_BACKUP_DAYS="auto_backup_days";
    private static final String AUTO_BACKUP_KEEP="auto_backup_keep";
    private static final String LAST_BACKUP_TIME="last_backup_time";
    private static final String LAST_BACKUP_STATUS="last_backup_status";

    public static String language(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(LANG,"ar");}
    public static boolean isArabic(Context c){return "ar".equals(language(c));}
    public static void setLanguage(Context c,String value){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(LANG,value).apply();}
    public static int hijriOffset(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(HIJRI_OFFSET,0);}
    public static void setHijriOffset(Context c,int value){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(HIJRI_OFFSET,value).apply();}

    public static boolean autoBackupEnabled(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getBoolean(AUTO_BACKUP_ENABLED,false);}
    public static void setAutoBackupEnabled(Context c,boolean v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putBoolean(AUTO_BACKUP_ENABLED,v).apply();}
    public static String autoBackupTree(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(AUTO_BACKUP_TREE,"");}
    public static void setAutoBackupTree(Context c,String uri){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putString(AUTO_BACKUP_TREE,uri==null?"":uri).apply();}
    public static int autoBackupDays(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(AUTO_BACKUP_DAYS,7);}
    public static void setAutoBackupDays(Context c,int v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(AUTO_BACKUP_DAYS,Math.max(1,v)).apply();}
    public static int autoBackupKeep(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getInt(AUTO_BACKUP_KEEP,5);}
    public static void setAutoBackupKeep(Context c,int v){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putInt(AUTO_BACKUP_KEEP,Math.max(1,v)).apply();}
    public static long lastBackupTime(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getLong(LAST_BACKUP_TIME,0L);}
    public static String lastBackupStatus(Context c){return c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).getString(LAST_BACKUP_STATUS,"");}
    public static void setLastBackup(Context c,long time,String status){c.getSharedPreferences(PREFS,Context.MODE_PRIVATE).edit().putLong(LAST_BACKUP_TIME,time).putString(LAST_BACKUP_STATUS,status==null?"":status).apply();}

    public static String tr(Context c,String ar,String en){return isArabic(c)?ar:en;}
    private AppSettings(){}
}
