package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

class GetUserByEmailTest : MainServiceTest() {
    private val exampleEmail = "test@example.com"
    private fun getExampleRequest() = Base.Email.newBuilder().setEmail(exampleEmail).build()

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 0) { userRepoMock.getUserById(any()) }
        coVerify(exactly = 0) { userRepoMock.getUserByEmail(any()) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()

        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUserId) }
        coVerify(exactly = 0) { userRepoMock.getUserByEmail(any()) }
    }

    @Test
    fun `When retrieving requested user by email fails, then exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserByEmail(exampleEmail) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { userRepoMock.getUserByEmail(exampleEmail) }
        coVerify(exactly = 0) { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) }
    }

    @Test
    fun `When verifying user access fails, then UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(email = exampleEmail)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns requestedUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id) } returns emptyList()

        assertThrows<UnauthorizedException.Single> { mainService.getUserByEmail(getExampleRequest()) }

        verify(exactly = 1) { GrpcContext.getUserIdFromContext() }
        coVerify(exactly = 1) { userRepoMock.getUserById(currentUser.id) }
        coVerify(exactly = 1) { userRepoMock.getUserByEmail(exampleEmail) }
        coVerify(exactly = 1) { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id) }
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
            val requestedUser = DataBuilder.createExampleUser(email = exampleEmail, status = status)

            coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns requestedUser

            assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
                mainService.getUserByEmail(getExampleRequest())
            }

            verify(atLeast = 1, atMost = 3) { GrpcContext.getUserIdFromContext() }
            coVerify(atLeast = 1, atMost = 3) { userRepoMock.getUserById(currentUser.id) }
            coVerify(atLeast = 1, atMost = 3) { userRepoMock.getUserByEmail(exampleEmail) }
            coVerify(exactly = 0) { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) }
        }
    }

    @Test
    fun `When all retrievals succeed and user is active, then user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser

        val activeStatuses = listOf(
            UserStatus.USER_STATUS_ACTIVE,
            UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED,
        )

        activeStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(email = exampleEmail, status = status)

            coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns requestedUser

            assertDoesNotThrow("Should succeed for status $status") {
                mainService.getUserByEmail(getExampleRequest())
            }

            verify(atLeast = 1, atMost = 2) { GrpcContext.getUserIdFromContext() }
            coVerify(atLeast = 1, atMost = 2) { userRepoMock.getUserById(currentUser.id) }
            coVerify(atLeast = 1, atMost = 2) { userRepoMock.getUserByEmail(exampleEmail) }
            coVerify(exactly = 0) { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) }
        }
    }
}
