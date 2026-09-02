package com.rafat.munasabati.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class RelativeTimeFormatterTest {
    @Test
    fun arabicDaysGrammar() {
        assertEquals("1 \u064a\u0648\u0645", V522RelativeTimeFormatter.formatMinutes(1440, false))
        assertEquals("\u064a\u0648\u0645\u0627\u0646", V522RelativeTimeFormatter.formatMinutes(2880, false))
        assertEquals("3 \u0623\u064a\u0627\u0645", V522RelativeTimeFormatter.formatMinutes(4320, false))
        assertEquals("11 \u064a\u0648\u0645\u064b\u0627", V522RelativeTimeFormatter.formatMinutes(15840, false))
    }

    @Test
    fun englishDaysGrammar() {
        assertEquals("1 day", V522RelativeTimeFormatter.formatMinutes(1440, true))
        assertEquals("2 days", V522RelativeTimeFormatter.formatMinutes(2880, true))
        assertEquals("7 days", V522RelativeTimeFormatter.formatMinutes(10080, true))
    }
}
