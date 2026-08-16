package se.uulm.snowballr.backend.model.exception

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.Status
import java.util.UUID

/**
 * Represents an exception that occurs when the current user accesses one or more entities without permission.
 *
 * @param currentUserId The ID of the user that is accessing the entity/entities.
 * @param accessType The type of access, i.e., the CRUD operation.
 * @param accessedEntityMessage The message of what is accessed.
 */
open class UnauthorizedException protected constructor(
    currentUserId: UUID,
    accessType: AccessType,
    accessedEntityMessage: String,
) : SnowballRException(
    Status.FORBIDDEN,
    "User with ID '$currentUserId' is not authorized to $accessType $accessedEntityMessage",
)
