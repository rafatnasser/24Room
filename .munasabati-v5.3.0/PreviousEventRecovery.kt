package com.rafat.munasabati.compat

import android.content.Context
import com.rafat.munasabati.data.EventRepository
import com.rafat.munasabati.migration.LegacyV4Migration

/**
 * One-time recovery for local events that may have been lost by the v5.2.x calendar cleanup heuristic.
 * Existing v5 rows win over older legacy snapshots; only genuinely missing legacy rows are restored.
 */
object PreviousEventRecovery {
    private const val RECOVERY_META = "v530_previous_event_recovery"
    private const val LEGACY_MIGRATION_META = "legacy_v4_migration"

    fun recoverMissingLegacyEventsOnce(
        context: Context,
        repo: EventRepository,
        now: Long = System.currentTimeMillis()
    ): Int {
        if (repo.getMeta(RECOVERY_META) == "done") return 0

        val current = repo.allEvents()
        val currentIds = current.mapTo(mutableSetOf()) { it.id }
        val oldMigrationState = repo.getMeta(LEGACY_MIGRATION_META).orEmpty()

        return runCatching {
            // Re-run the existing trusted v4 importer, then re-apply every current row so
            // newer user edits always take precedence over the old snapshot.
            repo.putMeta(LEGACY_MIGRATION_META, "")
            LegacyV4Migration(context, repo).runOnce()
            current.forEach(repo::upsertEvent)

            val recovered = repo.allEvents().count { event ->
                event.id !in currentIds && event.startEpochMillis < now
            }
            repo.putMeta(RECOVERY_META, "done")
            recovered
        }.getOrElse {
            // Never leave the migration state altered if recovery fails.
            repo.putMeta(LEGACY_MIGRATION_META, oldMigrationState)
            current.forEach(repo::upsertEvent)
            0
        }
    }
}
