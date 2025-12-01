package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetAllProjectsForUserTest : MainServiceTest() {
    private val requestedUserId = UUID.randomUUID()
    private val statusFilters = setOf(ProjectStatus.PROJECT_STATUS_ACTIVE, ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

    private fun getExampleRequest() = Base.Id.newBuilder().setId(requestedUserId.toString()).build()

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(getExampleRequest()) }
    }

    @Test
    fun `When all user projects are retrieved by another non-admin user, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

            assertThrows<UnauthorizedException> { mainService.getAllProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When the user projects are retrieved by an admin, then all user projects are returned successfully`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val requestedUser = DataBuilder.createExampleUser(id = requestedUserId)

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
            coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns emptyList()

            assertDoesNotThrow { mainService.getAllProjectsForUser(getExampleRequest()) }
        }

    @Test
    fun `When a user retrieves its own projects, then all projects are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id)
        val request = Base.Id.newBuilder().setId(requestedUser.id.toString()).build()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjectsForUser(request) }
    }
}
