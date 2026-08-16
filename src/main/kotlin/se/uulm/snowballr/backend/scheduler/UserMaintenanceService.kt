package se.uulm.snowballr.backend.scheduler

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * Service interface for maintaining user data.
 *
 * Implementations may provide various maintenance operations such as
 * cleaning up old data, anonymizing sensitive information, or performing
 * other user-related maintenance tasks.
 */
interface IUserMaintenanceService {
    /**
     * Clears sensitive data for users that were soft-deleted.
     */
    suspend fun clearSoftDeletedUsers()

    /**
     * Hard-deletes all users that were soft-deleted and are no longer referenced by any other entity.
     */
    suspend fun hardDeleteClearedUsers()
}

/**
 * Default implementation of [IUserMaintenanceService].
 *
 * Provides basic user data maintenance operations using the configured database.
 */
class UserMaintenanceService : IUserMaintenanceService, KoinComponent {
    private val envReader: EnvReader by inject()
    private val userTableRepo: IUserTableRepo by inject()
    private val criterionTableRepo: ICriterionTableRepo by inject()

    override suspend fun clearSoftDeletedUsers() {
        val thresholdDate = OffsetDateTime.now(ZoneId.systemDefault()).minusDays(
            envReader.env.lifetime.sensitiveInformationRetentionDays.toLong(),
        )
        userTableRepo.clearSoftDeletedUsers(thresholdDate)
    }

    override suspend fun hardDeleteClearedUsers() {
        val userIdsToDelete = userTableRepo.getUserIdsToDelete()

        userIdsToDelete.forEach { userId ->
            criterionTableRepo.deleteUserCriteriaByUserId(userId)
        }
        userTableRepo.hardDeleteClearedUsers(userIdsToDelete)
    }
}
