package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

class GetUserByIdTest : MainServiceTest() {
    companion object {
        private fun isActiveStatus(status: UserStatus) = status == UserStatus.USER_STATUS_ACTIVE ||
            status == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED

        @JvmStatic
        fun inactiveStatuses(): List<UserStatus> =
            UserStatus.entries.filterNot { isActiveStatus(it) || it == UserStatus.UNRECOGNIZED }

        @JvmStatic
        fun activeStatuses(): List<UserStatus> = UserStatus.entries.filter { isActiveStatus(it) }
    }

    @Test
    fun `When current user is admin, then requested user is returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

        assertDoesNotThrow { mainService.getUserById(requestedUser.id) }
    }

    @Test
    fun `When current user requests own user, then user is returned without redundant DB call`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id, status = UserStatus.USER_STATUS_ACTIVE)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

        assertDoesNotThrow { mainService.getUserById(requestedUser.id) }

        // Should not call userRepoMock.getUserById(requestedUserId) again because it's self-request
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUser.id) }
    }

    @Test
    fun `When current user is in same project as requested user, then requested user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coEvery {
            projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id)
        } returns listOf(DataBuilder.createExampleProjectMember(userId = currentUser.id))

        assertDoesNotThrow { mainService.getUserById(requestedUser.id) }
    }

    @Test
    fun `When current user is not authorized to access requested user, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUserId = UUID.randomUUID()

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getUserById(requestedUserId) }
        }

    @ParameterizedTest
    @MethodSource("inactiveStatuses")
    fun `When requested user is inactive, then a NotFoundException is thrown`(status: UserStatus) = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        // We set the requested user's ID to be the same as the current user's ID so that the current user can read the
        // requested user.
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id, status = status)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

        assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
            mainService.getUserById(requestedUser.id)
        }
    }

    @ParameterizedTest
    @MethodSource("activeStatuses")
    fun `When requested user is active, then user is returned`(status: UserStatus) = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(status = status)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

        assertDoesNotThrow("Should succeed for status $status") {
            mainService.getUserById(requestedUser.id)
        }
    }

    @Test
    fun `When retrieving requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUserId = UUID.randomUUID()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getUserById(requestedUserId) }
    }
}
