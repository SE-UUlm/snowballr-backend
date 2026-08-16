package se.uulm.snowballr.backend.scheduler

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Service interface for maintaining project data.
 *
 * Implementations may provide periodic cleanup or archival tasks to
 * remove sensitive information, anonymize archived projects, or
 * permanently delete cleared projects.
 */
interface IProjectMaintenanceService {
    /**
     * Clears sensitive project data for archived projects that were soft-deleted.
     */
    suspend fun clearSoftDeletedProjects()

    /**
     * Hard-deletes projects that were previously cleared and are no longer in use
     * and no longer referenced by any other entity.
     */
    suspend fun hardDeleteClearedProjects()
}

/**
 * Default implementation of [IProjectMaintenanceService].
 *
 * Provides standard maintenance operations for project records stored in the database.
 */
class ProjectMaintenanceService : IProjectMaintenanceService, KoinComponent {
    private val envReader: EnvReader by inject()
    private val projectTableRepo: IProjectTableRepo by inject()

    override suspend fun clearSoftDeletedProjects() {
        val thresholdDate = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(
            envReader.env.lifetime.sensitiveInformationRetentionDays.toLong(),
        )
        projectTableRepo.clearSoftDeletedProjects(thresholdDate)
    }

    override suspend fun hardDeleteClearedProjects() {
        projectTableRepo.hardDeleteClearedProjects()
    }
}
