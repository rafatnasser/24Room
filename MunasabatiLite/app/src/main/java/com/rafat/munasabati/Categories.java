package com.rafat.munasabati;

import android.content.Context;

public final class Categories {
    public static final String[] CODES = {
            "none","mawlid","marriage","engagement","lunch","dinner","breakfast",
            "entertainment","martyrdom","death","fatiha","fortieth","anniversary","weekly_habit"
    };
    private static final String[] AR = {
            "بدون فئة","مولد","زواج","خطوبة","غداء","عشاء","فطور",
            "ترفيه","استشهاد","وفاة","فاتحة","أربعينية","سنوية","عادة أسبوعية"
    };
    private static final String[] EN = {
            "Uncategorized","Mawlid","Wedding","Engagement","Lunch","Dinner","Breakfast",
            "Entertainment","Martyrdom","Death","Fatiha","Fortieth memorial","Anniversary","Weekly habit"
    };

    public static String label(Context c, String code) {
        int i = indexOf(code); return AppSettings.isArabic(c) ? AR[i] : EN[i];
    }
    public static String arabicLabel(String code) { return AR[indexOf(code)]; }
    public static String englishLabel(String code) { return EN[indexOf(code)]; }
    public static String[] labels(Context c, boolean includeAll) {
        String[] base = AppSettings.isArabic(c) ? AR : EN;
        if (!includeAll) return base.clone();
        String[] out = new String[base.length + 1];
        out[0] = AppSettings.tr(c, "كل الفئات", "All categories");
        System.arraycopy(base, 0, out, 1, base.length);
        return out;
    }
    public static int indexOf(String code) {
        if (code != null) for (int i=0;i<CODES.length;i++) if (CODES[i].equals(code)) return i;
        return 0;
    }
    private Categories() {}
}
