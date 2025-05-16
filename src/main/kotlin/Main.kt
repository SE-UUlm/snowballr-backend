package se.uulm.snowballr.backend

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import io.github.oshai.kotlinlogging.KotlinLogging
import org.slf4j.LoggerFactory
import se.uulm.snowballr.backend.env.DEFAULT_LOG_LEVEL
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.grpc.SnowballRServer

private val logger = KotlinLogging.logger {}

fun main() {
    val env = Env()
    configureRootLogger(env.miscellaneous.logLevel)
    val server = SnowballRServer(env.http.port)
    server.start()
    server.blockUntilShutdown()
}

fun configureRootLogger(logLevel: String) {
    val context = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = context.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    rootLogger.level = Level.toLevel(logLevel, Level.toLevel(DEFAULT_LOG_LEVEL))
    logger.info { "Set log level to $logLevel" }
}
