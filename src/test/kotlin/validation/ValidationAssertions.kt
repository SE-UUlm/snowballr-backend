package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import `in`.rcard.assertj.arrowcore.EitherAssert
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertInstanceOf
import se.uulm.snowballr.backend.model.ValidationIssue

/**
 * Asserts that the passed [result] is [Either.Left] containing at least one [ValidationIssue]
 * of type [T].
 *
 * This function:
 * - Fails if the result is not a Left.
 * - Fails if no issue of type [T] is found in the list of validation issues.
 * - Returns the first issue of type [T] found.
 *
 * **Note:** This does *not* enforce that the validation result contains only a single issue;
 * it only verifies that an issue of the expected type [T] exists.
 *
 * @return The validation issue of type [T].
 */

inline fun <reified T : ValidationIssue> assertInvalidResult(result: EitherNel<ValidationIssue, Unit>): T {
    EitherAssert.assertThat(result).isLeft()
    val issues = (result as Either.Left).value

    val matchingIssue = issues.find { it is T }
    assertThat(
        matchingIssue,
    ).withFailMessage("Expected issue of type ${T::class} but found: ${issues.map { it::class }}")
        .isNotNull()
    assertInstanceOf<T>(matchingIssue)
    return matchingIssue
}
