package se.uulm.snowballr.backend.model.exception.unauthorized

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthorizedException
import java.util.UUID

/**
 * Represents an exception that occurs when the current user attempts to perform a CRUD operation on a single entity
 * without permission.
 */
open class UnauthorizedSingleException(
    currentUserId: UUID,
    accessedEntityId: UUID,
    identifierType: IdentifierType,
    accessType: AccessType,
    accessedEntityType: EntityType,
) : UnauthorizedException(
    currentUserId,
    accessType,
    if (accessType == AccessType.CREATE) {
        "${accessedEntityType.singular}."
    } else {
        "${accessedEntityType.singular} with ${identifierType.displayName} '$accessedEntityId'."
    },
)

/**
 * Represents an exception that occurs when the current user attempts to create a new entity without permission.
 */
class UnauthorizedCreateException(
    currentUserId: UUID,
    accessedEntityId: UUID,
    accessedEntityType: EntityType,
    identifierType: IdentifierType = IdentifierType.ID,
) : UnauthorizedSingleException(currentUserId, accessedEntityId, identifierType, AccessType.CREATE, accessedEntityType)

/**
 * Represents an exception that occurs when the current user attempts to read an entity without permission.
 */
class UnauthorizedReadException(
    currentUserId: UUID,
    accessedEntityId: UUID,
    accessedEntityType: EntityType,
    identifierType: IdentifierType = IdentifierType.ID,
) : UnauthorizedSingleException(currentUserId, accessedEntityId, identifierType, AccessType.READ, accessedEntityType)

/**
 * Represents an exception that occurs when the current user attempts to update an entity without permission.
 */
class UnauthorizedUpdateException(
    currentUserId: UUID,
    accessedEntityId: UUID,
    accessedEntityType: EntityType,
    identifierType: IdentifierType = IdentifierType.ID,
) : UnauthorizedSingleException(currentUserId, accessedEntityId, identifierType, AccessType.UPDATE, accessedEntityType)

/**
 * Represents an exception that occurs when the current user attempts to delete an entity without permission.
 */
class UnauthorizedDeleteException(
    currentUserId: UUID,
    accessedEntityId: UUID,
    accessedEntityType: EntityType,
    identifierType: IdentifierType = IdentifierType.ID,
) : UnauthorizedSingleException(currentUserId, accessedEntityId, identifierType, AccessType.DELETE, accessedEntityType)
