package se.uulm.snowballr.backend.scheduler

import io.github.oshai.kotlinlogging.KotlinLogging
import org.quartz.CronScheduleBuilder
import org.quartz.JobBuilder
import org.quartz.Scheduler
import org.quartz.TriggerBuilder
import org.quartz.impl.StdSchedulerFactory

private val logger = KotlinLogging.logger {}

private const val MAINTENANCE = "maintenance"

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

        // Register all recurring jobs
        scheduleCleanExpiredTokens()
        scheduleClearSoftDeletedEntities()
    }

    fun stop() {
        logger.info { "Shutting down Quartz scheduler..." }
        scheduler.shutdown(true)
    }

    /**
     * Schedules a recurring job that cleans up expired verification and invitation tokens.
     *
     * The job runs every day at midnight.
     */
    private fun scheduleCleanExpiredTokens() {
        val job = JobBuilder.newJob(CleanExpiredTokensJob::class.java)
            .withIdentity("cleanExpiredTokens", MAINTENANCE)
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("cleanExpiredTokensTrigger", MAINTENANCE)
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
            .build()

        scheduler.scheduleJob(job, trigger)
    }

    /**
     * Schedules a recurring job that clears sensitive information from soft-deleted entities.
     *
     * This job runs every day at midnight.
     */
    private fun scheduleClearSoftDeletedEntities() {
        val job = JobBuilder.newJob(ClearSoftDeletedEntitiesJob::class.java)
            .withIdentity("clearSoftDeletedEntities", MAINTENANCE)
            .build()

        val trigger = TriggerBuilder.newTrigger()
            .withIdentity("clearSoftDeletedEntitiesTrigger", MAINTENANCE)
            .withSchedule(CronScheduleBuilder.cronSchedule("0 0 0 * * ?"))
            .build()

        scheduler.scheduleJob(job, trigger)
    }
}
