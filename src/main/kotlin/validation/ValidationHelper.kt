package se.uulm.snowballr.backend.validation

import arrow.core.raise.Raise
import arrow.core.raise.ensure
import se.uulm.snowballr.backend.model.BlankField
import se.uulm.snowballr.backend.model.EnumUnspecified
import se.uulm.snowballr.backend.model.InvalidId
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
fun Raise<ValidationIssue>.ensureFieldNonBlank(name: String, value: String,) =
    ensure(value.isNotBlank()) { BlankField(name) }

/**
 * Ensures that the given field value does not exceed the specified maximum length.
 * If the value exceeds the maximum length, a [TooLongField] validation issue is raised.
 *
 * @param name The name of the field being validated.
 * @param value The value of the field to check for its length.
 * @param maxLength The maximum allowed length for the field value.
 */
fun Raise<ValidationIssue>.ensureFieldLength(name: String, value: String, maxLength: Int,) =
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
fun Raise<ValidationIssue>.ensureEnumNotUnspecified(name: String, value: Enum<*>,) =
    ensure(value.ordinal > 0) { EnumUnspecified(name) }

/**
 * Ensures that the provided [id] of field [name] has a valid format.
 *
 * @param name The name of the field being validated.
 * @param id The ID to check for validity.
 */
fun Raise<ValidationIssue>.ensureIdValidity(name: String, id: String,) =
    ensure(runCatching { UUID.fromString(id) }.isSuccess) { InvalidId(name, id) }
