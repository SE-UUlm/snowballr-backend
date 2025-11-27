package se.uulm.snowballr.backend.model.exception.internal

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.InternalException

/**
 * Represents an exception that occurs when an entity creation was triggered, but it couldn't be fetched afterward.
 *
 * @param entityType The type of the entity that was not persisted.
 * @param entityId The unique identifier of the not persisted entity.
 */
class EntityNotPersistedException(
    entityType: EntityType,
    entityId: String,
) : InternalException("${entityType.singularUpper()} with ID '$entityId' was not persisted.")
