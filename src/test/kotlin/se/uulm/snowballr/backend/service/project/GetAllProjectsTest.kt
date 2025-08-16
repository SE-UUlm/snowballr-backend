package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetAllProjectsTest : MainServiceTest() {
    @Test
    fun `When retrieving the current user ID fails, then an exception is thrown`() = runTest {
        every { GrpcContext.getUserIdFromContext() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = runTest {
        val currentUserId = UUID.randomUUID()
        every { GrpcContext.getUserIdFromContext() } returns currentUserId
        coEvery { userRepoMock.getUserById(currentUserId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }

    @Test
    fun `When all projects are retrieved by a non-admin, then an exception is thrown`() = runTest {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        every { GrpcContext.getUserIdFromContext() } returns nonAdminUser.id
        coEvery { userRepoMock.getUserById(nonAdminUser.id) } returns nonAdminUser

        assertThrows<UnauthorizedException.All> { mainService.getAllProjects() }
    }

    @Test
    fun `When retrieving all projects fails, then an exception is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getAllProjects() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }

    @Test
    fun `When all projects are retrieved by an admin, then no exception is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjects() }
    }
}
