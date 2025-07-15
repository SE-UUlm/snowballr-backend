package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
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
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

@DelicateCoroutinesApi
@ExperimentalCoroutinesApi
class GetUserByEmailTest : MainServiceTest() {
    private val exampleEmail = "test@example.com"
    private fun getExampleRequest() = Base.Email.newBuilder().setEmail(exampleEmail).build()

    @BeforeEach
    fun setupTest() {
        every { GrpcContext.getUserIdFromContext() } throws NotImplementedError()
        coEvery { userRepoMock.getUserById(any()) } throws NotImplementedError()
        coEvery { userRepoMock.getUserByEmail(any()) } throws NotImplementedError()
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(any()) } throws NotImplementedError()
    }

    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }
    }

    @Test
    fun `When retrieving current user fails, then exception is thrown`() = testCoroutine {
        every { GrpcContext.getUserIdFromContext() } returns UUID.randomUUID()
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }
    }

    @Test
    fun `When retrieving requested user by email fails, then exception is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserByEmail(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }
    }

    @Test
    fun `When verifying user access fails, then UnauthorizedException is thrown`() = testCoroutine {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(email = exampleEmail)

        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns currentUser
        coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns requestedUser
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id) } returns emptyList()

        assertThrows<UnauthorizedException.Single> { mainService.getUserByEmail(getExampleRequest()) }
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
            val requestedUser = DataBuilder.createExampleUser(email = exampleEmail, status = status)

            coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns requestedUser

            assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
                mainService.getUserByEmail(getExampleRequest())
            }
        }
    }

    @Test
    fun `When all retrievals succeed and user is active, then user is returned`() = testCoroutine {
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
        }
    }
}
