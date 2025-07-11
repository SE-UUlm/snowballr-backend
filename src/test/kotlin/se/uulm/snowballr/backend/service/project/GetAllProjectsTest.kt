package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import snowballr.UserOuterClass.UserRole

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
class GetAllProjectsTest : MainServiceTest() {
    @Test
    fun `When all projects are retrieved by an admin, then no exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjects() }
    }

    @Test
    fun `When retrieving the current user fails, then an exception is thrown`() = testCoroutine {
        coEvery { userRepoMock.getUserById(any()) } throws TestSpecificException()
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }

    @Test
    fun `When all projects are retrieved by a non-admin, then an exception is thrown`() = testCoroutine {
        val nonAdminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        coEvery { userRepoMock.getUserById(any()) } returns nonAdminUser
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertThrows<UnauthorizedException.All> { mainService.getAllProjects() }
    }

    @Test
    fun `When retrieving all projects fails, then an exception is thrown`() = testCoroutine {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        coEvery { userRepoMock.getUserById(any()) } returns adminUser
        coEvery { projectRepoMock.getAllProjects() } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }
}
