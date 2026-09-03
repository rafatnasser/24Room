package com.rafat.munasabati.religious

import android.icu.util.Calendar
import android.icu.util.IslamicCalendar
import com.rafat.munasabati.model.EventCategory
import com.rafat.munasabati.model.EventModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Virtual recurring Hijri occasions shown inside the app calendar.
 * These are not persisted in Room, so they can never be confused with
 * user-created history or imported Android/Google/Outlook calendar rows.
 */
object AhlulBaytCalendarSource {
    data class Occasion(val month: Int, val day: Int, val ar: String, val en: String)

    val occasions: List<Occasion> = listOf(
        Occasion(1, 10, "عاشوراء - استشهاد الإمام الحسين عليه السلام", "Ashura - Martyrdom of Imam Hussain (A.S.)"),
        Occasion(2, 20, "أربعينية الإمام الحسين عليه السلام", "Arbaeen of Imam Hussain (A.S.)"),
        Occasion(2, 28, "وفاة النبي محمد صلى الله عليه وآله", "Passing of Prophet Muhammad (P.B.U.H. & H.P.)"),
        Occasion(3, 8, "استشهاد الإمام الحسن العسكري عليه السلام", "Martyrdom of Imam Hasan al-Askari (A.S.)"),
        Occasion(3, 9, "بدء إمامة الإمام المهدي عجل الله فرجه", "Beginning of the Imamate of Imam al-Mahdi (A.J.)"),
        Occasion(3, 17, "ولادة النبي محمد صلى الله عليه وآله والإمام الصادق عليه السلام", "Birth of Prophet Muhammad and Imam al-Sadiq (A.S.)"),
        Occasion(4, 8, "ولادة الإمام الحسن العسكري عليه السلام", "Birth of Imam Hasan al-Askari (A.S.)"),
        Occasion(5, 13, "ذكرى استشهاد السيدة فاطمة الزهراء عليها السلام - الرواية الأولى", "Martyrdom of Lady Fatima al-Zahra (A.S.) - First narration"),
        Occasion(6, 3, "استشهاد السيدة فاطمة الزهراء عليها السلام", "Martyrdom of Lady Fatima al-Zahra (A.S.)"),
        Occasion(6, 20, "ولادة السيدة فاطمة الزهراء عليها السلام", "Birth of Lady Fatima al-Zahra (A.S.)"),
        Occasion(7, 1, "ولادة الإمام محمد الباقر عليه السلام", "Birth of Imam Muhammad al-Baqir (A.S.)"),
        Occasion(7, 3, "استشهاد الإمام علي الهادي عليه السلام", "Martyrdom of Imam Ali al-Hadi (A.S.)"),
        Occasion(7, 10, "ولادة الإمام محمد الجواد عليه السلام", "Birth of Imam Muhammad al-Jawad (A.S.)"),
        Occasion(7, 13, "ولادة الإمام علي بن أبي طالب عليه السلام", "Birth of Imam Ali ibn Abi Talib (A.S.)"),
        Occasion(7, 25, "استشهاد الإمام موسى الكاظم عليه السلام", "Martyrdom of Imam Musa al-Kadhim (A.S.)"),
        Occasion(7, 27, "المبعث النبوي الشريف", "The Prophet's Mission (Mab'ath)"),
        Occasion(8, 3, "ولادة الإمام الحسين عليه السلام", "Birth of Imam Hussain (A.S.)"),
        Occasion(8, 5, "ولادة الإمام علي زين العابدين عليه السلام", "Birth of Imam Ali Zain al-Abidin (A.S.)"),
        Occasion(8, 15, "ولادة الإمام المهدي عجل الله فرجه", "Birth of Imam al-Mahdi (A.J.)"),
        Occasion(9, 15, "ولادة الإمام الحسن المجتبى عليه السلام", "Birth of Imam Hasan al-Mujtaba (A.S.)"),
        Occasion(9, 19, "ضربة الإمام علي عليه السلام", "Imam Ali (A.S.) wounded"),
        Occasion(9, 21, "استشهاد الإمام علي عليه السلام", "Martyrdom of Imam Ali (A.S.)"),
        Occasion(10, 25, "استشهاد الإمام جعفر الصادق عليه السلام", "Martyrdom of Imam Ja'far al-Sadiq (A.S.)"),
        Occasion(11, 11, "ولادة الإمام علي الرضا عليه السلام", "Birth of Imam Ali al-Ridha (A.S.)"),
        Occasion(12, 15, "ولادة الإمام علي الهادي عليه السلام", "Birth of Imam Ali al-Hadi (A.S.)"),
        Occasion(12, 18, "عيد الغدير الأغر", "Eid al-Ghadir"),
        Occasion(12, 24, "يوم المباهلة", "Day of Mubahala")
    )

    fun eventsForWindow(start: LocalDate, end: LocalDate, english: Boolean): List<EventModel> {
        if (end.isBefore(start)) return emptyList()
        val zone = ZoneId.systemDefault()
        val startHijri = hijriYear(start, zone)
        val endHijri = hijriYear(end, zone)
        val output = mutableListOf<EventModel>()
        for (year in (startHijri - 1)..(endHijri + 1)) {
            occasions.forEach { occasion ->
                val date = hijriToGregorian(year, occasion.month, occasion.day, zone)
                if (date.isBefore(start) || date.isAfter(end)) return@forEach
                val startMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
                output += EventModel(
                    title = if (english) occasion.en else occasion.ar,
                    notes = if (english) "Ahl al-Bayt occasion • Hijri ${occasion.day}/${occasion.month}/$year" else "من مناسبات أهل البيت عليهم السلام • ${occasion.day}/${occasion.month}/$year هـ",
                    startEpochMillis = startMillis,
                    endEpochMillis = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
                    category = EventCategory.RELIGIOUS,
                    recurrence = "yearly"
                )
            }
        }
        return output.distinctBy { it.title to it.startEpochMillis }.sortedBy { it.startEpochMillis }
    }

    private fun hijriYear(date: LocalDate, zone: ZoneId): Int = IslamicCalendar().apply {
        timeInMillis = date.atStartOfDay(zone).toInstant().toEpochMilli()
    }.get(Calendar.YEAR)

    private fun hijriToGregorian(year: Int, month: Int, day: Int, zone: ZoneId): LocalDate {
        val cal = IslamicCalendar().apply {
            clear()
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, 12)
        }
        return Instant.ofEpochMilli(cal.timeInMillis).atZone(zone).toLocalDate()
    }
}
