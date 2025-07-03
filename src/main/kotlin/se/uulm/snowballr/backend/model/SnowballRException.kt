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
     * Represents an exception that occurs when an entity cannot be found by its identifier.
     *
     * @constructor Creates a [NotFoundException] with the type and ID of the missing entity.
     * @param entityType The type of the entity that could not be found.
     * @param entityId The unique identifier of the missing entity.
     * @param identifierType The type of the [identifierType] field. Default to [IdentifierType.ID].
     */
    open class NotFoundException(
        entityType: EntityType,
        entityId: String,
        identifierType: IdentifierType = IdentifierType.ID,
    ) : SnowballRException("${entityType.singularUpper()} with ${identifierType.displayName} '$entityId' not found.")

    /**
     * Represents an exception that occurs when an entity already exists in the system
     * and creation is not allowed.
     *
     * @constructor Creates a [DuplicateEntityException] with a message about the entity.
     * @param entityType The type of the duplicated entity.
     * @param identifier The identifying value (e.g., email, username).
     * @param identifierType The type of the [identifier] field. Default to [IdentifierType.ID].
     */
    sealed class DuplicateEntityException(
        entityType: EntityType,
        identifier: String,
        identifierType: IdentifierType = IdentifierType.ID,
    ) : SnowballRException(
        "${entityType.singularUpper()} with ${identifierType.displayName} '$identifier' already exists.",
    ) {
        class UserEmail(
            email: String,
        ) : DuplicateEntityException(EntityType.USER, email, IdentifierType.EMAIL)
    }

    /**
     * Represents an exception that occurs when an entity creation was triggered, but it couldn't be fetched afterward.
     *
     * @constructor Creates a [EntityNotPersistedException] with the type and ID of the not persisted entity.
     * @param entityType The type of the entity that was not persisted.
     * @param entityId The unique identifier of the not persisted entity.
     */
    class EntityNotPersistedException(
        entityType: EntityType,
        entityId: String,
    ) : SnowballRException("${entityType.singularUpper()} with ID '$entityId' was not persisted.")

    /**
     * Represents an exception that occurs when the current user accesses one or more entities, but they don't have the
     * required permission.
     *
     * @constructor Creates an [UnauthorizedException] with the current user's ID, the type and ID of the accessed
     * entity.
     * @param currentUserId The ID of the user that is accessing the entity.
     * @param accessedEntityMessage The message of what is accessed.
     */
    sealed class UnauthorizedException(
        currentUserId: String,
        accessedEntityMessage: String,
    ) : SnowballRException("User with ID '$currentUserId' is not authorized to access $accessedEntityMessage") {
        class Single(
            currentUserId: String,
            accessedEntityType: EntityType,
            accessedEntityId: String,
            identifierType: IdentifierType = IdentifierType.ID,
        ) : UnauthorizedException(
            currentUserId,
            "${accessedEntityType.singular} with ${identifierType.displayName} '$accessedEntityId'.",
        )

        class All(
            currentUserId: String,
            accessedEntityType: EntityType,
        ) : UnauthorizedException(currentUserId, "all ${accessedEntityType.plural}.")
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
        entityType: EntityType,
        format: String,
    ) : SnowballRException("The ID '$id' of the ${entityType.singular} is not a valid $format.") {
        class UUID(
            id: String,
            entityType: EntityType,
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
