package se.uulm.snowballr.backend.model

import se.uulm.snowballr.backend.validation.AuthenticationValidator.PASSWORD_MIN_LENGTH
import se.uulm.snowballr.backend.validation.AuthenticationValidator.PASSWORD_MIN_NUMBER_DIGITS
import se.uulm.snowballr.backend.validation.AuthenticationValidator.PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS
import se.uulm.snowballr.backend.validation.AuthenticationValidator.PASSWORD_MIN_NUMBER_SPECIAL_CHARS
import se.uulm.snowballr.backend.validation.AuthenticationValidator.PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS

/**
 * Represents a validation issue that can occur during the validation process.
 *
 * Instances of [ValidationIssue] are typically used in validation workflows
 * to encapsulate specific validation failures, such as blank fields or fields
 * exceeding a maximum length. They can be leveraged in functional or reactive
 * validation models using constructs like [arrow.core.Either] or
 * [arrow.core.EitherNel] for error accumulation and handling.
 */
sealed interface ValidationIssue

/**
 * Represents a validation issue indicating an unrecognized or unsupported request type.
 *
 * This issue is returned when a request is encountered that cannot be handled by the validation logic,
 * typically when the request type does not match any of the expected types in the validation workflow.
 *
 * It serves as a fallback mechanism to ensure that all unsupported or unrecognized inputs can be properly flagged
 * and handled, preventing silent failures or undefined behavior.
 */
data object UnknownRequest : ValidationIssue

/**
 * Represents a validation issue for a field that is empty or contains only whitespace characters, i.e., is blank.
 *
 * @property name The name of the field that is blank.
 */
data class BlankField(
    val name: String,
) : ValidationIssue {
    override fun toString() =
        "The field '$name' must not be empty and must contain at least one non whitespace character."
}

/**
 * Represents a validation issue where a field exceeds its maximum allowed length.
 *
 * @property name The name of the field that exceeded the maximum length.
 * @property maxLength The maximum allowed length for the field.
 */
data class TooLongField(
    val name: String,
    val maxLength: Int,
) : ValidationIssue {
    override fun toString(): String = "The '$name' must not be longer than $maxLength characters."
}

/**
 * Represents a validation issue where an enumeration field has the `UNSPECIFIED` value.
 *
 * @property name The name of the enum field that has the `UNSPECIFIED` value.
 */
data class EnumUnspecified(
    val name: String,
) : ValidationIssue {
    override fun toString(): String = "The enum field '$name' must not have the `UNSPECIFIED` value."
}

/**
 * Represents a validation issue where an email has an invalid format.
 *
 * @property email The email that has an invalid format.
 */
data class InvalidEmail(
    val email: String,
) : ValidationIssue {
    override fun toString(): String = "The email address '$email' is invalid."
}

/**
 * Represents a validation issue where an ID has an invalid format.
 *
 * @property name The name of the ID field that has an invalid format.
 * @property id The invalid ID.
 */
data class InvalidId(
    val name: String,
    val id: String,
) : ValidationIssue {
    override fun toString(): String = "The ID '$id' is invalid for the field '$name'."
}

data class OutOfRangeValue<T : Comparable<T>>(
    val name: String,
    val value: T,
    val from: T,
    val to: T,
) : ValidationIssue {
    override fun toString(): String =
        "The value '$value' is not in the allowed range [$from-$to] for the field '$name'."
}

/**
 * Represents a validation issue where a password does not meet the required criteria.
 *
 * @property password The password that is invalid.
 * @property reason The specific reason why the password is invalid.
 */
data class InvalidPassword(
    val password: String,
    val reason: Reason,
) : ValidationIssue {
    override fun toString(): String = "The password is invalid: ${reason.message}"

    /**
     * Lists possible reasons for password invalidity.
     */
    enum class Reason(val message: String) {
        TOO_SHORT("Password contains less than $PASSWORD_MIN_LENGTH characters"),
        NOT_ENOUGH_LOWERCASE_CHARS(
            "Password contains less than $PASSWORD_MIN_NUMBER_LOWERCASE_LETTERS lowercase letters",
        ),
        NOT_ENOUGH_UPPERCASE_CHARS(
            "Password contains less than $PASSWORD_MIN_NUMBER_UPPERCASE_LETTERS uppercase letters",
        ),
        NOT_ENOUGH_DIGITS("Password contains less than $PASSWORD_MIN_NUMBER_DIGITS digits"),
        NOT_ENOUGH_SPECIAL_CHARS("Password contains less than $PASSWORD_MIN_NUMBER_SPECIAL_CHARS special characters"),
    }
}

/**
 * Represents a validation issue where a provided field mask is invalid.
 *
 * This issue occurs when the field mask is not valid, i.e., it is either blank, contains at least one invalid field, or
 * contains fields that are not allowed.
 * If the field mask is blank, the entire object would be overwritten. To prevent unintended overwrites, all intended
 * fields should be explicitly listed in the field mask rather than leaving the mask empty.
 * A field is considered invalid if it does not exist in the corresponding generated gRPC class.
 */
data class InvalidFieldMask(val message: String) : ValidationIssue {
    companion object {
        fun createForBlankFieldMask(): InvalidFieldMask = InvalidFieldMask("Field mask must be non-blank.")

        fun createForContainsInvalidFields(fields: List<String>): InvalidFieldMask {
            val fieldsString = fields.joinToString(", ") { field -> "'$field'" }
            return InvalidFieldMask("One or more of the following fields are invalid: $fieldsString")
        }

        fun createForContainsUnallowedFields(fields: List<String>): InvalidFieldMask {
            val fieldsString = fields.joinToString(", ") { field -> "'$field'" }
            return InvalidFieldMask("One or more of the following fields are not allowed: $fieldsString")
        }
    }

    override fun toString(): String = message
}

/**
 * Represents a validation issue where a list field exceeds its maximum allowed length.
 *
 * @property name The name of the list field that exceeded the maximum length.
 * @property maxLength The maximum allowed length for the list field.
 */
data class TooLongList(val name: String, val maxLength: Int) : ValidationIssue {
    override fun toString(): String = "The list '$name' must not contain more than $maxLength elements."
}

/**
 * Represents a composite validation issue that aggregates multiple individual [ValidationIssue]s.
 *
 * This is useful for scenarios where multiple validation errors need to be reported together,
 * providing a comprehensive overview of all issues encountered during the validation process.
 *
 * @property baseMessage A general message summarizing the composite issue.
 * @property issues A list of individual [ValidationIssue]s that make up the composite issue.
 */
data class CompositeIssue(val baseMessage: String, val issues: List<ValidationIssue>) : ValidationIssue {
    override fun toString(): String = "$baseMessage: ${issues.joinToString("; ")}"
}

/**
 * Represents a validation issue where an unsupported export format is requested.
 *
 * @property format The unsupported export format.
 */
data class UnsupportedExportFormat(val format: String) : ValidationIssue {
    override fun toString(): String = "The export format '$format' is not supported."
}
