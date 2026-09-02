package com.rafat.munasabati.compat

object CalendarEventOrigin {
    private val legacyImported = Regex("^\\d+:\\d+:")

    fun importedFingerprint(calendarId: Long, eventId: Long, startMillis: Long): String =
        "imported:$calendarId:$eventId:$startMillis"

    fun localFingerprint(hash: String): String = "local:$hash"

    fun isImportedFingerprint(value: String): Boolean =
        value.startsWith("imported:") || legacyImported.containsMatchIn(value)
}
