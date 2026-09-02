package com.rafat.munasabati.compat

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class UnifiedCalendarProjectionTest {
    @Test
    fun ahlBaytOccasion_isProjectedIntoMainCalendar() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        AhlBaytCategory.values().forEach { AhlBaytCalendar.setCategoryEnabled(context, it, true) }

        val date = (0L..400L)
            .map { LocalDate.now().plusDays(it) }
            .firstOrNull { AhlBaytCalendar.occasionsForDate(context, it).isNotEmpty() }
        assertNotNull("Expected at least one Ahl al-Bayt occasion within 400 days", date)

        val projected = UnifiedCalendarProjection.eventsForDate(context, date!!, emptyList())
        assertTrue(projected.isNotEmpty())
        assertTrue(projected.any { UnifiedCalendarProjection.isAhlBaytEvent(it) })
    }
}
