package com.rafat.munasabati;

import android.content.Context;
import java.util.*;

/** Per-category smart reminder rules stored locally. */
public final class SmartReminderRules {
    private static final String P="munasabati_smart_rules";
    public static int[] values(Context c,String category){
        String def=defaults(category),s=c.getSharedPreferences(P,0).getString(category,def);
        ArrayList<Integer> out=new ArrayList<>();for(String x:s.split(","))try{out.add(Integer.parseInt(x.trim()));}catch(Exception ignored){}
        if(out.isEmpty())for(String x:def.split(","))try{out.add(Integer.parseInt(x.trim()));}catch(Exception ignored){}
        int[] a=new int[out.size()];for(int i=0;i<a.length;i++)a[i]=out.get(i);return a;
    }
    public static void set(Context c,String category,Collection<Integer> vals){StringBuilder b=new StringBuilder();for(Integer v:vals){if(v==null)continue;if(b.length()>0)b.append(',');b.append(v);}c.getSharedPreferences(P,0).edit().putString(category,b.toString()).apply();}
    public static void reset(Context c,String category){c.getSharedPreferences(P,0).edit().remove(category).apply();}
    public static String defaults(String category){
        if("birthday".equals(category))return "10080,1440";
        if("travel".equals(category))return "1440,180";
        if("meeting".equals(category))return "60,15";
        if("mawlid".equals(category)||"martyrdom".equals(category)||"death".equals(category)||"fatiha".equals(category)||"fortieth".equals(category))return "1440,180";
        if("medical".equals(category))return "1440,60";
        if("wedding".equals(category)||"wedding_anniversary".equals(category)||"marriage".equals(category))return "2880,180";
        return "60,30";
    }
    public static String summary(Context c,String category){int[] a=values(c,category);StringBuilder b=new StringBuilder();for(int i=0;i<a.length;i++){if(i>0)b.append(" + ");b.append(label(c,a[i]));}return b.toString();}
    public static String label(Context c,int m){boolean ar=AppSettings.isArabic(c);if(m==10080)return ar?"أسبوع":"1 week";if(m==2880)return ar?"يومان":"2 days";if(m==1440)return ar?"يوم":"1 day";if(m==180)return ar?"3 ساعات":"3 hours";if(m==60)return ar?"ساعة":"1 hour";if(m==30)return ar?"30 دقيقة":"30 min";if(m==15)return ar?"15 دقيقة":"15 min";if(m==10)return ar?"10 دقائق":"10 min";return m+" min";}
    private SmartReminderRules(){}
}
