package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.EntityType

/**
 * Represents a [FailedPreconditionException] that indicates that an entity is not active.
 *
 * @param entityType The type of the entity.
 * @param entityId The ID of the entity.
 */
class EntityNotActiveException(
    entityType: EntityType,
    entityId: String,
) : FailedPreconditionException("The ${entityType.singularUpper()} with ID '$entityId' is not active.")
