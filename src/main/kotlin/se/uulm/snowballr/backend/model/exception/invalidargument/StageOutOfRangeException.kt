package se.uulm.snowballr.backend.model.exception.invalidargument

import se.uulm.snowballr.backend.model.exception.InvalidArgumentException

/**
 * Represents an exception that occurs when a stage value is not in the range of 0 to the maximum stage allowed.
 *
 * @param stage The value of the stage.
 * @param maxStage The maximum value of the stages allowed.
 */
class StageOutOfRangeException(
    stage: Long,
    maxStage: Long,
) : InvalidArgumentException("The stage $stage is not in the valid range from 0 to $maxStage.")
