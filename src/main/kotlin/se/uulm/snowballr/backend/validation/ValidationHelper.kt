@file:Suppress("TooManyFunctions")

package se.uulm.snowballr.backend.validation

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.FieldMask
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidEmail
import se.uulm.snowballr.backend.model.InvalidFieldMask
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.TooLongField
import se.uulm.snowballr.backend.model.ValidationIssue
import java.util.UUID

/**
 * Email regex.
 *
 * See: https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression/201378#201378
 */
@Suppress("MaxLineLength", "StringShouldBeRawString")
private val EMAIL_REGEX =
    Regex(
        "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])",
    )
private const val FIRST_NAME_MAX_LENGTH = 100
private const val LAST_NAME_MAX_LENGTH = 100

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
 * Ensures that the given text field value is valid.
 *
 * Validity is defined as follows:
 * - The field value must not be blank.
 * - The field value must not exceed the specified maximum length.
 *
 * @param name The name of the field being validated.
 * @param value The value of the field to check for validity.
 * @param maxLength The maximum allowed length for the field value.
 */
fun Raise<ValidationIssue>.ensureTextFieldValidity(name: String, value: String, maxLength: Int) {
    ensureFieldNonBlank(name, value)
    ensureFieldLength(name, value, maxLength)
}

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
 * Ensures that the provided stage is positive or null.
 * If the stage is negative, an [OutOfRangeValue] validation issue is raised.
 *
 * @param stage The stage value to validate.
 */
fun Raise<ValidationIssue>.ensureStageValidity(stage: Long) =
    ensure(stage >= 0) { OutOfRangeValue("stage", stage, 0, Long.MAX_VALUE) }

/**
 * Ensures that the provided first name is valid.
 * It checks that the first name is not blank and does not exceed the maximum length defined by [FIRST_NAME_MAX_LENGTH].
 *
 * @param firstName The first name to validate.
 */
fun Raise<ValidationIssue>.ensureFirstNameValidity(firstName: String) =
    ensureTextFieldValidity("first_name", firstName, FIRST_NAME_MAX_LENGTH)

/**
 * Ensures that the provided last name is valid.
 * It checks that the last name is not blank and does not exceed the maximum length defined by [LAST_NAME_MAX_LENGTH].
 *
 * @param lastName The last name to validate.
 */
fun Raise<ValidationIssue>.ensureLastNameValidity(lastName: String) =
    ensureTextFieldValidity("last_name", lastName, LAST_NAME_MAX_LENGTH)

/**
 * Ensures that the provided field mask is non-blank and contains only fields that are valid and allowed for the given
 * object type.
 * If the field mask is not valid, a [InvalidFieldMask] validation issue is raised.
 *
 * @param fieldMask The [FieldMask] to validate.
 * @param descriptor The object descriptor to validate against.
 * @param unallowedFields A list of paths that must not appear in the field mask.
 * @param allowEmpty Whether to allow an empty field mask. Defaults to false.
 */
fun Raise<ValidationIssue>.ensureFieldMaskIsValid(
    fieldMask: FieldMask,
    descriptor: Descriptor,
    unallowedFields: List<String> = emptyList(),
    allowEmpty: Boolean = false,
) {
    ensure(allowEmpty || fieldMask.pathsList.isNotEmpty()) { InvalidFieldMask(null) }
    ensure(FieldMaskUtil.isValid(descriptor, fieldMask)) {
        InvalidFieldMask(fieldMask.toString())
    }
    ensure(fieldMask.pathsList.toSet().none { unallowedFields.contains(it) }) {
        InvalidFieldMask(fieldMask.toString())
    }
}

/**
 * Ensures that the given number field is within the specified range.
 *
 * If the value is outside the range, an [OutOfRangeValue] validation issue is raised.
 *
 * @param T The type of the number field. Must be a subtype of [Comparable].
 * @param name The name of the field being validated.
 * @param value The value of the field to check for validity.
 * @param min The minimum allowed value for the field.
 * @param max The maximum allowed value for the field.
 */
fun <T : Comparable<T>> Raise<ValidationIssue>.ensureNumberFieldInRange(name: String, value: T, min: T, max: T) {
    ensure(value in min..max) {
        OutOfRangeValue(name, value, min, max)
    }
}
