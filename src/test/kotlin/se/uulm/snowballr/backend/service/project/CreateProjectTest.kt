package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.test.assertEquals
import snowballr.ProjectOuterClass.Project as GrpcProject

class CreateProjectTest : MainServiceTest() {
    private fun getExampleRequest() = GrpcProject.Create.getDefaultInstance()

    @Test
    fun `When a project is correctly created, then no exception is thrown`() = runTest {
        val project = DataBuilder.createExampleProject()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val userSettings = DataBuilder.createExampleUserSettings()

        mockCurrentUser(user)
        coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
        coEvery { criterionRepoMock.getCriteriaByIds(emptyList()) } returns emptyList()
        coEvery { projectRepoMock.createProject(any(), any(), userSettings) } returns project
        coEvery { projectMemberRepoMock.addUserToProject(user.id, project.id) } returns
            mockk<ProjectMember>()
        coEvery { projectMemberRepoMock.promoteProjectMemberToAdmin(project.id, user.id) } returns
            mockk<ProjectMember>()

        assertDoesNotThrow { mainService.createProject(getExampleRequest()) }
        coVerify(exactly = 0) { criterionRepoMock.createCriterion(any(), any()) }
    }

    @Test
    fun `When a project is correctly created and the user has default criteria, then no exception is thrown`() =
        runTest {
            val project = DataBuilder.createExampleProject()
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val criterion = DataBuilder.createExampleProjectCriterion()
            val userSettings = DataBuilder.createExampleUserSettings(criteriaIds = listOf(criterion.id))
            val criteriaIdsSlot = slot<List<UUID>>()

            mockCurrentUser(user)
            coEvery { userRepoMock.getUserSettings(user.id) } returns Result.success(userSettings)
            coEvery { criterionRepoMock.getCriteriaByIds(capture(criteriaIdsSlot)) } returns listOf(criterion)
            coEvery { projectRepoMock.createProject(any(), user.id, userSettings) } returns project
            coEvery { criterionRepoMock.createCriterion(any(), user.id) } returns criterion
            coEvery { projectMemberRepoMock.addUserToProject(user.id, project.id) } returns
                mockk<ProjectMember>()
            coEvery { projectMemberRepoMock.promoteProjectMemberToAdmin(project.id, user.id) } returns
                mockk<ProjectMember>()

            assertDoesNotThrow { mainService.createProject(getExampleRequest()) }
            assertEquals(listOf(criterion.id), criteriaIdsSlot.captured)
        }
}
