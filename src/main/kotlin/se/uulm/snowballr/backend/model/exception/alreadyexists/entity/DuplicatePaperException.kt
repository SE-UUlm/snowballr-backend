package se.uulm.snowballr.backend.model.exception.alreadyexists.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateEntityException

/**
 * Represents an exception that occurs when a paper with the given external ID already exists.
 *
 * @param externalIds The external IDs of the already existent paper.
 */
@Suppress("SpreadOperator")
class DuplicatePaperException(
    externalIds: List<ExternalId>,
) : DuplicateEntityException(
    EntityType.PAPER,
    *externalIds.toTypedArray(),
    identifierType = IdentifierType.EXTERNAL_ID,
)
