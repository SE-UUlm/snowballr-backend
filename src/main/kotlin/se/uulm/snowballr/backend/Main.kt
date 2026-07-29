package se.uulm.snowballr.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.context.startKoin
import org.koin.java.KoinJavaComponent.getKoin
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.DEFAULT_LOG_LEVEL
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.EnvService
import se.uulm.snowballr.backend.fetcher.FetcherOrchestrator
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.grpc.SnowballRServer
import se.uulm.snowballr.backend.rest.startRestServer
import se.uulm.snowballr.backend.scheduler.SchedulerManager

private val logger = KotlinLogging.logger {}

fun main() {
    val envReader = EnvReader(EnvService())
    val env = envReader.env

    // Configure Logger
    configureRootLogger(env.miscellaneous.logLevel)

    // Start Dependency Injection
    startKoin {
        modules(snowballRModule)
    }

    initializeFetcherOrchestrator()
    addDbShutdownHook()
    initializeSchedulerManager()

    // Create and run the server
    val server = SnowballRServer(env.http.port)
    server.start()

    startRestServer()

    // Wait for gRPC server shutdown
    server.blockUntilShutdown()
}

/**
 * Configures the root logger's log level. This sets the level for all other logger instances unless they don't set
 * their own log level.
 *
 * @param logLevel The desired log level for the root logger. Acceptable values are one of
 * [io.github.oshai.kotlinlogging.Level] as string such as `DEBUG` or `INFO`.
 * If an invalid log level is provided, the default log level [DEFAULT_LOG_LEVEL] will be used.
 */
private fun configureRootLogger(logLevel: String) {
    val context = (LoggerFactory.getILoggerFactory() ?: error("unable to get logger context")) as LoggerContext
    val rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME)

    rootLogger.level = Level.toLevel(logLevel, Level.toLevel(DEFAULT_LOG_LEVEL))
    logger.info { "Set log level to $logLevel" }
}

/**
 * Starts the [FetcherOrchestrator] and adds a shutdown hook to stop it on shutdown.
 */
private fun initializeFetcherOrchestrator() {
    val orchestrator = getKoin().get<IFetcherOrchestrator>()
    orchestrator.start()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            orchestrator.stop()
        },
    )
}

private fun addDbShutdownHook() {
    val db = getKoin().get<IDatabase>()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            db.close()
            logger.info { "Closed database connection" }
        },
    )
}

private fun initializeSchedulerManager() {
    val schedulerManager = SchedulerManager()
    schedulerManager.start()
    Runtime.getRuntime().addShutdownHook(
        Thread {
            schedulerManager.stop()
        },
    )
}
