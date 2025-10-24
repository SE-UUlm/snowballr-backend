package se.uulm.snowballr.backend.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.quartz.Job
import org.quartz.JobExecutionContext

private val logger = KotlinLogging.logger {}

/**
 * Quartz Job responsible for clearing soft-deleted users of sensitive data.
 *
 * This job runs synchronously within the Quartz thread pool and delegates
 * actual cleanup logic to [TokenMaintenanceService]. It wraps the suspended call
 * in a controlled [runBlocking] boundary, using [Dispatchers.IO] for blocking safety.
 */
class ClearSoftDeletedEntitiesJob : Job, KoinComponent {
    private val userMaintenanceService: IUserMaintenanceService by inject()
    private val projectMaintenanceService: IProjectMaintenanceService by inject()

    override fun execute(context: JobExecutionContext?) {
        logger.info { "Starting soft-deleted entity cleanup..." }

        runBlocking(Dispatchers.IO) {
            try {
                userMaintenanceService.clearSoftDeletedUsers()
            } catch (e: CancellationException) {
                logger.warn { "Soft-deleted user cleanup was cancelled: ${e.message}" }
                throw e
            }

            try {
                projectMaintenanceService.clearSoftDeletedProjects()
            } catch (e: CancellationException) {
                logger.warn { "Soft-deleted project cleanup was cancelled: ${e.message}" }
                throw e
            }
        }

        logger.info { "Soft-deleted entity cleanup finished." }
    }
}
