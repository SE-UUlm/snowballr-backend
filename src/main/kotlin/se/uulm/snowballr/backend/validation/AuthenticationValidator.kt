package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.InvalidPassword
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Authentication

/**
 * A validator for [Authentication] related requests.
 */
object AuthenticationValidator {
    const val PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS = 2
    const val PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS = 2
    const val PASSWORD_MIN_NUMBER_DIGITS = 2
    const val PASSWORD_MIN_NUMBER_SPECIAL_CHARS = 2
    const val PASSWORD_MIN_LENGTH = 8
    private val SPECIAL_CHAR_REGEX = Regex("[" + Regex.escape("#$%&@^`~.,:;\"'/|_-<>*+!?={[()]}ß") + "]")

    fun validateRegisterRequest(request: Authentication.RegisterRequest): EitherNel<ValidationIssue, Unit> = either {
        val result1 = either {
            zipOrAccumulate(
                { ensureEmailValidity(request.email) },
                { ensureFirstNameValidity(request.firstName) },
                { ensureLastNameValidity(request.lastName) },
            ) { _, _, _ -> }
        }

        val result2 = ensurePasswordValidity(request.password)

        return EitherNel.zipOrAccumulate(result1, result2) { _, _ -> }
    }

    fun validateVerifyEmailRequest(request: Authentication.VerifyEmailRequest): EitherNel<ValidationIssue, Unit> =
        either {
            ensureFieldNonBlank("token", request.token)
        }.toEitherNel()

    fun validateLoginRequest(request: Authentication.LoginRequest): EitherNel<ValidationIssue, Unit> = either {
        ensureEmailValidity(request.email)
        ensureFieldNonBlank("password", request.password)
    }.toEitherNel()

    fun validateChangePasswordRequest(
        request: Authentication.PasswordChangeRequest,
    ): EitherNel<ValidationIssue, Unit> = either {
        val result1 = either {
            ensureFieldNonBlank("old_password", request.oldPassword)
        }.toEitherNel()

        val result2 = ensurePasswordValidity(request.newPassword)
        return EitherNel.zipOrAccumulate(result1, result2) { _, _ -> }
    }

    /**
     * Ensures that the provided password meets the required complexity criteria.
     *
     * It checks the following conditions:
     * - Minimum length of [PASSWORD_MIN_LENGTH]
     * - Minimum number of lowercase letters defined by [PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS]
     * - Minimum number of uppercase letters defined by [PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS]
     * - Minimum number of digits defined by [PASSWORD_MIN_NUMBER_DIGITS]
     * - Minimum number of special characters defined by [PASSWORD_MIN_NUMBER_SPECIAL_CHARS]
     *
     * If any of these conditions are not met, an [se.uulm.snowballr.backend.model.InvalidPassword] validation issue is raised with the appropriate reason.
     *
     * @param password The password to validate.
     * @return An [arrow.core.Either] containing either the validation issues or a success indication.
     */
    private fun ensurePasswordValidity(password: String) = either {
        zipOrAccumulate(
            {
                ensure(password.length >= PASSWORD_MIN_LENGTH) {
                    InvalidPassword(password, InvalidPassword.Reason.TOO_SHORT)
                }
            },
            {
                ensure(password.count { it.isLowerCase() } >= PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS) {
                    InvalidPassword(password, InvalidPassword.Reason.NOT_ENOUGH_LOWERCASE_CHARS)
                }
            },
            {
                ensure(password.count { it.isUpperCase() } >= PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS) {
                    InvalidPassword(password, InvalidPassword.Reason.NOT_ENOUGH_UPPERCASE_CHARS)
                }
            },
            {
                ensure(password.count { it.isDigit() } >= PASSWORD_MIN_NUMBER_DIGITS) {
                    InvalidPassword(password, InvalidPassword.Reason.NOT_ENOUGH_DIGITS)
                }
            },
            {
                ensure(countSpecialChars(password) >= PASSWORD_MIN_NUMBER_SPECIAL_CHARS) {
                    InvalidPassword(password, InvalidPassword.Reason.NOT_ENOUGH_SPECIAL_CHARS)
                }
            },
        ) { _, _, _, _, _ -> }
    }

    /**
     * Counts the number of special characters in the given password.
     * Special characters are defined by the [SPECIAL_CHAR_REGEX].
     *
     * @param password The password to check for special characters.
     * @return The count of special characters in the password.
     */
    private fun countSpecialChars(password: String): Int =
        password.count { SPECIAL_CHAR_REGEX.containsMatchIn(it.toString()) }
}
