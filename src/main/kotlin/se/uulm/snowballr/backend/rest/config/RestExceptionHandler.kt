package se.uulm.snowballr.backend.rest.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import se.uulm.snowballr.backend.model.exception.SnowballRException

private val logger = KotlinLogging.logger {}

/**
 * Maps [SnowballRException]s thrown by the service layer to [ProblemDetail] responses, mirroring the status
 * mapping in [se.uulm.snowballr.backend.grpc.interceptor.exceptionInterceptor] for the gRPC transport.
 */
@RestControllerAdvice
class RestExceptionHandler {
    @ExceptionHandler(SnowballRException::class)
    fun handleSnowballRException(exception: SnowballRException): ProblemDetail {
        val status = HttpStatus.valueOf(exception.getStatus().code)

        logger.debug {
            "REST call failed due to ${exception::class.simpleName ?: "<unknown class>"} with status: $status. " +
                "Message: ${exception.message ?: "<no message>"}"
        }
        logger.trace { exception.stackTraceToString() }

        return ProblemDetail.forStatusAndDetail(status, exception.message ?: status.reasonPhrase)
    }
}
