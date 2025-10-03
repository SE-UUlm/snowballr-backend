package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

class GetUserByIdTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When current user is admin, then requested user is returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            status = UserStatus.USER_STATUS_ACTIVE,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)

        assertDoesNotThrow { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user requests own user, then user is returned without redundant DB call`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(
            id = currentUser.id,
            status = UserStatus.USER_STATUS_ACTIVE,
        )
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

        assertDoesNotThrow { mainService.getUserById(request) }

        // Should not call userRepoMock.getUserById(requestedUserId) again because it's self-request
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUser.id) }
    }

    @Test
    fun `When current user is in same project as requested user, then requested user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            status = UserStatus.USER_STATUS_ACTIVE,
        )

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns listOf(
            DataBuilder.createExampleProjectMember(userId = currentUser.id),
        )

        assertDoesNotThrow { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user is not authorized to access requested user, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getUserById(getExampleRequest()) }
        }

    @Test
    fun `When requested user is inactive, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(id = requestedUserId)

        mockCurrentUser(currentUser)

        val inactiveStatuses = UserStatus.entries.filterNot {
            it == UserStatus.USER_STATUS_ACTIVE || it == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        }

        inactiveStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId, status = status)

            coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)

            assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
                mainService.getUserById(getExampleRequest())
            }
        }
    }

    @Test
    fun `When requested user is active, then user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)

        val activeStatuses = listOf(
            UserStatus.USER_STATUS_ACTIVE,
            UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED,
        )

        activeStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId, status = status)

            coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.success(requestedUser)

            assertDoesNotThrow("Should succeed for status $status") {
                mainService.getUserById(getExampleRequest())
            }
        }
    }

    @Test
    fun `When retrieving requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUserId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getUserById(getExampleRequest()) }
    }
}
