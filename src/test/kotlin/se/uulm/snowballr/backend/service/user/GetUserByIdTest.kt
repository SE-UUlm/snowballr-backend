package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
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
    fun `When parsing the user ID fails, then InvalidIdException is thrown`() = runTest {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()
        val currentUserId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } returns DataBuilder.createExampleUser()

        assertThrows<InvalidIdException> { mainService.getUserById(request) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user is admin, then requested user is returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser(
            id = requestedUserId,
            status = UserStatus.USER_STATUS_ACTIVE,
        )

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser

        assertDoesNotThrow { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user requests own user, then user is returned without redundant DB call`() = runTest {
        val currentUser = DataBuilder.createExampleUser(status = UserStatus.USER_STATUS_ACTIVE)
        val request = Base.Id.newBuilder().setId(currentUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

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

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns listOf(
            DataBuilder.createExampleProjectMember(userId = currentUser.id),
        )

        assertDoesNotThrow { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user is not authorized to access requested user, then UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns emptyList()

            assertThrows<UnauthorizedException.Single> { mainService.getUserById(getExampleRequest()) }
        }

    @Test
    fun `When requested user is inactive, then NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        val inactiveStatuses = UserStatus.entries.filterNot {
            it == UserStatus.USER_STATUS_ACTIVE || it == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        }

        inactiveStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId, status = status)

            coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser

            assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
                mainService.getUserById(getExampleRequest())
            }
        }
    }

    @Test
    fun `When requested user is active, then user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        val activeStatuses = listOf(
            UserStatus.USER_STATUS_ACTIVE,
            UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED,
        )

        activeStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId, status = status)

            coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser

            assertDoesNotThrow("Should succeed for status $status") {
                mainService.getUserById(getExampleRequest())
            }
        }
    }

    @Test
    fun `When retrieving requested user fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(getExampleRequest()) }
    }
}
