package se.uulm.snowballr.backend.model

/**
 * Base class for all exceptions in the SnowballR application.
 *
 * Used to encapsulate specific error details and provide a consistent exception structure.
 * Can be extended to create more detailed exceptions specific to various error scenarios.
 *
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
     * @param entityType The type of the duplicated entity.
     * @param identifier The identifying value (e.g., email, username).
     * @param identifierType The type of the [identifier] field. Default to [IdentifierType.ID].
     */
    class DuplicateEntityException(
        entityType: EntityType,
        identifier: String,
        identifierType: IdentifierType = IdentifierType.ID,
    ) : SnowballRException(
        "${entityType.singularUpper()} with ${identifierType.displayName} '$identifier' already exists.",
    )

    /**
     * Represents an exception that occurs when an entity creation was triggered, but it couldn't be fetched afterward.
     *
     * @param entityType The type of the entity that was not persisted.
     * @param entityId The unique identifier of the not persisted entity.
     */
    class EntityNotPersistedException(
        entityType: EntityType,
        entityId: String,
    ) : SnowballRException("${entityType.singularUpper()} with ID '$entityId' was not persisted.")

    /**
     * Represents an exception that occurs when the current user accesses one or more entities without permission.
     *
     * @param currentUserId The ID of the user that is accessing the entity/entities.
     * @param accessedEntityMessage The message of what is accessed.
     */
    sealed class UnauthorizedException(
        currentUserId: String,
        accessedEntityMessage: String,
    ) : SnowballRException("User with ID '$currentUserId' is not authorized to access $accessedEntityMessage") {
        /**
         * Represents an [UnauthorizedException] that occurs when the current user accesses a single entity without
         * permission.
         *
         * @param accessedEntityType The type of the entity that was accessed without permission.
         * @param accessedEntityId The ID of the entity that was accessed without permission.
         * @param currentUserId The ID of the user that is accessing the entity.
         * @param identifierType The type of the identifier used to access the entity.
         */
        class Single(
            accessedEntityType: EntityType,
            accessedEntityId: String,
            currentUserId: String,
            identifierType: IdentifierType = IdentifierType.ID,
        ) : UnauthorizedException(
            currentUserId,
            "${accessedEntityType.singular} with ${identifierType.displayName} '$accessedEntityId'.",
        )

        /**
         * Represents an [UnauthorizedException] that occurs when the current user accesses several entities without
         * permission.
         *
         * @param accessedEntityType The type of the entities that were accessed without permission.
         * @param currentUserId The ID of the user that is accessing the entities.
         */
        class All(
            accessedEntityType: EntityType,
            currentUserId: String,
        ) : UnauthorizedException(currentUserId, "all ${accessedEntityType.plural}.")
    }

    /**
     * Represents an exception that occurs when an ID is in an invalid format.
     *
     * @param entityType The type of the entity to which the ID belongs to.
     * @param entityId The value of the invalid ID.
     * @param format The format that the ID should've had.
     */
    sealed class InvalidIdException(
        entityType: EntityType,
        entityId: String,
        format: String,
    ) : SnowballRException("The ID '$entityId' of the ${entityType.singular} is not a valid $format.") {
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

    /**
     * Represents an exception that occurs when expected gRPC context data is missing.
     *
     * This may indicate a misconfigured interceptor, a bug in the server flow,
     * or a misuse of context propagation.
     *
     * @param keyDescription A human-readable description of the missing key or context value.
     */
    sealed class MissingContextException(
        keyDescription: String,
    ) : SnowballRException("Missing context value: $keyDescription") {
        /**
         * Represents a [MissingContextException] that occurs when the user ID is missing in the context.
         */
        class MissingUserId : MissingContextException("Authenticated user ID")

        /**
         * Represents a [MissingContextException] that occurs when the cookies map is missing in the context.
         */
        class MissingCookiesMap : MissingContextException("Cookie map")
    }
}
