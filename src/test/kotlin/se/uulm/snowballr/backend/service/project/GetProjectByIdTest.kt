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
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectByIdTest : MainServiceTest() {
    @Test
    fun `When the requesting user is not a member of the project, then an UnauthorizedException is thrown`() = runTest {
        val noAccessUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(noAccessUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, noAccessUser.id) } returns false

        assertThrows<UnauthorizedException> { mainService.getProjectById(project.id) }
    }

    @Test
    fun `When the requesting user is a server admin, then the project can be retrieved`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, adminUser.id) } returns false

        assertDoesNotThrow { mainService.getProjectById(project.id) }
    }

    @Test
    fun `When the requesting user is a project member, then the project can be retrieved`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns true

        assertDoesNotThrow { mainService.getProjectById(project.id) }
    }

    @Test
    fun `When an error occurs while the project is retrieved, then a ProjectNotFoundException is thrown`() = runTest {
        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectId = UUID.randomUUID()

        mockCurrentUser(adminUser)
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

        assertThrows<ProjectNotFoundException> { mainService.getProjectById(projectId) }
    }

    @Test
    fun `When the project is deleted by a default user, then a ProjectNotFoundException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_DELETED)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<ProjectNotFoundException> { mainService.getProjectById(project.id) }
    }

    @Test
    fun `When the project is deleted by a server admin, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(status = ProjectStatus.PROJECT_STATUS_DELETED)

        mockCurrentUser(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, user.id) } returns true

        assertDoesNotThrow { mainService.getProjectById(project.id) }
    }
}
