package se.uulm.snowballr.backend.rest.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.internal.missingcontext.MissingUserIdException
import se.uulm.snowballr.backend.model.exception.invalidargument.IncorrectOldPasswordException
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException

class RestExceptionHandlerTest {
    private val handler = RestExceptionHandler()

    @Nested
    inner class HandleSnowballRException {
        @Test
        fun `When a NotFoundException is thrown, then the response has status 404 with the exception message as detail`() {
            val exception = InvitationTokenNotFoundException()

            val problemDetail = handler.handleSnowballRException(exception)

            assertEquals(HttpStatus.NOT_FOUND.value(), problemDetail.status)
            assertEquals(exception.message, problemDetail.detail)
        }

        @Test
        fun `When an InvalidArgumentException is thrown, then the response has status 400 with the exception message as detail`() {
            val exception = IncorrectOldPasswordException()

            val problemDetail = handler.handleSnowballRException(exception)

            assertEquals(HttpStatus.BAD_REQUEST.value(), problemDetail.status)
            assertEquals(exception.message, problemDetail.detail)
        }

        @Test
        fun `When an UnauthenticatedException is thrown, then the response has status 401 with the exception message as detail`() {
            val exception = UnauthenticatedException()

            val problemDetail = handler.handleSnowballRException(exception)

            assertEquals(HttpStatus.UNAUTHORIZED.value(), problemDetail.status)
            assertEquals(exception.message, problemDetail.detail)
        }

        @Test
        fun `When an InternalException is thrown, then the response has status 500 with the exception message as detail`() {
            val exception = MissingUserIdException()

            val problemDetail = handler.handleSnowballRException(exception)

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), problemDetail.status)
            assertEquals(exception.message, problemDetail.detail)
        }
    }
}
