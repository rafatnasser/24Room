package com.rafat.munasabati.compat

import java.text.Normalizer
import kotlin.math.abs

object CalendarRetentionPolicy {
    private const val LEGACY_START_TOLERANCE_MS = 3L * 60_000L

    fun shouldKeepImported(endMillis: Long, nowMillis: Long): Boolean = endMillis > nowMillis

    fun eligibleForPrevious(isImported: Boolean, startMillis: Long, nowMillis: Long): Boolean =
        !isImported && startMillis < nowMillis

    fun legacySignature(title: String, startMillis: Long, endMillis: Long, location: String, notes: String): String =
        listOf(normalize(title), startMillis.toString(), endMillis.toString(), normalize(location), normalize(notes)).joinToString("|")

    fun likelyLegacyCalendarMatch(localTitle: String, localStart: Long, providerTitle: String, providerStart: Long): Boolean =
        normalize(localTitle).isNotBlank() &&
            normalize(localTitle) == normalize(providerTitle) &&
            abs(localStart - providerStart) <= LEGACY_START_TOLERANCE_MS

    private fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC)
        .lowercase()
        .replace(Regex("[\\u064B-\\u065F\\u0670]"), "")
        .replace(Regex("[^\\p{L}\\p{N}]+"), " ")
        .trim()
        .replace(Regex("\\s+"), " ")
}
