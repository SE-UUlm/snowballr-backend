package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
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

class GetUserByEmailTest : MainServiceTest() {
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
    fun `When retrieving requested user by email fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(currentUser.email) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getUserByEmail(currentUser.email) }
    }

    @Test
    fun `When verifying user access fails, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUser.email) } returns Result.success(requestedUser)
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getUserByEmail(requestedUser.email) }
    }

    @ParameterizedTest
    @MethodSource("inactiveStatuses")
    fun `When requested user is inactive, then a NotFoundException is thrown`(status: UserStatus) = runTest {
        val currentUser = DataBuilder.createExampleUser()
        // We set the requested user's email to be the same as the current user's email so that the current user can
        // read the requested user.
        val requestedUser = DataBuilder.createExampleUser(
            id = currentUser.id,
            email = currentUser.email,
            status = status,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUser.email) } returns Result.success(requestedUser)

        assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
            mainService.getUserByEmail(requestedUser.email)
        }
    }

    @ParameterizedTest
    @MethodSource("activeStatuses")
    fun `When all retrievals succeed and user is active, then user is returned`(status: UserStatus) = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(status = status)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(requestedUser.email) } returns Result.success(requestedUser)

        assertDoesNotThrow("Should succeed for status $status") {
            mainService.getUserByEmail(requestedUser.email)
        }
    }
}
