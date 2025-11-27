package se.uulm.snowballr.backend.model.exception.unauthorized

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthorizedException
import java.util.UUID

/**
 * Factory for creating [UnauthorizedException]s.
 */
object UnauthorizedExceptionFactory {
    /**
     * Creates an [UnauthorizedException] for the given [accessType] on the given single entity of type
     * [accessedEntityType] with the ID [accessedEntityId].
     */
    fun createForAccessType(
        accessType: AccessType,
        currentUserId: UUID,
        accessedEntityId: UUID,
        accessedEntityType: EntityType,
        identifierType: IdentifierType = IdentifierType.ID,
    ) = when (accessType) {
        AccessType.CREATE -> UnauthorizedCreateException(
            currentUserId,
            accessedEntityId,
            accessedEntityType,
            identifierType,
        )

        AccessType.READ -> UnauthorizedReadException(
            currentUserId,
            accessedEntityId,
            accessedEntityType,
            identifierType,
        )

        AccessType.UPDATE -> UnauthorizedUpdateException(
            currentUserId,
            accessedEntityId,
            accessedEntityType,
            identifierType,
        )

        AccessType.DELETE -> UnauthorizedDeleteException(
            currentUserId,
            accessedEntityId,
            accessedEntityType,
            identifierType,
        )
    }
}
