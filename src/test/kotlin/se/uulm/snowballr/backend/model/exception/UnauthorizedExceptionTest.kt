package se.uulm.snowballr.backend.model.exception

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedActionException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedDeleteException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedSingleException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import java.util.UUID
import kotlin.test.assertEquals

class UnauthorizedExceptionTest {
    private val currentUserId = UUID.randomUUID()

    @Nested
    inner class UnauthorizedActionExceptions {
        @Test
        fun `When creating an UnauthorizedActionException with AccessType#CREATE, then the message is correctly formatted`() {
            val projectId = UUID.randomUUID()
            val exception = UnauthorizedActionException(EntityType.PROJECT, projectId, AccessType.CREATE, currentUserId)

            assertEquals(
                "User with ID '$currentUserId' is not authorized to create something in project with ID '$projectId'.",
                exception.message,
            )
        }

        @Test
        fun `When creating an UnauthorizedActionException with AccessType#DELETE, then the message is correctly formatted`() {
            val projectId = UUID.randomUUID()
            val exception = UnauthorizedActionException(EntityType.PROJECT, projectId, AccessType.DELETE, currentUserId)

            assertEquals(
                "User with ID '$currentUserId' is not authorized to delete something from project with ID " +
                    "'$projectId'.",
                exception.message,
            )
        }
    }

    @Nested
    inner class UnauthorizedAllExceptions {
        @Test
        fun `When creating an UnauthorizedSingleException with AccessType#DELETE, then the message is correctly formatted`() {
            val exception = UnauthorizedAllException(currentUserId, AccessType.DELETE, EntityType.USER)

            assertEquals("User with ID '$currentUserId' is not authorized to delete all users.", exception.message)
        }

        @Test
        fun `When creating an UnauthorizedReadAllException, then the message is correctly formatted`() {
            val exception = UnauthorizedReadAllException(currentUserId, EntityType.USER)

            assertEquals("User with ID '$currentUserId' is not authorized to read all users.", exception.message)
        }
    }

    @Nested
    inner class UnauthorizedSingleExceptions {
        private val accessedEntityId = UUID.randomUUID()

        @Test
        fun `When creating an UnauthorizedSingleException with AccessType#CREATE, then the message is correctly formatted`() {
            val exception = UnauthorizedSingleException(
                currentUserId,
                accessedEntityId,
                IdentifierType.ID,
                AccessType.CREATE,
                EntityType.USER,
            )

            assertEquals("User with ID '$currentUserId' is not authorized to create user.", exception.message)
        }

        @ParameterizedTest
        @EnumSource(AccessType::class, names = ["CREATE"], mode = EnumSource.Mode.EXCLUDE)
        fun `When creating an UnauthorizedSingleException with any other AccessType than AccessType#CREATE, then the message is correctly formatted`(
            accessType: AccessType,
        ) {
            val exception = UnauthorizedSingleException(
                currentUserId,
                accessedEntityId,
                IdentifierType.ID,
                accessType,
                EntityType.USER,
            )

            assertEquals(
                "User with ID '$currentUserId' is not authorized to $accessType user with ID '$accessedEntityId'.",
                exception.message,
            )
        }

        @Test
        fun `When creating an UnauthorizedCreateException, then the message is correctly formatted`() {
            val exception = UnauthorizedCreateException(currentUserId, accessedEntityId, EntityType.USER)

            assertEquals("User with ID '$currentUserId' is not authorized to create user.", exception.message)
        }

        @Test
        fun `When creating an UnauthorizedUpdateException, then the message is correctly formatted`() {
            val exception = UnauthorizedUpdateException(currentUserId, accessedEntityId, EntityType.USER)

            assertEquals(
                "User with ID '$currentUserId' is not authorized to update user with ID '$accessedEntityId'.",
                exception.message,
            )
        }

        @Test
        fun `When creating an UnauthorizedReadException, then the message is correctly formatted`() {
            val exception = UnauthorizedReadException(currentUserId, accessedEntityId, EntityType.USER)

            assertEquals(
                "User with ID '$currentUserId' is not authorized to read user with ID '$accessedEntityId'.",
                exception.message,
            )
        }

        @Test
        fun `When creating an UnauthorizedDeleteException, then the message is correctly formatted`() {
            val exception = UnauthorizedDeleteException(currentUserId, accessedEntityId, EntityType.USER)

            assertEquals(
                "User with ID '$currentUserId' is not authorized to delete user with ID '$accessedEntityId'.",
                exception.message,
            )
        }
    }
}
