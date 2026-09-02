package com.rafat.munasabati.ui

internal object V522RelativeTimeFormatter {
    fun formatMinutes(totalMinutes: Long, english: Boolean): String {
        val minutes = totalMinutes.coerceAtLeast(0L)
        return when {
            minutes < 60L -> formatUnit(minutes, english, "دقيقة", "دقيقتان", "دقائق", "دقيقة", "minute")
            minutes < 1440L -> formatUnit(minutes / 60L, english, "ساعة", "ساعتان", "ساعات", "ساعة", "hour")
            else -> formatUnit(minutes / 1440L, english, "يوم", "يومان", "أيام", "يومًا", "day")
        }
    }

    private fun formatUnit(
        value: Long,
        english: Boolean,
        arabicSingular: String,
        arabicDual: String,
        arabicFew: String,
        arabicMany: String,
        englishSingular: String,
    ): String {
        if (english) return "$value $englishSingular${if (value == 1L) "" else "s"}"
        if (value == 0L) return "0 $arabicFew"
        if (value == 1L) return "1 $arabicSingular"
        if (value == 2L) return arabicDual

        val lastTwo = value % 100L
        return when {
            lastTwo in 3L..10L -> "$value $arabicFew"
            lastTwo in 11L..99L -> "$value $arabicMany"
            else -> "$value $arabicSingular"
        }
    }
}
