package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectByIdTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(projectId.toString())
        .build()

    @Test
    fun `When the requesting user is not a member of the project, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val noAccessUser = DataBuilder.createExampleUser()

        every { GrpcContext.getUserIdFromContext() } returns noAccessUser.id
        coEvery { userRepoMock.getUserById(noAccessUser.id) } returns noAccessUser
        coEvery { projectMemberRepoMock.getProjectMembers(any()) } returns emptyList()

        assertThrows<UnauthorizedException.Single> { mainService.getProjectById(request) }
    }

    @Test
    fun `When the requesting user is a server admin, then the project can be retrieved`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val project = DataBuilder.createExampleProject(id = projectId)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertDoesNotThrow { mainService.getProjectById(request) }
    }

    @Test
    fun `When the requesting user is a project member, then the project can be retrieved`() = runTest {
        val request = getExampleRequest()

        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = projectId)

        every { GrpcContext.getUserIdFromContext() } returns user.id
        coEvery { userRepoMock.getUserById(user.id) } returns user
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertDoesNotThrow { mainService.getProjectById(request) }
    }

    @Test
    fun `When an error occurs while the project is retrieved, then an exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        every { GrpcContext.getUserIdFromContext() } returns adminUser.id
        coEvery { userRepoMock.getUserById(adminUser.id) } returns adminUser
        coEvery { projectMemberRepoMock.getProjectMembers(any()) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.getProjectById(request) }
    }
}
