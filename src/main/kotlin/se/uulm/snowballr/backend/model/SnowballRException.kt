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
     * Represents an exception that occurs when an entity already exists in the system
     * and creation is not allowed.
     *
     * @constructor Creates a [DuplicateEntityException] with a message about the entity.
     * @param entityType The type of the duplicated entity.
     * @param identifier The identifying value (e.g., email, username).
     * @param identifierName The name of the identifier field (defaults to "ID").
     */
    sealed class DuplicateEntityException(
        entityType: String,
        identifier: String,
        identifierName: String = "ID",
    ) : SnowballRException("$entityType with $identifierName '$identifier' already exists.") {
        class UserEmail(
            email: String,
        ) : DuplicateEntityException("User", email, "email")
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

        class ProjectMember(
            projectMemberId: String,
        ) : EntityNotPersistedException("ProjectMember", projectMemberId)

        class User(
            userId: String,
        ) : EntityNotPersistedException("User", userId)
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
            identifier: String? = "ID",
        ) : UnauthorizedException(currentUserId, "$accessedEntityType with $identifier '$accessedEntityId'.") {
            class User(
                currentUserId: String,
                identifier: String? = "ID",
                accessedUserId: String,
            ) : Single(currentUserId, "user", accessedUserId, identifier)
        }

        sealed class All(
            currentUserId: String,
            accessedEntityType: String,
        ) : UnauthorizedException(currentUserId, "all $accessedEntityType.") {
            class User(
                currentUserId: String,
            ) : All(currentUserId, "users")

            class Project(
                currentUserId: String,
            ) : All(currentUserId, "projects")
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

    /**
     * Represents an exception that occurs when expected gRPC context data is missing.
     *
     * This may indicate a misconfigured interceptor, a bug in the server flow,
     * or a misuse of context propagation.
     *
     * @constructor Creates a [MissingContextException] with a description of the missing value.
     * @param keyDescription A human-readable description of the missing key or context value.
     */
    sealed class MissingContextException(
        keyDescription: String,
    ) : SnowballRException("Missing context value: $keyDescription") {
        class MissingUserId : MissingContextException("Authenticated user ID")

        class MissingCookiesMap : MissingContextException("Cookie map")
    }
}
