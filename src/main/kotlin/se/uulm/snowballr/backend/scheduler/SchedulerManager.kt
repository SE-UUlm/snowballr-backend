package se.uulm.snowballr.backend.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.Scheduler
import org.quartz.impl.StdSchedulerFactory

private val logger = KotlinLogging.logger {}

/**
 * Central manager for registering and controlling scheduled maintenance jobs.
 *
 * This class uses the Quartz Scheduler to periodically execute background maintenance tasks such as cleaning expired
 * tokens and updating time-limited entities.
 */
class SchedulerManager {
    private val scheduler: Scheduler = StdSchedulerFactory.getDefaultScheduler()

    fun start() {
        logger.info { "Start Quartz scheduler..." }
        scheduler.start()
    }

    fun stop() {
        logger.info { "Shutting down Quartz scheduler..." }
        scheduler.shutdown(true)
    }
}
