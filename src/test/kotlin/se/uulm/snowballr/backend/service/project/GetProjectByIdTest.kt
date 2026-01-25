package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectByIdTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(projectId.toString())
        .build()

    @Test
    fun `When the requesting user is not a member of the project, then an UnauthorizedException is thrown`() = runTest {
        val request = getExampleRequest()
        val noAccessUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(noAccessUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectById(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then the project can be retrieved`() = runTest {
        val request = getExampleRequest()
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = projectId)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()

        assertDoesNotThrow { mainService.getProjectById(request) }
    }

    @Test
    fun `When the requesting user is a project member, then the project can be retrieved`() = runTest {
        val request = getExampleRequest()
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = projectId)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)

        assertDoesNotThrow { mainService.getProjectById(request) }
    }

    @Test
    fun `When an error occurs while the project is retrieved, then a ProjectNotFoundException is thrown`() = runTest {
        val request = getExampleRequest()
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

        assertThrows<ProjectNotFoundException> { mainService.getProjectById(request) }
    }

    @Test
    fun `When the project is deleted by a default user, then a ProjectNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val request = getExampleRequest()
        val project = DataBuilder.createExampleProject(id = projectId, status = ProjectStatus.PROJECT_STATUS_DELETED)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<ProjectNotFoundException> { mainService.getProjectById(request) }
    }

    @Test
    fun `When the project is deleted by a server admin, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val request = getExampleRequest()
        val project = DataBuilder.createExampleProject(id = projectId, status = ProjectStatus.PROJECT_STATUS_DELETED)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)

        assertDoesNotThrow { mainService.getProjectById(request) }
    }
}
