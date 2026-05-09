package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass.ProjectStatus

class GetAllProjectsForUserTest : MainServiceTest() {
    private val statusFilters = setOf(ProjectStatus.PROJECT_STATUS_ACTIVE, ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED)

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(requestedUser.id) }
    }

    @Test
    fun `When a user retrieves another user's projects, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val requestedUser = DataBuilder.createExampleUser()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadUserProjects(currentUser, requestedUser.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { mainService.getAllProjectsForUser(requestedUser.id) }
        }

    @Test
    fun `When a user retrieves another user's projects and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadUserProjects(currentUser, requestedUser.id) }
        coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjectsForUser(requestedUser.id) }
    }
}
