package se.uulm.snowballr.backend

import arrow.core.Either
import arrow.core.EitherNel
import `in`.rcard.assertj.arrowcore.EitherAssert
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import se.uulm.snowballr.backend.model.ValidationIssue

/**
 * Executes a suspending function within a coroutine that runs on the main dispatcher.
 *
 * @param testFunction The suspending function to be executed during the test.
 */
fun testCoroutine(testFunction: suspend () -> Unit) {
    runBlocking {
        launch(Dispatchers.Main) {
            testFunction()
        }
    }
}

/**
 * Asserts that the passed [result] is [Either.Left] with the issue of type [issueType].
 *
 * **Note:** This assumes that the validation result only contains one validation issue.
 */
fun <T> assertInvalidResult(result: EitherNel<ValidationIssue, Unit>, issueType: Class<T>) {
    EitherAssert.assertThat(result).isLeft()
    val value = (result as Either.Left).value
    assertThat(value.size).isEqualTo(1)
    val issue = value.first()
    assertThat(issue).isInstanceOf(issueType)
}
