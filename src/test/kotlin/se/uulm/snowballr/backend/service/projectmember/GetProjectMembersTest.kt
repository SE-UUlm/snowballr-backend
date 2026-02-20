package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectMembersTest : MainServiceTest() {
    @Test
    fun `When a server admin requests the project members, then no exception is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectMemberUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(
            userId = projectMemberUser.id,
            projectId = project.id,
        )
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, projectMemberUser)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, adminUser.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(project.id) }
    }

    @Test
    fun `When a project member requests the project members, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, user)

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns true
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(project.id) }
    }

    @Test
    fun `When a non project member requests the project members, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns false

        assertThrows<UnauthorizedException> { mainService.getProjectMembers(project.id) }
    }

    @Test
    fun `When project members are requested from a nonexistent project, then a ProjectNotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val projectId = UUID.randomUUID()

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

            assertThrows<ProjectNotFoundException> { mainService.getProjectMembers(projectId) }
        }
}
