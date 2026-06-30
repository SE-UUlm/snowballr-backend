package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import kotlin.test.assertEquals

class UpdateUserTest : UserServiceTest() {
    private fun getExampleRequest(user: User) = UpdateUserRequest(
        userId = user.id,
        firstName = user.firstName,
        lastName = user.lastName,
        email = user.email,
        role = user.role,
        status = user.status,
    )

    @Test
    fun `When retrieving user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getExampleRequest(otherUser)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.updateUser(request, emptyList()) }
    }

    @Test
    fun `When a user updates another user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getExampleRequest(otherUser)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coEvery {
            userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateUser(request, emptyList()) }
    }

    @Test
    fun `When a user tries to change another user's role, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val otherUser = DataBuilder.createExampleUser()

            val request = getExampleRequest(otherUser)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
            coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
            coEvery {
                userAccessCheckerMock.isAllowedToUpdateUserRole(currentUser, otherUser.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.updateUser(request, listOf("user.role")) }
        }

    @Test
    fun `When a user tries to change another user's email to an existent email, then a DuplicateUserException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val otherUser = DataBuilder.createExampleUser(email = "other@user.com")

            val request = getExampleRequest(otherUser)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
            coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
            coEvery { userRepoMock.doesUserExistByEmail(otherUser.email) } returns true

            assertThrows<DuplicateUserException> { service.updateUser(request, listOf("user.email")) }
        }

    @Test
    fun `When admin updates the email of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser(email = "other@user.com")

        val request = getExampleRequest(otherUser)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
        coEvery { userRepoMock.doesUserExistByEmail(otherUser.email) } returns false
        coEvery { userRepoMock.updateUser(request, listOf("user.email")) } returns otherUser

        val updatedUser = service.updateUser(request, listOf("user.email"))

        assertEquals(otherUser.id, updatedUser.id)
        assertEquals(otherUser.email, updatedUser.email)
    }

    @Test
    fun `When admin updates all other fields of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getExampleRequest(otherUser)
        val paths = listOf("user.first_name", "user.last_name", "user.role", "user.status")

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUserRole(currentUser, otherUser.id) }
        coEvery { userRepoMock.updateUser(request, paths) } returns otherUser

        val updatedUser = service.updateUser(request, paths)

        assertEquals(otherUser.id, updatedUser.id)
        assertEquals(otherUser.firstName, updatedUser.firstName)
        assertEquals(otherUser.lastName, updatedUser.lastName)
        assertEquals(otherUser.role, updatedUser.role)
        assertEquals(otherUser.status, updatedUser.status)
    }

    @Test
    fun `When a non-admin user tries to change their own role, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        val request = getExampleRequest(currentUser)

        mockCurrentUser(currentUser)
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, currentUser) }
        coEvery {
            userAccessCheckerMock.isAllowedToUpdateUserRole(currentUser, currentUser.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateUser(request, listOf("user.role")) }

        coVerify(exactly = 0) { userRepoMock.updateUser(any(), any()) }
    }

    @Test
    fun `When a user tries to change their own email to an existent email, then a DuplicateUserException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()

            val request = getExampleRequest(currentUser)

            mockCurrentUser(currentUser)
            coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, currentUser) }
            coEvery { userRepoMock.doesUserExistByEmail(currentUser.email) } returns true

            assertThrows<DuplicateUserException> { service.updateUser(request, listOf("user.email")) }

            coVerify(exactly = 0) { userRepoMock.updateUser(any(), any()) }
        }
}
