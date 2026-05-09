package se.uulm.snowballr.backend.service.user

import com.google.protobuf.util.FieldMaskUtil
import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import kotlin.test.assertEquals
import snowballr.UserOuterClass.User as GrpcUser

class UpdateUserTest : MainServiceTest() {
    private fun getRequest(user: User, paths: List<String> = emptyList()) = GrpcUser.Update.newBuilder()
        .setUser(user.toGrpcUser())
        .setMask(FieldMaskUtil.fromStringList(paths))
        .build()

    @Test
    fun `When retrieving user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getRequest(otherUser)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.updateUser(request) }
    }

    @Test
    fun `When a user updates another user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getRequest(otherUser)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coEvery {
            userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.updateUser(request) }
    }

    @Test
    fun `When a user tries to change another user's role, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val otherUser = DataBuilder.createExampleUser()

            val request = getRequest(otherUser, listOf("role"))

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
            coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
            coEvery {
                userAccessCheckerMock.isAllowedToUpdateUserRole(currentUser, otherUser.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.updateUser(request) }
        }

    @Test
    fun `When a user tries to change another user's email to an existent email, then a DuplicateUserException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val otherUser = DataBuilder.createExampleUser(email = "other@user.com")

            val request = getRequest(otherUser, listOf("email"))

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
            coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
            coEvery { userRepoMock.doesUserExistByEmail(otherUser.email) } returns true

            assertThrows<DuplicateUserException> { mainService.updateUser(request) }
        }

    @Test
    fun `When admin updates the email of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser(email = "other@user.com")

        val request = getRequest(otherUser, listOf("email"))

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
        coEvery { userRepoMock.doesUserExistByEmail(otherUser.email) } returns false
        coEvery { userRepoMock.updateUser(request) } returns otherUser

        val updatedUser = mainService.updateUser(request)

        assertEquals(otherUser.id.toString(), updatedUser.id)
        assertEquals(otherUser.email, updatedUser.email)
    }

    @Test
    fun `When admin updates all other fields of another user, then it succeeds`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val otherUser = DataBuilder.createExampleUser()

        val request = getRequest(otherUser, listOf("first_name", "last_name", "role", "status"))

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(otherUser.id) } returns Result.success(otherUser)
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUser(currentUser, otherUser) }
        coJustRun { userAccessCheckerMock.isAllowedToUpdateUserRole(currentUser, otherUser.id) }
        coEvery { userRepoMock.updateUser(request) } returns otherUser

        val updatedUser = mainService.updateUser(request)

        assertEquals(otherUser.id.toString(), updatedUser.id)
        assertEquals(otherUser.firstName, updatedUser.firstName)
        assertEquals(otherUser.lastName, updatedUser.lastName)
        assertEquals(UserRole.valueOf(otherUser.role.name), updatedUser.role)
        assertEquals(UserStatus.valueOf(otherUser.status.name), updatedUser.status)
    }
}
