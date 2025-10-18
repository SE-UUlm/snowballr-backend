package se.uulm.snowballr.backend

import org.assertj.core.api.AbstractOffsetDateTimeAssert
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Same as [AbstractOffsetDateTimeAssert.isBetween] but with a delta in microseconds.
 *
 * @see AbstractOffsetDateTimeAssert.isBetween
 *
 * @param startInclusive Lower bound of the time interval (inclusive).
 * @param endInclusive Upper bound of the time interval (inclusive).
 * @param deltaInUs Delta in microseconds that the time must be within the interval. Defaults to 1000 (1 ms).
 */
fun AbstractOffsetDateTimeAssert<*>.isBetweenWithDelta(
    startInclusive: OffsetDateTime,
    endInclusive: OffsetDateTime,
    deltaInUs: Long = 1000,
): AbstractOffsetDateTimeAssert<*> {
    val start = startInclusive.minus(deltaInUs, ChronoUnit.MICROS)
    val end = endInclusive.plus(deltaInUs, ChronoUnit.MICROS)
    return this.isBetween(start, end)
}
