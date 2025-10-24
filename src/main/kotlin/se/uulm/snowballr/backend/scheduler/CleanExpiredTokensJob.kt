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
 * Quartz Job responsible for cleaning up expired tokens.
 *
 * This job runs synchronously within the Quartz thread pool and delegates
 * actual cleanup logic to [TokenMaintenanceService]. It wraps the suspended call
 * in a controlled [runBlocking] boundary, using [Dispatchers.IO] for blocking safety.
 */
class CleanExpiredTokensJob : Job, KoinComponent {
    private val tokenMaintenanceService: ITokenMaintenanceService by inject()

    override fun execute(context: JobExecutionContext?) {
        logger.info { "Starting expired token cleanup..." }

        runBlocking(Dispatchers.IO) {
            try {
                tokenMaintenanceService.deleteExpiredTokens()
            } catch (e: CancellationException) {
                logger.warn { "Expired token cleanup was cancelled: ${e.message}" }
                throw e
            }
        }

        logger.info { "Expired token cleanup finished." }
    }
}
