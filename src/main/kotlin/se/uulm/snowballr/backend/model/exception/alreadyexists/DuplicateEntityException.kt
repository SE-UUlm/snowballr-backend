package se.uulm.snowballr.backend.model.exception.alreadyexists

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.displayEntityIds
import se.uulm.snowballr.backend.model.exception.AlreadyExistsException

/**
 * Represents an exception that occurs when an entity already exists in the system and creation is not allowed.
 *
 * @param entityType The type of the duplicated entity.
 * @param entityIds The missing entity's ID(s).
 * @param identifierType The type of the entity's identifier. Defaults to [IdentifierType.ID].
 */
open class DuplicateEntityException protected constructor(
    entityType: EntityType,
    vararg entityIds: Any,
    identifierType: IdentifierType = IdentifierType.ID,
) : AlreadyExistsException(
    "${entityType.singularUpper()} ${displayEntityIds(entityIds.toList(), identifierType)} already exists.",
)
