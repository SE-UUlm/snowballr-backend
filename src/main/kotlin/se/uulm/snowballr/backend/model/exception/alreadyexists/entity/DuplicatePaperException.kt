package se.uulm.snowballr.backend.model.exception.alreadyexists.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateEntityException

/**
 * Represents an exception that occurs when a paper with the given external ID already exists.
 *
 * @param externalId The external ID of the already existent paper.
 */
class DuplicatePaperException(
    externalId: String,
) : DuplicateEntityException(
    EntityType.PAPER,
    externalId,
    identifierType = IdentifierType.EXTERNAL_ID,
)
