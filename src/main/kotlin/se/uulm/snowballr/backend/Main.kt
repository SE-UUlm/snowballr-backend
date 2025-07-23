package se.uulm.snowballr.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.koin.core.context.startKoin
import org.slf4j.LoggerFactory
import se.uulm.snowballr.backend.env.DEFAULT_LOG_LEVEL
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.EnvService
import se.uulm.snowballr.backend.grpc.SnowballRServer

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

    // Create and run the server
    val server = SnowballRServer(env.http.port)
    server.start()
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
fun configureRootLogger(logLevel: String) {
    val context = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    rootLogger.level = Level.toLevel(logLevel, Level.toLevel(DEFAULT_LOG_LEVEL))
    logger.info { "Set log level to $logLevel" }
}
