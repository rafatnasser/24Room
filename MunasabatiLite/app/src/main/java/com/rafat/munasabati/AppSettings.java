package com.rafat.munasabati;

import android.content.Context;

public final class AppSettings {
    private static final String PREFS = "munasabati_settings";
    private static final String LANG = "language";
    private static final String HIJRI_OFFSET = "hijri_offset";

    public static String language(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(LANG, "ar");
    }
    public static boolean isArabic(Context c) { return "ar".equals(language(c)); }
    public static void setLanguage(Context c, String value) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(LANG, value).apply();
    }
    public static int hijriOffset(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getInt(HIJRI_OFFSET, 0);
    }
    public static void setHijriOffset(Context c, int value) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(HIJRI_OFFSET, value).apply();
    }
    public static String tr(Context c, String ar, String en) { return isArabic(c) ? ar : en; }
    private AppSettings() {}
}
