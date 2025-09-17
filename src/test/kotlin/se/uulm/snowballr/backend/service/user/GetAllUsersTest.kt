package se.uulm.snowballr.backend.service.user

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole

class GetAllUsersTest : MainServiceTest() {
    @Test
    fun `When current user is not admin, then an UnauthorizedException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        mockCurrentUser(currentUser)

        assertThrows<UnauthorizedException> { mainService.getAllUsers() }
    }

    @Test
    fun `When user is admin, then all users are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getAllUsers() } returns emptyList()

        assertDoesNotThrow { mainService.getAllUsers() }
    }
}
