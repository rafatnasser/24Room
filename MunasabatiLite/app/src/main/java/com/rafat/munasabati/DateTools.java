package com.rafat.munasabati;

import android.content.Context;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.chrono.HijrahDate;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

public final class DateTools {
    private static final String[] HIJRI_AR = {"محرم","صفر","ربيع الأول","ربيع الآخر","جمادى الأولى","جمادى الآخرة","رجب","شعبان","رمضان","شوال","ذو القعدة","ذو الحجة"};
    private static final String[] HIJRI_EN = {"Muharram","Safar","Rabi' I","Rabi' II","Jumada I","Jumada II","Rajab","Sha'ban","Ramadan","Shawwal","Dhu al-Qi'dah","Dhu al-Hijjah"};

    public static String gregorian(Context c, long millis, boolean withTime) {
        Locale l = AppSettings.isArabic(c) ? new Locale("ar") : Locale.ENGLISH;
        String p = withTime ? "EEEE، d MMMM yyyy • h:mm a" : "EEEE، d MMMM yyyy";
        if (!AppSettings.isArabic(c)) p = withTime ? "EEEE, d MMMM yyyy • h:mm a" : "EEEE, d MMMM yyyy";
        return new SimpleDateFormat(p, l).format(new Date(millis));
    }
    public static String gregorianShort(Context c, long millis) {
        Locale l = AppSettings.isArabic(c) ? new Locale("ar") : Locale.ENGLISH;
        return new SimpleDateFormat("d MMM yyyy", l).format(new Date(millis));
    }
    public static String time(Context c, long millis) {
        Locale l = AppSettings.isArabic(c) ? new Locale("ar") : Locale.ENGLISH;
        return new SimpleDateFormat("h:mm a", l).format(new Date(millis));
    }
    public static HijrahDate hijriDate(Context c, long millis) {
        LocalDate g = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();
        return HijrahDate.from(g).plus(AppSettings.hijriOffset(c), ChronoUnit.DAYS);
    }
    public static String hijri(Context c, long millis) {
        HijrahDate h = hijriDate(c, millis);
        int d=h.get(ChronoField.DAY_OF_MONTH), m=h.get(ChronoField.MONTH_OF_YEAR), y=h.get(ChronoField.YEAR_OF_ERA);
        String month = AppSettings.isArabic(c) ? HIJRI_AR[m-1] : HIJRI_EN[m-1];
        return d + " " + month + " " + y + (AppSettings.isArabic(c) ? " هـ" : " AH");
    }
    public static String hijriNumeric(Context c, long millis) {
        HijrahDate h = hijriDate(c, millis);
        return h.get(ChronoField.DAY_OF_MONTH)+"/"+h.get(ChronoField.MONTH_OF_YEAR)+"/"+h.get(ChronoField.YEAR_OF_ERA);
    }
    public static String dayArabic(long millis) { return new SimpleDateFormat("EEEE", new Locale("ar")).format(new Date(millis)); }
    public static String dayEnglish(long millis) { return new SimpleDateFormat("EEEE", Locale.ENGLISH).format(new Date(millis)); }
    public static int dayOfWeek(long millis) {
        java.util.Calendar c=java.util.Calendar.getInstance(); c.setTimeInMillis(millis); return c.get(java.util.Calendar.DAY_OF_WEEK);
    }
    private DateTools() {}
}
