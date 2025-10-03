package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
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

class GetUserByEmailTest : MainServiceTest() {
    private val exampleEmail = "test@example.com"
    private fun getExampleRequest() = Base.Email.newBuilder().setEmail(exampleEmail).build()

    @Test
    fun `When retrieving requested user by email fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getUserByEmail(getExampleRequest()) }
    }

    @Test
    fun `When verifying user access fails, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val requestedUser = DataBuilder.createExampleUser(email = exampleEmail)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns Result.success(requestedUser)
        coEvery { projectMemberRepoMock.getMembersInSameProjectsAsUser(requestedUser.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getUserByEmail(getExampleRequest()) }
    }

    @Test
    fun `When requested user is inactive, then a NotFoundException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(email = exampleEmail)

        mockCurrentUser(currentUser)

        val inactiveStatuses = UserStatus.entries.filterNot {
            it == UserStatus.USER_STATUS_ACTIVE || it == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        }

        inactiveStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(
                id = currentUser.id,
                email = exampleEmail,
                status = status,
            )

            coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns Result.success(requestedUser)

            assertThrows<NotFoundException>("Should throw NotFoundException for status $status") {
                mainService.getUserByEmail(getExampleRequest())
            }
        }
    }

    @Test
    fun `When all retrievals succeed and user is active, then user is returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)

        val activeStatuses = listOf(
            UserStatus.USER_STATUS_ACTIVE,
            UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED,
        )

        activeStatuses.forEach { status ->
            val requestedUser = DataBuilder.createExampleUser(email = exampleEmail, status = status)

            coEvery { userRepoMock.getUserByEmail(exampleEmail) } returns Result.success(requestedUser)

            assertDoesNotThrow("Should succeed for status $status") {
                mainService.getUserByEmail(getExampleRequest())
            }
        }
    }
}
