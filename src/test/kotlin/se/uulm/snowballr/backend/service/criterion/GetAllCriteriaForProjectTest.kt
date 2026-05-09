package se.uulm.snowballr.backend.service.criterion

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException

class GetAllCriteriaForProjectTest : CriterionServiceTest() {
    @Test
    fun `When a user retrieves all criteria for a project, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(user)
            coEvery { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getAllCriteriaForProject(project.id) }
        }

    @Test
    fun `When a user retrieves all criteria for a project and has access, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val criterion = DataBuilder.createExampleProjectCriterion(projectId = project.id, createdBy = user.id)

        mockCurrentUser(user)
        coJustRun { projectAccessCheckerMock.isAllowedToReadProject(user, project.id) }
        coEvery { criterionRepoMock.getAllProjectCriteria(project.id) } returns listOf(criterion)

        assertDoesNotThrow { service.getAllCriteriaForProject(project.id) }
    }
}
