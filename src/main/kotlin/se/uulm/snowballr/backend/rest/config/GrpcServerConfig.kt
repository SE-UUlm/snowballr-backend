package se.uulm.snowballr.backend.rest.config

import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.grpc.SnowballRServer

/**
 * Starts the gRPC server as part of the Spring application lifecycle during coexistence.
 *
 * Spring is the process entrypoint; gRPC is started once the application context is ready and stops itself via
 * the JVM shutdown hook registered in [SnowballRServer.start].
 */
@Configuration
class GrpcServerConfig {
    @Bean
    fun grpcServer(envReader: EnvReader): SnowballRServer = SnowballRServer(envReader.env.http.port)

    @Bean
    fun grpcServerRunner(grpcServer: SnowballRServer) = CommandLineRunner { grpcServer.start() }
}
