package se.uulm.snowballr.backend.validation

import arrow.core.Either
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidPassword
import se.uulm.snowballr.backend.model.TooLongField
import snowballr.Authentication

class AuthenticationValidatorTest {
    @Nested
    inner class RegisterRequest {
        private val validRegisterRequestBuilder: Authentication.RegisterRequest.Builder =
            Authentication.RegisterRequest
                .newBuilder()
                .setEmail("alice.smith@example.com")
                .setFirstName("Alice")
                .setLastName("Smith")
                .setPassword("AAbb__00")

        @Test
        fun `When a valid request is validated, then no issue is returned`() {
            val request = validRegisterRequestBuilder.build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid email is validated, then the 'InvalidEmail' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setEmail("invalid-email")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }

        @Test
        fun `When a blank first name is validated, then the 'BlankField' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setFirstName("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a first name with more than 100 characters is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setFirstName("a".repeat(101))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When a blank last name is validated, then the 'BlankField' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setLastName("")
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a last name with more than 100 characters is validated, then the 'TooLongField' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setLastName("b".repeat(101))
                    .build()
            val result = validateRequest(request)

            assertInvalidResult<TooLongField>(result)
        }

        @Test
        fun `When a password with less than 8 characters is validated, then the 'InvalidPassword' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("short")
                    .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.TOO_SHORT, issue.reason)
        }

        @Test
        fun `When a password with less than 2 lowercase letters is validated, then the 'InvalidPassword' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("AABB__00")
                    .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.NOT_ENOUGH_LOWERCASE_CHARS, issue.reason)
        }

        @Test
        fun `When a password with less than 2 uppercase letters is validated, then the 'InvalidPassword' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("aabb__00")
                    .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.NOT_ENOUGH_UPPERCASE_CHARS, issue.reason)
        }

        @Test
        fun `When a password with less than 2 digits is validated, then the 'InvalidPassword' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("AAbb__aa")
                    .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.NOT_ENOUGH_DIGITS, issue.reason)
        }

        @Test
        fun `When a password with less than 2 special characters is validated, then the 'InvalidPassword' issue is returned`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("AAbb0000")
                    .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.NOT_ENOUGH_SPECIAL_CHARS, issue.reason)
        }

        @Test
        fun `When a password is invalid, then all issues are accumulated`() {
            val request =
                validRegisterRequestBuilder
                    .setPassword("")
                    .build()

            val result = validateRequest(request)
            EitherAssert.assertThat(result).isLeft()
            val issues = (result as Either.Left).value

            assertThat(issues).hasSize(5)
            assertThat(issues).contains(InvalidPassword("", InvalidPassword.Reason.TOO_SHORT))
            assertThat(issues).contains(InvalidPassword("", InvalidPassword.Reason.NOT_ENOUGH_LOWERCASE_CHARS))
            assertThat(issues).contains(InvalidPassword("", InvalidPassword.Reason.NOT_ENOUGH_UPPERCASE_CHARS))
            assertThat(issues).contains(InvalidPassword("", InvalidPassword.Reason.NOT_ENOUGH_DIGITS))
            assertThat(issues).contains(InvalidPassword("", InvalidPassword.Reason.NOT_ENOUGH_SPECIAL_CHARS))
        }
    }

    @Nested
    inner class VerifyEmailRequest {
        @Test
        fun `When a valid verify email request is validated, then no issue is returned`() {
            val request = Authentication.VerifyEmailRequest.newBuilder()
                .setToken("a-valid-non-blank-token-12345")
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When a blank token is validated, then the 'BlankField' issue is returned`() {
            val request = Authentication.VerifyEmailRequest.newBuilder()
                .setToken("")
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When a token consisting only of whitespace is validated, then the 'BlankField' issue is returned`() {
            val request = Authentication.VerifyEmailRequest.newBuilder()
                .setToken("   ")
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }
    }

    @Nested
    inner class LoginRequest {
        @Test
        fun `When a valid login request is validated, then no issue is returned`() {
            val request = Authentication.LoginRequest.newBuilder()
                .setEmail("test.user@example.com")
                .setPassword("AAbb__00")
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When an invalid email is validated, then the 'InvalidEmail' issue is returned`() {
            val request = Authentication.LoginRequest.newBuilder()
                .setEmail("invalid-email")
                .build()
            val result = validateRequest(request)

            assertInvalidResult<InvalidEmail>(result)
        }

        @Test
        fun `When a blank password is validated, then the 'BlankField' issue is returned`() {
            val request = Authentication.LoginRequest.newBuilder()
                .setEmail("test.user@example.com")
                .setPassword("")
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }
    }

    @Nested
    inner class PasswordChangeRequest {
        @Test
        fun `When a valid password change request is validated, then no issue is returned`() {
            val request = Authentication.PasswordChangeRequest.newBuilder()
                .setOldPassword("AAbb__00")
                .setNewPassword("CCdd__11")
                .build()
            val result = validateRequest(request)

            EitherAssert.assertThat(result).isRight()
        }

        @Test
        fun `When the old password is blank, then the 'BlankField' issue is returned`() {
            val request = Authentication.PasswordChangeRequest.newBuilder()
                .setOldPassword("")
                .setNewPassword("CCdd__11")
                .build()
            val result = validateRequest(request)

            assertInvalidResult<BlankField>(result)
        }

        @Test
        fun `When the new password is invalid, then the 'InvalidPassword' issue is returned`() {
            val request = Authentication.PasswordChangeRequest.newBuilder()
                .setOldPassword("AAbb__00")
                .setNewPassword("short")
                .build()
            val result = validateRequest(request)

            val issue = assertInvalidResult<InvalidPassword>(result)
            assertEquals(InvalidPassword.Reason.TOO_SHORT, issue.reason)
        }
    }
}
