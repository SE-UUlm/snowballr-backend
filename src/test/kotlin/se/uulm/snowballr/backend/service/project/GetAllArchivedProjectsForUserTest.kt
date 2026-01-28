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
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole

class GetAllArchivedProjectsForUserTest : MainServiceTest() {
    private val statusFilters = setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED)

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getAllArchivedProjectsForUser(requestedUser.id) }
    }

    @Test
    fun `When a non-admin retrieves another user's archived projects, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val requestedUser = DataBuilder.createExampleUser()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)

            assertThrows<UnauthorizedException> { mainService.getAllArchivedProjectsForUser(requestedUser.id) }
        }

    @Test
    fun `When archived projects are retrieved by an admin, then they are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns emptyList()

        assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(requestedUser.id) }
    }

    @Test
    fun `When a user retrieves its own archived projects, then they are returned successfully`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser(id = currentUser.id)

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns emptyList()

        assertDoesNotThrow { mainService.getAllArchivedProjectsForUser(requestedUser.id) }
    }
}
