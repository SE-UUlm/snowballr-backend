package se.uulm.snowballr.backend.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quartz.Job
import org.quartz.JobExecutionContext

private val logger = KotlinLogging.logger { }

/**
 * Quartz Job responsible for hard-deleting all soft-deleted users.
 *
 * This job runs synchronously within the Quartz thread pool and delegates
 * actual cleanup logic to [TokenMaintenanceService]. It wraps the suspended call
 * in a controlled [runBlocking] boundary, using [Dispatchers.IO] for blocking safety.
 */
class HardDeleteSoftDeletedEntitiesJob : Job, KoinComponent {
    private val userMaintenanceService: IUserMaintenanceService by inject()
    private val projectMaintenanceService: IProjectMaintenanceService by inject()

    override fun execute(context: JobExecutionContext?) {
        logger.info { "Starting hard-delete of soft-deleted entities..." }

        @Suppress("InjectDispatcher")
        runBlocking(Dispatchers.IO) {
            try {
                userMaintenanceService.hardDeleteClearedUsers()
            } catch (e: CancellationException) {
                logger.warn { "Hard-deleting, already soft-deleted users, was cancelled: ${e.message}" }
                throw e
            }

            try {
                projectMaintenanceService.hardDeleteClearedProjects()
            } catch (e: CancellationException) {
                logger.warn { "Hard-deleting, already soft-deleted projects, was cancelled: ${e.message}" }
                throw e
            }
        }

        logger.info { "Hard-delete of soft-deleted entities finished." }
    }
}
