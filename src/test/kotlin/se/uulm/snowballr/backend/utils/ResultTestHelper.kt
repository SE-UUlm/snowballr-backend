package se.uulm.snowballr.backend.utils

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.assertInstanceOf
import se.uulm.snowballr.backend.model.exception.SnowballRException

/**
 * Assert that the passed [Result] is not a [Result.Failure] and contains the data of type [T]. The data is returned.
 */
inline fun <reified T : Any> assertResultSuccess(result: Result<T>): T {
    assertTrue(result.isSuccess)
    val data = result.getOrThrow()
    assertInstanceOf<T>(data)
    return data
}

/**
 * Assert that the passed [Result] is a [Result.Failure] and contains the exception of type [T].
 */
inline fun <reified T : SnowballRException> assertResultFailure(result: Result<*>) {
    assertTrue(result.isFailure)
    val exception = result.exceptionOrNull()
    assertInstanceOf<T>(exception)
}
