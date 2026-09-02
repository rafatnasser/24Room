package com.rafat.munasabati.compat

import org.junit.Assert.*
import org.junit.Test

class CalendarRetentionPolicyTest {
    @Test fun importedEventIsRemovedWhenEnded() {
        val now = 2_000L
        assertFalse(CalendarRetentionPolicy.shouldKeepImported(2_000L, now))
        assertFalse(CalendarRetentionPolicy.shouldKeepImported(1_999L, now))
        assertTrue(CalendarRetentionPolicy.shouldKeepImported(2_001L, now))
    }

    @Test fun importedEventNeverBelongsToPrevious() {
        val now = 10_000L
        assertFalse(CalendarRetentionPolicy.eligibleForPrevious(true, 1_000L, now))
        assertTrue(CalendarRetentionPolicy.eligibleForPrevious(false, 1_000L, now))
    }

    @Test fun legacySignatureNormalizesText() {
        val a = CalendarRetentionPolicy.legacySignature(" Meeting  A ", 100L, 200L, " Room 1 ", " Note ")
        val b = CalendarRetentionPolicy.legacySignature("meeting a", 100L, 200L, "room 1", "note")
        assertEquals(a, b)
    }
}
