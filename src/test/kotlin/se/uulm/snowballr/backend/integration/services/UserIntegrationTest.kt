package se.uulm.snowballr.backend.integration.services

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.integration.IntegrationTest
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus.Companion.fromGrpc
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.model.parseUUID
import snowballr.Authentication
import snowballr.UserOuterClass.UserStatus
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class UserIntegrationTest : IntegrationTest() {
    @Nested
    inner class Register {
        @Test
        fun `When a user registers, then their status is unconfirmed until they verify their email`() = runTest {
            val newUser = DataBuilder.createExampleUser(email = "new.user@example.com")
            val tokenSlot = slot<String>()

            coEvery { emailManagerMock.createVerificationLink(capture(tokenSlot)) } returns "https://example.com/verify"
            coJustRun { emailManagerMock.sendVerificationEmail(any(), any()) }

            userService.register(
                RegisterRequest(
                    firstName = newUser.firstName,
                    lastName = newUser.lastName,
                    email = newUser.email,
                    password = "SecureP@ssw0rd!",
                ),
            )

            val unverifiedUser = userService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED, unverifiedUser.status)

            authenticationService.verifyEmail(
                Authentication.VerifyEmailRequest.newBuilder()
                    .setToken(tokenSlot.captured)
                    .build(),
            )

            val verifiedUser = userService.getUserByEmail(newUser.email)
            assertEquals(UserStatus.USER_STATUS_ACTIVE, verifiedUser.status)
        }
    }

    @Nested
    inner class GetUser {
        @Test
        fun `When the current user requests their own data, then their data is returned`() = runTest {
            val currentUser = userService.getCurrentUser()

            assertEquals(testUserId.toString(), currentUser.id)
        }

        @Test
        fun `When an admin requests another user by ID, then the user's data is returned`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            val fetchedUser = userService.getUserById(otherUserId)

            assertEquals(otherUser.id, fetchedUser.id)
            assertEquals(otherUser.email, fetchedUser.email)
        }

        @Test
        fun `When an admin requests all users, then all users are included in the response`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))

            val allUsers = userService.getAllUsers()
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
                assertThrows<UnauthorizedReadAllException> { userService.getAllUsers() }
            }
        }
    }

    @Nested
    inner class UpdateUser {
        @Test
        fun `When a user updates their own first name, then the updated name is persisted`() = runTest {
            val currentUser = userService.getCurrentUser()
            val newFirstName = "UpdatedFirstName"

            val request = UpdateUserRequest(
                userId = parseUUID(currentUser.id, EntityType.USER),
                firstName = newFirstName,
                lastName = currentUser.lastName,
                email = currentUser.email,
                role = UserRole.fromGrpc(currentUser.role),
                status = fromGrpc(currentUser.status),
            )

            val updatedUser = userService.updateUser(request, listOf("user.first_name"))

            assertEquals(newFirstName, updatedUser.firstName)
        }

        @Test
        fun `When an admin updates another user's first name, then the updated name is persisted`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val newFirstName = "AdminUpdatedName"

            val request = UpdateUserRequest(
                userId = parseUUID(otherUser.id, EntityType.USER),
                firstName = newFirstName,
                lastName = otherUser.lastName,
                email = otherUser.email,
                role = UserRole.fromGrpc(otherUser.role),
                status = fromGrpc(otherUser.status),
            )

            val updatedUser = userService.updateUser(request, listOf("user.first_name"))

            assertEquals(newFirstName, updatedUser.firstName)
        }

        @Test
        fun `When a non-admin user tries to escalate their own role, then an UnauthorizedUpdateException is thrown`() =
            runTest {
                val nonAdminUser = addUser(DataBuilder.createExampleUser(email = "non.admin@example.com"))
                val nonAdminUserId = parseUUID(nonAdminUser.id, EntityType.USER)

                val request = UpdateUserRequest(
                    userId = parseUUID(nonAdminUser.id, EntityType.USER),
                    firstName = nonAdminUser.firstName,
                    lastName = nonAdminUser.lastName,
                    email = nonAdminUser.email,
                    role = UserRole.ADMIN,
                    status = fromGrpc(nonAdminUser.status),
                )

                actAsUser(nonAdminUserId) {
                    assertThrows<UnauthorizedUpdateException> { userService.updateUser(request, listOf("user.role")) }
                }
            }

        @Test
        fun `When a user tries to change their email to one already in use, then a DuplicateUserException is thrown`() =
            runTest {
                val existingUser = addUser(DataBuilder.createExampleUser(email = "existing@example.com"))
                val currentUser = userService.getCurrentUser()

                val request = UpdateUserRequest(
                    userId = parseUUID(currentUser.id, EntityType.USER),
                    firstName = existingUser.firstName,
                    lastName = existingUser.lastName,
                    email = existingUser.email,
                    role = UserRole.fromGrpc(existingUser.role),
                    status = fromGrpc(existingUser.status),
                )

                assertThrows<DuplicateUserException> { userService.updateUser(request, listOf("user.email")) }
            }
    }

    @Nested
    inner class DeleteUser {
        @Test
        fun `When an admin soft-deletes a non-admin user, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            assertDoesNotThrow { userService.softDeleteUser(otherUserId) }
        }

        @Test
        fun `When a user soft-deletes themselves, then the operation succeeds`() = runTest {
            val otherUser = addUser(DataBuilder.createExampleUser(email = "other.user@example.com"))
            val otherUserId = parseUUID(otherUser.id, EntityType.USER)

            actAsUser(otherUserId) {
                assertDoesNotThrow { userService.softDeleteUser(otherUserId) }
            }
        }
    }
}
