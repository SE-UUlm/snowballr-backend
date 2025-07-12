package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.BeforeEach
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
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetUserByIdTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } throws NotImplementedError()
    }

    @Test
    fun `When parsing the user ID fails, then InvalidIdException is thrown`() = testCoroutine {
        val request = Base.Id.newBuilder().setId("invalid-uuid").build()
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } returns DataBuilder.createExampleUser()

        assertThrows<InvalidIdException> { mainService.getUserById(request) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(getExampleRequest()) }
    }

    @Test
    fun `When current user is admin, then requested user is returned successfully`() = testCoroutine {
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
    fun `When current user requests own user, then user is returned without redundant DB call`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(
            id = currentUser.id,
            status = UserStatus.USER_STATUS_ACTIVE,
        )
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns requestedUser

        assertDoesNotThrow { mainService.getUserById(request) }

        // Should not call userRepoMock.getUserById(requestedUserId) again because it's self-request
        coVerify(exactly = 1) { userRepoMock.getUserById(requestedUser.id) }
    }

    @Test
    fun `When current user is in same project as requested user, then requested user is returned`() = testCoroutine {
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
        testCoroutine {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            every { GrpcContext.getUserIdFromContext() } returns currentUser.id
            coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
            coEvery { userRepoMock.getUserById(requestedUserId) } returns requestedUser
            coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUserId) } returns emptyList()

            assertThrows<UnauthorizedException.Single> { mainService.getUserById(getExampleRequest()) }
        }

    @Test
    fun `When requested user is inactive, then NotFoundException is thrown`() = testCoroutine {
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
    fun `When requested user is active, then user is returned`() = testCoroutine {
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
    fun `When retrieving requested user fails, then exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserById(requestedUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserById(getExampleRequest()) }
    }
}
