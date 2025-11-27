package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole

class GetAllProjectsTest : MainServiceTest() {
    @Test
    fun `When all projects are retrieved by a non-admin, then an UnauthorizedException is thrown`() = runTest {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        mockCurrentUser(nonAdminUser)

        assertThrows<UnauthorizedException> { mainService.getAllProjects() }
    }

    @Test
    fun `When all projects are retrieved by an admin, then no exception is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjects() }
    }
}
