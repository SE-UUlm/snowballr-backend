package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents a specific type of exception that occurs when a value is not in the correct range.
 *
 * @param value The value that is not in range.
 * @param from The left border of the correct range interval.
 * @param to The right border of the correct range interval.
 */
sealed class OutOfRangeException(
    value: Number,
    from: Number,
    to: Number,
) : SnowballRException(Status.INVALID_ARGUMENT, "The value $value is not in the range of from $from to $to.") {
    /**
     * Represents an [OutOfRangeException] that occurs when a [Stage] value is not in the correct range.
     *
     * @param stage The value of the stage.
     * @param maxStage The maximum value of the stages allowed.
     */
    class Stage(
        stage: Long,
        maxStage: Long,
    ) : OutOfRangeException(stage, 0, maxStage)
}
