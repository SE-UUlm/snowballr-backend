package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus

class GetAllArchivedProjectsForUserTest : ProjectServiceTest() {
    private val statusFilters = setOf(ProjectStatus.ARCHIVED)

    @Test
    fun `When retrieving the requested user fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val requestedUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.getAllArchivedProjectsForUser(requestedUser.id) }
    }

    @Test
    fun `When a user retrieves another user's archived projects, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val requestedUser = DataBuilder.createExampleUser()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadUserProjects(currentUser, requestedUser.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getAllArchivedProjectsForUser(requestedUser.id) }
        }

    @Test
    fun `When a user retrieves another user's archived projects and has access, then the correct values are returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val requestedUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery { userRepoMock.getUserById(requestedUser.id) } returns Result.success(requestedUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadUserProjects(currentUser, requestedUser.id) }
            coEvery { projectRepoMock.getUserProjects(requestedUser.id, statusFilters) } returns listOf(project)

            val result = service.getAllArchivedProjectsForUser(requestedUser.id)

            assertEquals(1, result.size)
            val resultElement = result.first()
            assertProjectEquality(project, resultElement)
        }
}
