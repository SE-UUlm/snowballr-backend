package se.uulm.snowballr.backend.validation

import `in`.rcard.assertj.arrowcore.EitherAssert
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.assertInvalidResult
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidEmail
import snowballr.Base

class BaseValidatorTest {
    @Nested
    inner class IdRequest {
        @Test
        fun `When a valid ID request is validated, then no issue is returned`() {
            val request =
                Base.Id
                    .newBuilder()
                    .setId("1")
                    .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank ID request is validated, then the 'BlankField' issue is returned`() {
            val request =
                Base.Id
                    .newBuilder()
                    .setId("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult(result, BlankField::class.java)
        }
    }

    @Nested
    inner class EmailRequest {
        @Test
        fun `When a valid email request is validated, then no issue is returned`() {
            val request =
                Base.Email
                    .newBuilder()
                    .setEmail("test-user@example.com")
                    .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid email request is validated, then the 'InvalidEmail' issue is returned`() {
            val request =
                Base.Email
                    .newBuilder()
                    .setEmail("invalid-email")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult(result, InvalidEmail::class.java)
        }
    }

    @Nested
    inner class NothingRequest {
        @Test
        fun `When a valid nothing request is validated, then no issue is returned`() {
            val request = Base.Nothing.getDefaultInstance()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }
    }
}
