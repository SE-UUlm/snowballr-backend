package se.uulm.snowballr.backend.validation

import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.InvalidPassword
import se.uulm.snowballr.backend.model.InvalidPassword.Reason
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.ValidationIssue
import java.util.UUID

/**
 * Ensures that the given field value is not blank (i.e., it contains at least one non-whitespace character).
 * If the value is blank, a [BlankField] validation issue is raised.
 *
 * @param name The name of the field being validated.
 * @param value The value of the field to check for blankness.
 */
fun Raise<ValidationIssue>.ensureFieldNonBlank(name: String, value: String) =
    ensure(value.isNotBlank()) { BlankField(name) }

/**
 * Ensures that the given field value does not exceed the specified maximum length.
 * If the value exceeds the maximum length, a [TooLongField] validation issue is raised.
 *
 * @param name The name of the field being validated.
 * @param value The value of the field to check for its length.
 * @param maxLength The maximum allowed length for the field value.
 */
fun Raise<ValidationIssue>.ensureFieldLength(name: String, value: String, maxLength: Int) =
    ensure(value.length <= maxLength) { TooLongField(name, maxLength) }

/**
 * Ensures that the provided enum value is not the `UNSPECIFIED` value.
 * If the value is `UNSPECIFIED`, an [EnumUnspecified] validation issue is raised.
 *
 * It is assumed that all gRPC enums have an `UNSPECIFIED` value with ordinal 0.
 *
 * @param name The name of the enum field being validated.
 * @param value The enum value to check for being `UNSPECIFIED`.
 */
fun Raise<ValidationIssue>.ensureEnumNotUnspecified(name: String, value: Enum<*>) =
    ensure(value.ordinal > 0) { EnumUnspecified(name) }

/**
 * Ensures that the provided [id] of field [name] has a valid format.
 *
 * @param name The name of the field being validated.
 * @param id The ID to check for validity.
 */
fun Raise<ValidationIssue>.ensureIdValidity(name: String, id: String) =
    ensure(runCatching { UUID.fromString(id) }.isSuccess) { InvalidId(name, id) }

/**
 * Ensures that the provided email has a valid format.
 * If the email does not match the [EMAIL_REGEX], an [InvalidEmail] validation issue is raised.
 *
 * @param email The email address to validate.
 */
fun Raise<ValidationIssue>.ensureEmailValidity(email: String) =
    ensure(EMAIL_REGEX.matches(email)) { InvalidEmail(email) }

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
 * If any of these conditions are not met, an [InvalidPassword] validation issue is raised with the appropriate reason.
 *
 * @param password The password to validate.
 * @return An [arrow.core.Either] containing either the validation issues or a success indication.
 */
fun ensurePasswordValidity(password: String) = either {
    zipOrAccumulate(
        {
            ensure(password.length >= PASSWORD_MIN_LENGTH) {
                InvalidPassword(password, Reason.TOO_SHORT)
            }
        },
        {
            ensure(password.count { it.isLowerCase() } >= PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS) {
                InvalidPassword(password, Reason.NOT_ENOUGH_LOWERCASE_CHARS)
            }
        },
        {
            ensure(password.count { it.isUpperCase() } >= PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS) {
                InvalidPassword(password, Reason.NOT_ENOUGH_UPPERCASE_CHARS)
            }
        },
        {
            ensure(password.count { it.isDigit() } >= PASSWORD_MIN_NUMBER_DIGITS) {
                InvalidPassword(password, Reason.NOT_ENOUGH_DIGITS)
            }
        },
        {
            ensure(countSpecialChars(password) >= PASSWORD_MIN_NUMBER_SPECIAL_CHARS) {
                InvalidPassword(password, Reason.NOT_ENOUGH_SPECIAL_CHARS)
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

/**
 * Ensures that the provided first name is valid.
 * It checks that the first name is not blank and does not exceed the maximum length defined by [FIRST_NAME_MAX_LENGTH].
 *
 * @param firstName The first name to validate.
 */
fun Raise<ValidationIssue>.ensureFirstNameValidity(firstName: String) {
    ensureFieldNonBlank("first_name", firstName)
    ensureFieldLength("first_name", firstName, FIRST_NAME_MAX_LENGTH)
}

/**
 * Ensures that the provided last name is valid.
 * It checks that the last name is not blank and does not exceed the maximum length defined by [LAST_NAME_MAX_LENGTH].
 *
 * @param lastName The last name to validate.
 */
fun Raise<ValidationIssue>.ensureLastNameValidity(lastName: String) {
    ensureFieldNonBlank("last_name", lastName)
    ensureFieldLength("last_name", lastName, LAST_NAME_MAX_LENGTH)
}
