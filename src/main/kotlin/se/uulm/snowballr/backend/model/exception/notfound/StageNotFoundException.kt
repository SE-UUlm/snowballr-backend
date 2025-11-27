package se.uulm.snowballr.backend.model.exception.notfound

import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException

/**
 * Represents an exception that occurs when a stage in a [Project] is not found.
 */
class StageNotFoundException(stage: Long) : NotFoundException("Stage '$stage' not found.")
