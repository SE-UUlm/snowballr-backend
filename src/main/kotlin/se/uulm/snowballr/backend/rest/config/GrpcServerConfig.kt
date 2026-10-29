package se.uulm.snowballr.backend.rest.config

import io.grpc.BindableService
import io.grpc.ServerInterceptor
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.grpc.server.GlobalServerInterceptor
import se.uulm.snowballr.backend.grpc.SnowballRService
import se.uulm.snowballr.backend.grpc.interceptor.authenticationInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.exceptionInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.loggingInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.requestContextCoroutineInterceptor
import se.uulm.snowballr.backend.grpc.interceptor.validationInterceptor

/**
 * Registers the gRPC service and interceptors with the Spring gRPC server.
 *
 * Spring gRPC starts and stops the server as part of the application lifecycle and auto-configures the health and
 * reflection services. Every [BindableService] bean is served, and every [GlobalServerInterceptor] bean is applied to
 * all services, ordered by [Order]: the lowest order runs first.
 *
 * Spring's own global interceptors (observation, exception handling) use orders below ours and therefore wrap this
 * chain.
 */
@Configuration
class GrpcServerConfig {
    @Bean
    fun snowballRService(): BindableService = SnowballRService()

    @Bean
    @GlobalServerInterceptor
    @Order(LOGGING_ORDER)
    fun grpcLoggingInterceptor(): ServerInterceptor = loggingInterceptor

    @Bean
    @GlobalServerInterceptor
    @Order(AUTHENTICATION_ORDER)
    fun grpcAuthenticationInterceptor(): ServerInterceptor = authenticationInterceptor

    // Must run after authenticationInterceptor: it reads REQUEST_CONTEXT_KEY written by auth and installs the
    // RequestContext into the service coroutine context.
    @Bean
    @GlobalServerInterceptor
    @Order(REQUEST_CONTEXT_ORDER)
    fun grpcRequestContextInterceptor(): ServerInterceptor = requestContextCoroutineInterceptor

    @Bean
    @GlobalServerInterceptor
    @Order(VALIDATION_ORDER)
    fun grpcValidationInterceptor(): ServerInterceptor = validationInterceptor

    @Bean
    @GlobalServerInterceptor
    @Order(EXCEPTION_ORDER)
    fun grpcExceptionInterceptor(): ServerInterceptor = exceptionInterceptor

    private companion object {
        const val LOGGING_ORDER = 100
        const val AUTHENTICATION_ORDER = 200
        const val REQUEST_CONTEXT_ORDER = 300
        const val VALIDATION_ORDER = 400
        const val EXCEPTION_ORDER = 500
    }
}
