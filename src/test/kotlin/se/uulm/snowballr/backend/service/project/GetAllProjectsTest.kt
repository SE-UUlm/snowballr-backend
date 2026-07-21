package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException

class GetAllProjectsTest : ProjectServiceTest() {
    @Test
    fun `When a user retrieves all projects, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { projectAccessCheckerMock.isAllowedToReadAllProjects(currentUser) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.getAllProjects() }
    }

    @Test
    fun `When a user retrieves all projects and has access, then the correct values are returned`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadAllProjects(currentUser) }
        coEvery { projectRepoMock.getAllProjects() } returns listOf(project)

        val result = service.getAllProjects()

        assertEquals(1, result.size)
        val resultElement = result.first()
        assertProjectEquality(project, resultElement)
    }
}
