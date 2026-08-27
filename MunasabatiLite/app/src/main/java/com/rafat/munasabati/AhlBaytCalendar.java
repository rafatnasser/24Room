package com.rafat.munasabati;

import android.content.Context;
import android.graphics.Color;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/** Built-in Twelver Shia calendar for the Fourteen Infallibles (peace be upon them). */
public final class AhlBaytCalendar {
    private static final String PREFS="munasabati_ahlbayt_calendar";
    private static final String KEY_BIRTHS="show_births";
    private static final String KEY_DEATHS="show_deaths";

    public enum Kind { BIRTH, DEATH }

    public static final class Occasion {
        public final String ar,en;
        public final int month,day;
        public final Kind kind;
        public final String noteAr,noteEn;
        Occasion(String ar,String en,int month,int day,Kind kind,String noteAr,String noteEn){
            this.ar=ar;this.en=en;this.month=month;this.day=day;this.kind=kind;
            this.noteAr=noteAr==null?"":noteAr;this.noteEn=noteEn==null?"":noteEn;
        }
    }

    public static final class Occurrence {
        public final Occasion occasion;
        public final long time;
        Occurrence(Occasion occasion,long time){this.occasion=occasion;this.time=time;}
    }

    private static Occasion b(String ar,String en,int m,int d){return new Occasion(ar,en,m,d,Kind.BIRTH,"","");}
    private static Occasion bn(String ar,String en,int m,int d,String nar,String nen){return new Occasion(ar,en,m,d,Kind.BIRTH,nar,nen);}
    private static Occasion d(String ar,String en,int m,int day){return new Occasion(ar,en,m,day,Kind.DEATH,"","");}
    private static Occasion dn(String ar,String en,int m,int day,String nar,String nen){return new Occasion(ar,en,m,day,Kind.DEATH,nar,nen);}

    private static final Occasion[] DATA={
        b("مولد النبي محمد ﷺ","Birth of Prophet Muhammad (pbuh&hp)",3,17),
        d("وفاة النبي محمد ﷺ","Death anniversary of Prophet Muhammad (pbuh&hp)",2,28),
        b("مولد السيدة فاطمة الزهراء عليها السلام","Birth of Lady Fatimah al-Zahra (p)",6,20),
        dn("استشهاد السيدة فاطمة الزهراء عليها السلام","Martyrdom of Lady Fatimah al-Zahra (p)",6,3,"توجد رواية أخرى مشهورة في 13 جمادى الأولى.","Another well-known narration places the commemoration on 13 Jumada I."),
        b("مولد الإمام علي أمير المؤمنين عليه السلام","Birth of Imam Ali (p)",7,13),
        d("استشهاد الإمام علي أمير المؤمنين عليه السلام","Martyrdom of Imam Ali (p)",9,21),
        b("مولد الإمام الحسن المجتبى عليه السلام","Birth of Imam Hasan al-Mujtaba (p)",9,15),
        dn("استشهاد الإمام الحسن المجتبى عليه السلام","Martyrdom of Imam Hasan al-Mujtaba (p)",2,7,"توجد رواية أخرى مشهورة في 28 صفر.","Another well-known narration places the commemoration on 28 Safar."),
        b("مولد الإمام الحسين سيد الشهداء عليه السلام","Birth of Imam Husayn (p)",8,3),
        d("استشهاد الإمام الحسين سيد الشهداء عليه السلام","Martyrdom of Imam Husayn (p) — عاشوراء",1,10),
        b("مولد الإمام علي زين العابدين عليه السلام","Birth of Imam Ali Zayn al-Abidin (p)",8,5),
        d("استشهاد الإمام علي زين العابدين عليه السلام","Martyrdom of Imam Ali Zayn al-Abidin (p)",1,25),
        bn("مولد الإمام محمد الباقر عليه السلام","Birth of Imam Muhammad al-Baqir (p)",7,1,"توجد رواية أخرى في بعض المصادر في 3 صفر.","Some sources record another narration on 3 Safar."),
        d("استشهاد الإمام محمد الباقر عليه السلام","Martyrdom of Imam Muhammad al-Baqir (p)",12,7),
        b("مولد الإمام جعفر الصادق عليه السلام","Birth of Imam Jafar al-Sadiq (p)",3,17),
        d("استشهاد الإمام جعفر الصادق عليه السلام","Martyrdom of Imam Jafar al-Sadiq (p)",10,25),
        b("مولد الإمام موسى الكاظم عليه السلام","Birth of Imam Musa al-Kadhim (p)",2,7),
        d("استشهاد الإمام موسى الكاظم عليه السلام","Martyrdom of Imam Musa al-Kadhim (p)",7,25),
        b("مولد الإمام علي الرضا عليه السلام","Birth of Imam Ali al-Rida (p)",11,11),
        dn("استشهاد الإمام علي الرضا عليه السلام","Martyrdom of Imam Ali al-Rida (p)",2,17,"توجد روايات أخرى في بعض التقاويم في أواخر صفر.","Other calendars preserve narrations in the final days of Safar."),
        b("مولد الإمام محمد الجواد عليه السلام","Birth of Imam Muhammad al-Jawad (p)",7,10),
        dn("استشهاد الإمام محمد الجواد عليه السلام","Martyrdom of Imam Muhammad al-Jawad (p)",11,30,"إذا كان ذو القعدة 29 يومًا تُعرض المناسبة في آخر يوم من الشهر.","If Dhu al-Qi'dah has 29 days, the occasion is shown on the month's final day."),
        bn("مولد الإمام علي الهادي عليه السلام","Birth of Imam Ali al-Hadi (p)",7,2,"توجد روايات أخرى لتاريخ المولد في بعض المصادر.","Other birth-date narrations exist in some sources."),
        d("استشهاد الإمام علي الهادي عليه السلام","Martyrdom of Imam Ali al-Hadi (p)",7,3),
        b("مولد الإمام الحسن العسكري عليه السلام","Birth of Imam Hasan al-Askari (p)",4,8),
        d("استشهاد الإمام الحسن العسكري عليه السلام","Martyrdom of Imam Hasan al-Askari (p)",3,8),
        b("مولد الإمام محمد المهدي عجل الله فرجه الشريف","Birth of Imam Muhammad al-Mahdi (may Allah hasten his reappearance)",8,15)
    };

    public static boolean showBirths(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(KEY_BIRTHS,true);}
    public static boolean showDeaths(Context c){return c.getSharedPreferences(PREFS,0).getBoolean(KEY_DEATHS,true);}
    public static void setShowBirths(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(KEY_BIRTHS,v).apply();}
    public static void setShowDeaths(Context c,boolean v){c.getSharedPreferences(PREFS,0).edit().putBoolean(KEY_DEATHS,v).apply();}
    public static int birthCount(){int n=0;for(Occasion o:DATA)if(o.kind==Kind.BIRTH)n++;return n;}
    public static int deathCount(){int n=0;for(Occasion o:DATA)if(o.kind==Kind.DEATH)n++;return n;}

    public static String title(Context c,Occasion o){return AppSettings.isArabic(c)?o.ar:o.en;}
    public static String note(Context c,Occasion o){return AppSettings.isArabic(c)?o.noteAr:o.noteEn;}
    public static String kindLabel(Context c,Occasion o){
        if(o.kind==Kind.BIRTH)return AppSettings.tr(c,"مولد من مواليد أهل البيت عليهم السلام","Ahl al-Bayt birth anniversary");
        return AppSettings.tr(c,"ذكرى وفاة / استشهاد من أهل البيت عليهم السلام","Ahl al-Bayt death / martyrdom anniversary");
    }
    public static int color(Occasion o){return o.kind==Kind.BIRTH?Color.rgb(198,145,43):Color.rgb(111,45,58);}
    public static String icon(Occasion o){return o.kind==Kind.BIRTH?"✦":"🕯";}

    public static List<Occurrence> occurrencesOnDay(Context c,long millis){
        Calendar x=Calendar.getInstance();x.setTimeInMillis(millis);x.set(Calendar.HOUR_OF_DAY,12);x.set(Calendar.MINUTE,0);x.set(Calendar.SECOND,0);x.set(Calendar.MILLISECOND,0);
        ArrayList<Occurrence> out=new ArrayList<>();
        HijrahDate h=DateTools.hijriDate(c,x.getTimeInMillis());
        int month=h.get(ChronoField.MONTH_OF_YEAR),actualDay=h.get(ChronoField.DAY_OF_MONTH),monthLength=h.lengthOfMonth();
        for(Occasion o:DATA){
            if(o.kind==Kind.BIRTH&&!showBirths(c))continue;if(o.kind==Kind.DEATH&&!showDeaths(c))continue;
            int target=o.day==30&&monthLength==29?29:o.day;
            if(o.month==month&&target==actualDay)out.add(new Occurrence(o,x.getTimeInMillis()));
        }
        return out;
    }

    public static List<Occurrence> occurrencesBetween(Context c,long start,long end){
        ArrayList<Occurrence> out=new ArrayList<>();if(end<=start)return out;
        Calendar day=Calendar.getInstance();day.setTimeInMillis(start);day.set(Calendar.HOUR_OF_DAY,12);day.set(Calendar.MINUTE,0);day.set(Calendar.SECOND,0);day.set(Calendar.MILLISECOND,0);
        int guard=0;while(day.getTimeInMillis()<end&&guard++<430){out.addAll(occurrencesOnDay(c,day.getTimeInMillis()));day.add(Calendar.DAY_OF_YEAR,1);}return out;
    }

    private AhlBaytCalendar(){}
}
