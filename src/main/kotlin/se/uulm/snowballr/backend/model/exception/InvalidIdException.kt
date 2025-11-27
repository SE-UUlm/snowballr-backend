package se.uulm.snowballr.backend.model.exception

import io.grpc.Status
import se.uulm.snowballr.backend.model.EntityType

/**
 * Represents a specific type of exception that occurs when an ID is in an invalid format.
 *
 * @param entityType The type of the entity to which the ID belongs to.
 * @param entityId The value of the invalid ID.
 * @param format The format that the ID should've had.
 */
sealed class InvalidIdException(
    entityType: EntityType,
    entityId: String,
    format: String,
) : SnowballRException(
    Status.INVALID_ARGUMENT,
    "The ID '$entityId' of the ${entityType.singular} is not a valid $format.",
) {
    /**
     * Represents an [InvalidIdException] that occurs when a [UUID] is in an invalid format.
     *
     * @param entityType The type of the entity to which the [UUID] belongs to.
     * @param id The value of the invalid [UUID].
     */
    class UUID(
        entityType: EntityType,
        id: String,
    ) : InvalidIdException(entityType, id, "UUID")
}
