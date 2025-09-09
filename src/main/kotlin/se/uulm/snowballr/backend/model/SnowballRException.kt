package se.uulm.snowballr.backend.model

import org.simplejavamail.MailException
import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import java.io.IOException

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
     * @param entityIds The missing entity's ID(s).
     * @param identifierType The type of the entity's identifier. Defaults to [IdentifierType.ID].
     * @param location The location where the entity could not be found, e.g., the project or paper. Should start with " in ...".
     */
    open class NotFoundException(
        entityType: EntityType,
        vararg entityIds: String,
        identifierType: IdentifierType = IdentifierType.ID,
        location: String = "",
    ) : SnowballRException(
        "${entityType.singularUpper()} ${displayEntityIds(entityIds.toList(), identifierType)} not found$location.",
    )

    /**
     * Represents an exception indicating that a [ProjectPaper] entity could not be found within the context of a
     * specific project.
     *
     * @param localProjectPaperId The local identifier of the missing [ProjectPaper] entity.
     * @param projectId The identifier of the project in which the [ProjectPaper] could not be found.
     */
    open class ProjectPaperNotFoundException(
        localProjectPaperId: String,
        projectId: String,
    ) : NotFoundException(
        EntityType.PROJECT_PAPER,
        localProjectPaperId,
        identifierType = IdentifierType.LOCAL_ID,
        location = " in project with ID $projectId",
    )

    /**
     * Represents an exception that occurs when an entity already exists in the system
     * and creation is not allowed.
     *
     * @param entityType The type of the duplicated entity.
     * @param entityIds The missing entity's ID(s).
     * @param identifierType The type of the entity's identifier. Defaults to [IdentifierType.ID].
     */
    class DuplicateEntityException(
        entityType: EntityType,
        vararg entityIds: String,
        identifierType: IdentifierType = IdentifierType.ID,
    ) : SnowballRException(
        "${entityType.singularUpper()} ${displayEntityIds(entityIds.toList(), identifierType)}" +
            " already exists.",
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
     * @param accessType The type of access, i.e., an update, read access, ...
     */
    sealed class UnauthorizedException(
        currentUserId: String,
        accessedEntityMessage: String,
        accessType: AccessType,
    ) : SnowballRException("User with ID '$currentUserId' is not authorized to $accessType $accessedEntityMessage") {
        /**
         * Represents an [UnauthorizedException] that occurs when the current user accesses a single entity without
         * permission.
         *
         * @param accessedEntityType The type of the entity that was accessed without permission.
         * @param accessedEntityId The ID of the entity that was accessed without permission.
         * @param accessType The type of access (see [AccessType] for possible types).
         * @param currentUserId The ID of the user that is accessing the entity.
         * @param identifierType The type of the identifier used to access the entity.
         */
        class Single(
            accessedEntityType: EntityType,
            accessedEntityId: String,
            accessType: AccessType = AccessType.READ,
            currentUserId: String,
            identifierType: IdentifierType = IdentifierType.ID,
        ) : UnauthorizedException(
            currentUserId,
            "${accessedEntityType.singular} with ${identifierType.displayName} '$accessedEntityId'.",
            accessType,
        )

        /**
         * Represents an [UnauthorizedException] that occurs when the current user accesses several entities without
         * permission.
         *
         * @param accessedEntityType The type of the entities that were accessed without permission.
         * @param accessType The type of access (see [AccessType] for possible types).
         * @param currentUserId The ID of the user that is accessing the entities.
         */
        class All(
            accessedEntityType: EntityType,
            accessType: AccessType = AccessType.READ,
            currentUserId: String,
        ) : UnauthorizedException(currentUserId, "all ${accessedEntityType.plural}.", accessType)
    }

    /**
     * Represents a specific type of exception that occurs when a user is not authenticated.
     *
     * This exception is thrown when an operation requires user authentication,
     * but the user is not authenticated.
     *
     * @constructor Creates an [UnauthenticatedException] with a default message.
     */
    class UnauthenticatedException : SnowballRException("User is not authenticated.")

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
     * Represents a specific type of exception that occurs when a value is not in the correct range.
     *
     * @param value The value that is not in range.
     * @param from The left border of the correct range interval.
     * @param to The right border of the correct range interval.
     */
    sealed class OutOfRangeException(
        value: Number,
        from: Number,
        to: Number,
    ) : SnowballRException("The value $value is not in the range of from $from to $to.") {
        /**
         * Represents an [OutOfRangeException] that occurs when a [Stage] value is not in the correct range.
         *
         * @param stage The value of the stage.
         * @param maxStage The maximum value of the stages allowed.
         */
        class Stage(
            stage: Long,
            maxStage: Long,
        ) : OutOfRangeException(stage, 0, maxStage)
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
         * Represents a [MissingContextException] that occurs when the authentication status is missing in the context.
         */
        class MissingAuthenticationStatus : MissingContextException("Authentication status")

        /**
         * Represents a [MissingContextException] that occurs when the user ID is missing in the context.
         */
        class MissingUserId : MissingContextException("Authenticated user ID")

        /**
         * Represents a [MissingContextException] that occurs when the cookies map is missing in the context.
         */
        class MissingCookiesMap : MissingContextException("Cookie map")
    }

    /**
     * Represents a specific type of exception that occurs when a call has the wrong preconditions.
     *
     * @constructor Creates a [FailedPreconditionException] with the description of the failed precondition.
     * @param description The description of the failed precondition.
     */
    class FailedPreconditionException(
        description: String,
    ) : SnowballRException(description)

    /**
     * Represents an exception that occurs within the [EmailManager].
     *
     * @param message The message describing the email-related error.
     * @param cause The cause of the exception, which can be another exception, or null
     */
    sealed class EmailException(
        message: String,
        cause: Throwable? = null,
    ) : SnowballRException(message, cause) {
        /**
         * Thrown when an email template file cannot be found or compiled during application startup.
         * This is a fatal startup error.
         *
         * @param templateFileName The name of the file that failed to compile.
         * @param cause The original [IOException] from the template engine.
         */
        class TemplateCompilationFailed(
            templateFileName: String,
            cause: IOException,
        ) : EmailException("Failed to compile email template '$templateFileName'.", cause)

        /**
         * Thrown when the email provider fails to send an email.
         *
         * @param recipient The email address of the intended recipient.
         * @param cause The original [MailException] from the mailer library.
         */
        class MailSendFailed(
            recipient: String,
            cause: MailException,
        ) : EmailException("Mailer failed to send email to '$recipient'.", cause)
    }

    /**
     * Represents an exception that occurs when a verification token is not found.
     *
     * @constructor Creates a [VerificationTokenNotFoundException].
     */
    class VerificationTokenNotFoundException : SnowballRException("Verification token not found.")
}
