package com.rafat.munasabati.compat

object CalendarRetentionPolicy {
    fun shouldKeepImported(endMillis: Long, nowMillis: Long): Boolean = endMillis > nowMillis

    fun eligibleForPrevious(isImported: Boolean, startMillis: Long, nowMillis: Long): Boolean =
        !isImported && startMillis < nowMillis

    fun legacySignature(title: String, startMillis: Long, endMillis: Long, location: String, notes: String): String =
        listOf(normalize(title), startMillis.toString(), endMillis.toString(), normalize(location), normalize(notes)).joinToString("|")

    private fun normalize(value: String): String = value.trim().lowercase().replace(Regex("\\s+"), " ")
}
