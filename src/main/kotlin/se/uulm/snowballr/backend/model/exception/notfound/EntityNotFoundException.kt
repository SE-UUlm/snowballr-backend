package se.uulm.snowballr.backend.model.exception.notfound

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.displayEntityIds
import se.uulm.snowballr.backend.model.exception.NotFoundException

/**
 * Represents an exception that occurs when an entity could not be found by its identifier(s).
 *
 * @param entityType The type of the entity that could not be found.
 * @param entityIds The ID(s) of the missing entity.
 * @param identifierType The type of the entity's identifier. Defaults to [IdentifierType.ID].
 * @param location The location where the entity could not be found, e.g., the project or paper. Should start with
 * " in ...".
 */
open class EntityNotFoundException(
    entityType: EntityType,
    vararg entityIds: Any,
    identifierType: IdentifierType = IdentifierType.ID,
    location: String = "",
) : NotFoundException(
    "${entityType.singularUpper()} ${displayEntityIds(entityIds.toList(), identifierType)} not found$location.",
)
