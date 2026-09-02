package com.rafat.munasabati.smart

import com.rafat.munasabati.model.EventModel
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class SmartSuggestionEngineTest {
    private val zone = ZoneId.of("Asia/Riyadh")

    @Test fun annualEventWithinSevenDaysCreatesSuggestion() {
        val now = ZonedDateTime.of(2026, 9, 2, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val original = ZonedDateTime.of(2024, 9, 9, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val event = EventModel(title="Annual", startEpochMillis=original, recurrence="yearly")
        val s = SmartSuggestionEngine.suggestionFor(event, now, zone)
        assertNotNull(s)
        assertEquals(7L, s!!.daysUntil)
        assertEquals(2026, Instant.ofEpochMilli(s.nextOccurrenceMillis).atZone(zone).year)
    }

    @Test fun existingSevenDayReminderSuppressesSuggestion() {
        val now = ZonedDateTime.of(2026, 9, 2, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val original = ZonedDateTime.of(2024, 9, 5, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val event = EventModel(title="Annual", startEpochMillis=original, recurrence="yearly", remindersCsv="60,10080")
        assertNull(SmartSuggestionEngine.suggestionFor(event, now, zone))
    }

    @Test fun leapDayRollsSafely() {
        val now = ZonedDateTime.of(2027, 2, 20, 9, 0, 0, 0, zone).toInstant().toEpochMilli()
        val original = ZonedDateTime.of(2024, 2, 29, 20, 0, 0, 0, zone).toInstant().toEpochMilli()
        val next = SmartSuggestionEngine.nextAnnualOccurrenceMillis(original, now, zone)
        val d = Instant.ofEpochMilli(next).atZone(zone).toLocalDate()
        assertEquals(LocalDate.of(2027, 2, 28), d)
    }
}
