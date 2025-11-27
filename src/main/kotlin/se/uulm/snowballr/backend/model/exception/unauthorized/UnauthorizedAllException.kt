package se.uulm.snowballr.backend.model.exception.unauthorized

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthorizedException
import java.util.UUID

/**
 * Represents an exception that occurs when the current user attempts to perform an operation on all entities without
 * permission.
 */
open class UnauthorizedAllException(
    currentUserId: UUID,
    accessType: AccessType,
    accessedEntityType: EntityType,
) : UnauthorizedException(currentUserId, accessType, "all ${accessedEntityType.plural}.")

/**
 * Represents an exception that occurs when the current user attempts to read several entities without permission.
 */
class UnauthorizedReadAllException(
    currentUserId: UUID,
    accessedEntityType: EntityType,
) : UnauthorizedAllException(currentUserId, AccessType.READ, accessedEntityType)
