package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
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

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns Result.success(adminUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns
            listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a project member requests the project members, then no exception is thrown`() = runTest {
        val request = getExampleRequest()

        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, user)

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns
            listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a non project member requests the project members, then an unauthorized exception is thrown`() = runTest {
        val request = getExampleRequest()

        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When project members are request from a non existing project, then a not found exception is thrown`() =
        runTest {
            val request = getExampleRequest()

            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            every { GrpcContext.getUserIdFromContext() } returns user.id
            coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
            coEvery {
                projectRepoMock.getProjectById(projectId)
            } throws SnowballRException.NotFoundException(EntityType.PROJECT, request.id)

            assertThrows<SnowballRException.NotFoundException> { mainService.getProjectMembers(request) }
        }
}
