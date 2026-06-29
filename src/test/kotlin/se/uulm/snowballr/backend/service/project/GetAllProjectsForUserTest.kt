package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import kotlin.test.assertEquals

class GetAllProjectsForUserTest : ProjectServiceTest() {
    private val statusFilters = setOf(ProjectStatus.ACTIVE, ProjectStatus.ACTIVE_LOCKED)

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getAllProjectsForUser(requestedUser.id) }
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

            assertThrows<TestSpecificException> { service.getAllProjectsForUser(requestedUser.id) }
        }

    @Test
    fun `When a user retrieves another user's projects and has access, then the correct values are returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val requestedUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadUserProjects(currentUser, requestedUser.id) }
            coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns listOf(project)

            val result = service.getAllProjectsForUser(requestedUser.id)

            assertEquals(1, result.projectsCount)
            val resultElement = result.projectsList.first()
            assertProjectEquality(project, resultElement)
        }
}
