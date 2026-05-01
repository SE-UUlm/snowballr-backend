package se.uulm.snowballr.backend.integration

import com.google.protobuf.FieldMask
import io.mockk.coEvery
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.Authentication
import snowballr.UserOuterClass.UserStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import snowballr.UserOuterClass.User as GrpcUser

class UserIntegrationTest : IntegrationTest() {
    @Nested
    inner class Register {
        @Test
        fun `When a user registers, then their status is unconfirmed until they verify their email`() = runTest {
            val newUser = DataBuilder.createExampleUser(email = "new.user@example.com")
            val tokenSlot = slot<String>()

            coEvery { emailManagerMock.createVerificationLink(capture(tokenSlot)) } returns "https://example.com/verify"
            coEvery { emailManagerMock.sendVerificationEmail(any(), any()) } returns Unit

            mainService.register(
                Authentication.RegisterRequest.newBuilder()
                    .setFirstName(newUser.firstName)
                    .setLastName(newUser.lastName)
                    .setEmail(newUser.email)
                    .setPassword("SecureP@ssw0rd!")
                    .build(),
            )

            val unverifiedUser = mainService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED, unverifiedUser.status)

            mainService.verifyEmail(
                Authentication.VerifyEmailRequest.newBuilder()
                    .setToken(tokenSlot.captured)
                    .build(),
            )

            val verifiedUser = mainService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.USER_STATUS_ACTIVE, verifiedUser.status)
        }
    }

    @Nested
    inner class GetUser {
        @Test
        fun `When the current user requests their own data, then their data is returned`() = runTest {
            val currentUser = mainService.getCurrentUser()

            assertEquals(testUserId.toString(), currentUser.id)
        }

        @Test
        fun `When an admin requests another user by ID, then the user's data is returned`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            val fetchedUser = mainService.getUserById(otherUserId)

            assertEquals(otherUser.id, fetchedUser.id)
            assertEquals(otherUser.email, fetchedUser.email)
        }

        @Test
        fun `When an admin requests all users, then all users are included in the response`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            val allUsers = mainService.getAllUsers()
            val userIds = allUsers.usersList.map { it.id }

            assertEquals(2, allUsers.usersList.size)
            assertTrue(userIds.contains(testUserId.toString()))
            assertTrue(userIds.contains(otherUser.id))
        }

        @Test
        fun `When a non-admin requests all users, then access is denied`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            actAsUser(otherUserId) {
                assertThrows<UnauthorizedReadAllException> { mainService.getAllUsers() }
            }
        }
    }

    @Nested
    inner class UpdateUser {
        @Test
        fun `When a user updates their own first name, then the updated name is persisted`() = runTest {
            val currentUser = mainService.getCurrentUser()
            val newFirstName = "UpdatedFirstName"

            val request = GrpcUser.Update.newBuilder()
                .setUser(GrpcUser.newBuilder().setId(currentUser.id).setFirstName(newFirstName))
                .setMask(FieldMask.newBuilder().addPaths("user.first_name"))
                .build()

            val updatedUser = mainService.updateUser(request)

            assertEquals(newFirstName, updatedUser.firstName)
        }

        @Test
        fun `When an admin updates another user's first name, then the updated name is persisted`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val newFirstName = "AdminUpdatedName"

            val request = GrpcUser.Update.newBuilder()
                .setUser(GrpcUser.newBuilder().setId(otherUser.id).setFirstName(newFirstName))
                .setMask(FieldMask.newBuilder().addPaths("user.first_name"))
                .build()

            val updatedUser = mainService.updateUser(request)

            assertEquals(newFirstName, updatedUser.firstName)
        }
    }

    @Nested
    inner class DeleteUser {
        @Test
        fun `When an admin soft-deletes a non-admin user, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            assertDoesNotThrow { mainService.softDeleteUser(otherUserId) }
        }

        @Test
        fun `When a user soft-deletes themselves, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            actAsUser(otherUserId) {
                assertDoesNotThrow { mainService.softDeleteUser(otherUserId) }
            }
        }
    }
}
