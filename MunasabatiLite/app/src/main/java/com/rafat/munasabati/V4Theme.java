package com.rafat.munasabati;
import android.app.*;import android.content.*;import android.graphics.Color;import android.os.Build;

public final class V4Theme{
 private static final String P="munasabati_v4",MODE="theme_mode",COLOR="primary",DYN="dynamic";
 public static final String PRIMARY_HEX="#0F4D43";
 public static final String SECONDARY_HEX="#2E7D6B";
 public static final String MINT_HEX="#A8D5C6";
 public static final String CORAL_HEX="#FF7B5C";
 public static final String IVORY_HEX="#FFF7F2";

 public static void apply(Context c){String m=c.getSharedPreferences(P,0).getString(MODE,"system");if(Build.VERSION.SDK_INT>=31){UiModeManager u=(UiModeManager)c.getSystemService(Context.UI_MODE_SERVICE);if("dark".equals(m))u.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES);else if("light".equals(m))u.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO);else u.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO);} }
 public static String mode(Context c){return c.getSharedPreferences(P,0).getString(MODE,"system");}
 public static void setMode(Context c,String m){c.getSharedPreferences(P,0).edit().putString(MODE,m).apply();apply(c);}
 public static boolean dynamic(Context c){return c.getSharedPreferences(P,0).getBoolean(DYN,false);}
 public static void setDynamic(Context c,boolean v){c.getSharedPreferences(P,0).edit().putBoolean(DYN,v).apply();}
 public static int primary(Context c){if(dynamic(c)&&Build.VERSION.SDK_INT>=31)try{return c.getColor(android.R.color.system_accent1_600);}catch(Exception ignored){}String h=c.getSharedPreferences(P,0).getString(COLOR,PRIMARY_HEX);try{return Color.parseColor(h);}catch(Exception e){return Color.rgb(15,77,67);}}
 public static int secondary(){return Color.rgb(46,125,107);}
 public static int mint(){return Color.rgb(168,213,198);}
 public static int coral(){return Color.rgb(255,123,92);}
 public static int ivory(){return Color.rgb(255,247,242);}
 public static void setPrimary(Context c,String h){c.getSharedPreferences(P,0).edit().putString(COLOR,h).apply();}
 private V4Theme(){}
}
