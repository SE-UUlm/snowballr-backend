package se.uulm.snowballr.backend.model

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
