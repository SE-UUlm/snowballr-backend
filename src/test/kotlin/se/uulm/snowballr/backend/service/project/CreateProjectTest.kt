package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import java.util.UUID

class CreateProjectTest : ProjectServiceTest() {
    private fun getExampleRequest() = CreateProjectRequest(name = "Test Project")

    private fun mockProjectAdminCreation(project: Project, user: User) {
        val projectMember = DataBuilder.createExampleProjectMember(project.id, user.id, MemberRole.DEFAULT)
        val projectAdmin = DataBuilder.createExampleProjectMember(project.id, user.id, MemberRole.ADMIN)

        coEvery { projectMemberRepoMock.addUserToProject(user.id, project.id) } returns projectMember
        coEvery {
            projectMemberRepoMock.updateProjectMemberRole(project.id, user.id, MemberRole.ADMIN)
        } returns projectAdmin
    }

    @Test
    fun `When a project is correctly created, then the created project has the correct values`() = runTest {
        val project = DataBuilder.createExampleProject()
        val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
        val userSettings = DataBuilder.createExampleUserSettings()

        mockCurrentUser(user)
        coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
        coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
        coEvery { projectRepoMock.createProject(getExampleRequest(), user.id, userSettings) } returns project
        mockProjectAdminCreation(project, user)

        val result = service.createProject(getExampleRequest())

        assertProjectEquality(project, result)

        coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), user.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.addUserToProject(user.id, project.id) }
        coVerify(exactly = 1) {
            projectMemberRepoMock.updateProjectMemberRole(project.id, user.id, MemberRole.ADMIN)
        }
    }

    @Test
    fun `When a project is correctly created and the user has default criteria, then the created project has the correct values`() =
        runTest {
            val project = DataBuilder.createExampleProject()
            val user = DataBuilder.createExampleUser(role = UserRole.DEFAULT)
            val criterion = DataBuilder.createExampleProjectCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))
            val criteriaIdsSlot = slot<List<UUID>>()

            val criterionCreateRequest = CreateCriterionRequest(
                tag = criterion.tag,
                name = criterion.name,
                description = criterion.description,
                category = criterion.category,
                projectId = project.id,
            )

            mockCurrentUser(user)
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
            coEvery { criterionRepoMock.getCriteriaByIds(capture(criteriaIdsSlot)) } returns listOf(criterion)
            coEvery { projectRepoMock.createProject(getExampleRequest(), user.id, userSettings) } returns project
            coEvery { criterionRepoMock.createCriterion(criterionCreateRequest, user.id) } returns criterion
            mockProjectAdminCreation(project, user)

            val result = service.createProject(getExampleRequest())

            assertProjectEquality(project, result)
            assertEquals(listOf(criterion.id), criteriaIdsSlot.captured)
        }
}
