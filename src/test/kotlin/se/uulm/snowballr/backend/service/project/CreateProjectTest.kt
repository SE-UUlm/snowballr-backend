package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest

class CreateProjectTest : ProjectServiceTest() {
    private fun getExampleRequest() = CreateProjectRequest(name = "Test Project")

    @Test
    fun `When a project is correctly created, then the created project has the correct values`() = runTest {
        val project = DataBuilder.createExampleProject()
        val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
        val settings = user.settings.defaultProjectSettings

        mockCurrentUser(user)
        coEvery { criterionRepoMock.getAllUserCriteria(user.id) } returns emptyList()
        coEvery {
            projectRepoMock.createProject(getExampleRequest(), user.id, settings, emptyList())
        } returns project

        val result = service.createProject(getExampleRequest())

        assertProjectEquality(project, result)
        coVerify(exactly = 1) {
            projectRepoMock.createProject(getExampleRequest(), user.id, settings, emptyList())
        }
    }

    @Test
    fun `When a project is correctly created and the user has user criteria, then they are passed to be cloned`() =
        runTest {
            val project = DataBuilder.createExampleProject()
            val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
            val settings = user.settings.defaultProjectSettings
            val criteria = listOf(
                DataBuilder.createExampleUserCriterion(tag = "A", createdBy = user.id),
                DataBuilder.createExampleUserCriterion(tag = "B", createdBy = user.id),
            )

            mockCurrentUser(user)
            coEvery { criterionRepoMock.getAllUserCriteria(user.id) } returns criteria
            coEvery {
                projectRepoMock.createProject(getExampleRequest(), user.id, settings, criteria)
            } returns project

            val result = service.createProject(getExampleRequest())

            assertProjectEquality(project, result)
            coVerify(exactly = 1) {
                projectRepoMock.createProject(getExampleRequest(), user.id, settings, criteria)
            }
        }
}
