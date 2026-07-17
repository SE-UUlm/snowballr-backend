package se.uulm.snowballr.backend.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

private val logger = KotlinLogging.logger {}

@SpringBootApplication
class SnowballRApplication

fun startRestServer(port: Int = 8090): ConfigurableApplicationContext {
    logger.info { "Starting REST server on port $port" }
    return SpringApplicationBuilder(SnowballRApplication::class.java)
        .properties(
            mapOf(
                "server.port" to port,
                "spring.threads.virtual.enabled" to true,
                "spring.main.banner-mode" to "off",
            ),
        )
        .run()
}
