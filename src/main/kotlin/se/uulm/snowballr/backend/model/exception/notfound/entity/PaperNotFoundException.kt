package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a paper could not be found by its ID.
 *
 * @param paperId The ID of the missing paper.
 */
class PaperNotFoundException(
    paperId: UUID,
) : EntityNotFoundException(EntityType.PAPER, paperId)
