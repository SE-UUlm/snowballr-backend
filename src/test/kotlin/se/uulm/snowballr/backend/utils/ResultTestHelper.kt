package se.uulm.snowballr.backend.utils

import org.assertj.core.api.Assertions.assertThat
import se.uulm.snowballr.backend.model.SnowballRException
import kotlin.test.assertIs

/**
 * Assert that the passed [Result] is not a [Result.Failure] and contains the data of type [T].
 */
inline fun <reified T> assertResultSuccess(result: Result<T>): T {
    assertThat(result.isSuccess).isTrue()
    val data = result.getOrNull()
    assertIs<T>(data)
    return data
}

/**
 * Assert that the passed [Result] is a [Result.Failure] and contains the exception of type [T].
 */
inline fun <reified T : SnowballRException> assertResultFailure(result: Result<*>) {
    assertThat(result.isFailure).isTrue()
    val exception = result.exceptionOrNull()
    assertIs<T>(exception)
}
