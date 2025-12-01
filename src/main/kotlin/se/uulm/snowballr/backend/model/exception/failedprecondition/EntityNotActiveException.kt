package se.uulm.snowballr.backend.model.exception.failedprecondition

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException

/**
 * Represents an exception that occurs when an entity is not active but should be.
 *
 * @param entityType The type of the entity.
 * @param entityId The ID of the entity.
 */
class EntityNotActiveException(
    entityType: EntityType,
    entityId: Any,
) : FailedPreconditionException("The ${entityType.singularUpper()} with ID '$entityId' is not active.")
