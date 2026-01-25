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
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectMembersTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(projectId.toString())
        .build()

    @Test
    fun `When a server admin requests the project members, then no exception is thrown`() = runTest {
        val request = getExampleRequest()
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectMemberUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember = DataBuilder.createExampleProjectMember(
            userId = projectMemberUser.id,
            projectId = project.id,
        )
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, projectMemberUser)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a project member requests the project members, then no exception is thrown`() = runTest {
        val request = getExampleRequest()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, user)

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns
            listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a non project member requests the project members, then an UnauthorizedException is thrown`() = runTest {
        val request = getExampleRequest()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When project members are requested from a nonexistent project, then a ProjectNotFoundException is thrown`() =
        runTest {
            val request = getExampleRequest()
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

            mockCurrentUser(user)
            coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

            assertThrows<ProjectNotFoundException> { mainService.getProjectMembers(request) }
        }
}
