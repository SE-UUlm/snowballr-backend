package se.uulm.snowballr.backend.model

/**
 * Base class for all exceptions in the SnowballR application.
 *
 * Used to encapsulate specific error details and provide a consistent exception structure.
 * Can be extended to create more detailed exceptions specific to various error scenarios.
 *
 * @constructor Creates an instance of [SnowballRException] with an optional message and cause.
 * @param message Detailed message describing the reason for the exception, or null if not provided.
 * @param cause The cause of the exception, which can be another exception, or null if not provided.
 */
sealed class SnowballRException(
    message: String? = null,
    cause: Throwable? = null,
) : RuntimeException(message, cause) {
    /**
     * Represents a specific type of exception that occurs when an entity cannot be found.
     *
     * This exception is intended to provide a clear and structured way to handle
     * scenarios where a particular entity, identified by its type and ID, is not found.
     * It serves as a base class to define more specific "not found" exceptions for various entities.
     *
     * @constructor Creates a [NotFoundException] with the type and ID of the missing entity.
     * @param entityType The type of the entity that could not be found.
     * @param entityId The unique identifier of the missing entity.
     * @param identifier The name of the identifier. Defaults to 'ID'.
     */
    sealed class NotFoundException(
        entityType: String,
        entityId: String,
        identifier: String? = "ID",
    ) : SnowballRException("$entityType with $identifier '$entityId' not found.") {
        class Project(
            projectId: String,
        ) : NotFoundException("Project", projectId)

        class User(
            userId: String,
            identifier: String? = "ID",
        ) : NotFoundException("User", userId, identifier)
    }

    /**
     * Represents a specific type of exception that occurs when an entity creation was triggered, but it couldn't be
     * fetched afterward.
     *
     * @constructor Creates a [EntityNotPersistedException] with the type and ID of the not persisted entity.
     * @param entityType The type of the entity that was not persisted.
     * @param entityId The unique identifier of the not persisted entity.
     */
    sealed class EntityNotPersistedException(
        entityType: String,
        entityId: String,
    ) : SnowballRException("$entityType with ID '$entityId' was not persisted.") {
        class Project(
            projectId: String,
        ) : EntityNotPersistedException("Project", projectId)

        class Criterion(
            criterionId: String,
        ) : EntityNotPersistedException("Criterion", criterionId)
    }

    /**
     * Represents a specific type of exception that occurs when an entity is accessed by
     * the current user, but they are not authorized.
     *
     * This exception is intended to provide a clear and structured way to handle
     * scenarios where a particular entity is accessed without permission.
     * It serves as a base class to define more specific "unauthorized" exceptions for
     * various entities.
     *
     * @constructor Creates an [UnauthorizedException] with the current user's ID, the
     * type and ID of the accessed entity.
     * @param currentUserId The ID of the user that is accessing the entity.
     * @param accessedEntityMessage The message of what is accessed.
     */
    sealed class UnauthorizedException(
        currentUserId: String,
        accessedEntityMessage: String,
    ) : SnowballRException("User with ID '$currentUserId' is not authorized to access $accessedEntityMessage") {
        sealed class Single(
            currentUserId: String,
            accessedEntityType: String,
            accessedEntityId: String,
        ) : UnauthorizedException(currentUserId, "$accessedEntityType with ID '$accessedEntityId'.")

        sealed class All(
            currentUserId: String,
            accessedEntityType: String,
        ) : UnauthorizedException(currentUserId, "all $accessedEntityType.") {
            class User(
                currentUserId: String,
            ) : All(currentUserId, "users")
        }
    }

    /**
     * Represents a specific type of exception that occurs when an ID is in an invalid format.
     *
     * @constructor Creates an [InvalidIdException] with the value and format of the invalid ID.
     * @param id The value of the invalid ID.
     * @param entityType The type of the entity to which the ID belongs to.
     * @param format The format that the ID should've had.
     */
    sealed class InvalidIdException(
        id: String,
        entityType: String,
        format: String,
    ) : SnowballRException("The ID '$id' of the $entityType is not a valid $format.") {
        class UUID(
            id: String,
            entityType: String,
        ) : InvalidIdException(id, entityType, "UUID")
    }
}
