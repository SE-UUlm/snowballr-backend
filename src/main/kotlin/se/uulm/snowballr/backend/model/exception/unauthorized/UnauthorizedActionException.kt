package se.uulm.snowballr.backend.model.exception.unauthorized

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import java.util.UUID

/**
 * Represents an [UnauthorizedException] that occurs when the current user performs an action on an entity.
 *
 * @param accessedEntityType The type of entity that the action was attempted on, represented as [EntityType].
 * @param accessedEntityId The unique identifier of the entity being accessed.
 * @param accessType The type of access being attempted, such as READ, CREATE, UPDATE, or DELETE.
 * @param currentUserId The identifier of the user attempting the unauthorized action.
 * @param identifierType The type of identifier used for the entity, typically from [IdentifierType]. Defaults
 * to [IdentifierType.ID].
 */
class UnauthorizedActionException(
    accessedEntityType: EntityType,
    accessedEntityId: Any,
    accessType: AccessType,
    currentUserId: UUID,
    identifierType: IdentifierType = IdentifierType.ID,
) : UnauthorizedException(
    currentUserId,
    accessType,
    "something ${
        if (accessType in listOf(AccessType.DELETE, AccessType.READ)) {
            "from"
        } else {
            "in"
        }
    } ${accessedEntityType.singular} with ${identifierType.displayName} '$accessedEntityId'.",
)
