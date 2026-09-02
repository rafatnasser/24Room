package com.rafat.munasabati.compat

import org.junit.Assert.*
import org.junit.Test

class CalendarEventOriginTest {
    @Test fun detects_new_imported_marker() {
        assertTrue(CalendarEventOrigin.isImportedFingerprint("imported:4:99:1700000000000"))
    }

    @Test fun detects_legacy_imported_marker() {
        assertTrue(CalendarEventOrigin.isImportedFingerprint("99:1700000000000:Meeting"))
    }

    @Test fun keeps_local_linked_event_local() {
        assertFalse(CalendarEventOrigin.isImportedFingerprint("local:123456"))
        assertFalse(CalendarEventOrigin.isImportedFingerprint("123456"))
    }
}
