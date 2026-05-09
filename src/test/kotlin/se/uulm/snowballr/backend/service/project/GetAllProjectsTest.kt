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

class GetAllProjectsTest : MainServiceTest() {
    @Test
    fun `When a user retrieves all projects, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coEvery { projectAccessCheckerMock.isAllowedToReadAllProjects(currentUser) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getAllProjects() }
    }

    @Test
    fun `When a user retrieves all projects and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()

        mockCurrentUser(currentUser)
        coJustRun { projectAccessCheckerMock.isAllowedToReadAllProjects(currentUser) }
        coEvery { projectRepoMock.getAllProjects() } returns emptyList()

        assertDoesNotThrow { mainService.getAllProjects() }
    }
}
