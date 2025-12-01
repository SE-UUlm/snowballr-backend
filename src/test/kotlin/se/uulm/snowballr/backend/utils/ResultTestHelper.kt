package se.uulm.snowballr.backend.utils

import org.junit.jupiter.api.Assertions.assertTrue
import se.uulm.snowballr.backend.model.exception.SnowballRException
import kotlin.test.assertIs

/**
 * Assert that the passed [Result] is not a [Result.Failure] and contains the data of type [T]. The data is returned.
 */
inline fun <reified T> assertResultSuccess(result: Result<T>): T {
    assertTrue(result.isSuccess)
    val data = result.getOrNull()
    assertIs<T>(data)
    return data
}

/**
 * Assert that the passed [Result] is a [Result.Failure] and contains the exception of type [T].
 */
inline fun <reified T : SnowballRException> assertResultFailure(result: Result<*>) {
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull()
    assertIs<T>(exception)
}
