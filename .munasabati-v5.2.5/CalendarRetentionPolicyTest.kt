package com.rafat.munasabati.compat

import org.junit.Assert.*
import org.junit.Test

class CalendarRetentionPolicyTest {
    @Test fun importedEventIsRemovedAtEnd() {
        val now = 1_000_000L
        assertFalse(CalendarRetentionPolicy.shouldKeepImported(now, now))
        assertFalse(CalendarRetentionPolicy.shouldKeepImported(now - 1, now))
        assertTrue(CalendarRetentionPolicy.shouldKeepImported(now + 1, now))
    }

    @Test fun importedNeverBelongsToPrevious() {
        val now = 2_000_000L
        assertFalse(CalendarRetentionPolicy.eligibleForPrevious(true, now - 10, now))
        assertTrue(CalendarRetentionPolicy.eligibleForPrevious(false, now - 10, now))
    }

    @Test fun legacyMatchIgnoresSpacingPunctuationAndSmallTimeDrift() {
        assertTrue(
            CalendarRetentionPolicy.likelyLegacyCalendarMatch(
                "اجتماع   الإدارة!", 1_000_000L,
                "اجتماع الإدارة", 1_120_000L
            )
        )
    }

    @Test fun legacyMatchRejectsDifferentTitleOrLargeTimeDrift() {
        assertFalse(CalendarRetentionPolicy.likelyLegacyCalendarMatch("اجتماع الإدارة", 1_000_000L, "اجتماع آخر", 1_000_000L))
        assertFalse(CalendarRetentionPolicy.likelyLegacyCalendarMatch("اجتماع الإدارة", 1_000_000L, "اجتماع الإدارة", 1_300_001L))
    }
}
