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
     * scenarios where a particular entity, identified by its name and ID, is not found.
     * It serves as a base class to define more specific "not found" exceptions for various entities.
     *
     * @constructor Creates a [NotFoundException] with the name and ID of the missing entity.
     * @param entityName The name of the entity that could not be found.
     * @param entityId The unique identifier of the missing entity.
     * @param identifier The name of the identifier. Defaults to 'ID'.
     */
    sealed class NotFoundException(
        entityName: String,
        entityId: String,
        identifier: String? = "ID",
    ) : SnowballRException("$entityName with $identifier '$entityId' not found.") {
        class Project(
            projectId: String,
        ) : NotFoundException("Project", projectId)

        class User(
            userId: String,
            identifier: String? = null,
        ) : NotFoundException("User", userId, identifier)
    }

    /**
     * Represents a specific type of exception that occurs when an entity creation was triggered, but it couldn't be
     * fetched afterward.
     *
     * @constructor Creates a [EntityNotPersistedException] with the name and ID of the not persisted entity.
     * @param entityName The name of the entity that was not persisted.
     * @param entityId The unique identifier of the not persisted entity.
     */
    sealed class EntityNotPersistedException(
        entityName: String,
        entityId: String,
    ) : SnowballRException("$entityName with ID '$entityId' was not persisted.") {
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
     * scenarios where a particular entity is accessed, but the current user is not
     * authorized to do so.
     * It serves as a base class to define more specific "unauthorized" exceptions for
     * various entities.
     *
     * @constructor Creates an [UnauthorizedException] with the current user's ID, the
     * name and ID of the accessed entity.
     * @param currentUserId The ID of the user that is accessing the entity.
     * @param accessedEntityMessage The message of what is accessed.
     */
    sealed class UnauthorizedException(
        currentUserId: String,
        accessedEntityMessage: String,
    ) : SnowballRException("User with ID '$currentUserId' is not authorized to access $accessedEntityMessage") {
        sealed class Single(
            currentUserId: String,
            accessedEntityName: String,
            accessedEntityId: String,
        ) : UnauthorizedException(currentUserId, "$accessedEntityName with ID '$accessedEntityId'.") {
            class Project
        }

        sealed class All(
            currentUserId: String,
            accessedEntityName: String,
        ) : UnauthorizedException(currentUserId, "all $accessedEntityName.") {
            class User(
                currentUserId: String,
            ) : UnauthorizedException(currentUserId, "user")
        }
    }
}
