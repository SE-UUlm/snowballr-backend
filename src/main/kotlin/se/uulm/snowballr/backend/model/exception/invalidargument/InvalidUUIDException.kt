package se.uulm.snowballr.backend.model.exception.invalidargument

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.InvalidArgumentException

/**
 * Represents an exception that occurs when an ID is in an invalid UUID format.
 *
 * @param entityType The type of the entity to which the [InvalidUUIDException] belongs to.
 * @param id The value of the invalid UUID.
 */
class InvalidUUIDException(
    entityType: EntityType,
    id: Any,
) : InvalidArgumentException("The ID '$id' of the ${entityType.singular} is not a valid UUID.")
