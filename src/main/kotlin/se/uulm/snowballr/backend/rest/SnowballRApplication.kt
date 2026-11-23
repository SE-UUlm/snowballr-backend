package se.uulm.snowballr.backend.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.context.ConfigurableApplicationContext

private val logger = KotlinLogging.logger {}

/**
 * Spring property that enables the REST API controllers and the OpenAPI/Swagger docs (see `application.properties`).
 * It is off unless set, so every REST controller must be annotated with
 * `@ConditionalOnBooleanProperty(REST_API_ENABLED_PROPERTY)`. The gRPC server and the actuator are not affected.
 */
const val REST_API_ENABLED_PROPERTY = "snowballr.rest-api.enabled"

@SpringBootApplication
class SnowballRApplication

/**
 * Starts the Spring application, which serves the gRPC API on [grpcPort] and the actuator on [port]. The REST API is
 * served on [port] as well, but only if [restApiEnabled] is set.
 */
fun startRestServer(grpcPort: Int, restApiEnabled: Boolean, port: Int = 8090): ConfigurableApplicationContext {
    logger.info {
        "Starting REST server on port $port and gRPC server on port $grpcPort. " +
            "REST API is ${if (restApiEnabled) "enabled" else "disabled"}"
    }
    return SpringApplicationBuilder(SnowballRApplication::class.java)
        .properties(
            mapOf(
                "server.port" to port,
                "spring.grpc.server.port" to grpcPort,
                "spring.threads.virtual.enabled" to true,
                "spring.main.banner-mode" to "off",
                REST_API_ENABLED_PROPERTY to restApiEnabled,
            ),
        )
        .run()
}
