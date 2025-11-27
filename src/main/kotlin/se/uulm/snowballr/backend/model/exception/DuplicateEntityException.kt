package se.uulm.snowballr.backend.model.exception

import io.grpc.Status
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.displayEntityIds

/**
 * Represents an exception that occurs when an entity already exists in the system
 * and creation is not allowed.
 *
 * @param entityType The type of the duplicated entity.
 * @param entityIds The missing entity's ID(s).
 * @param identifierType The type of the entity's identifier. Defaults to [se.uulm.snowballr.backend.model.IdentifierType.ID].
 */
class DuplicateEntityException(
    entityType: EntityType,
    vararg entityIds: String,
    identifierType: IdentifierType = IdentifierType.ID,
) : SnowballRException(
    Status.ALREADY_EXISTS,
    "${entityType.singularUpper()} ${displayEntityIds(entityIds.toList(), identifierType)} already exists.",
)
