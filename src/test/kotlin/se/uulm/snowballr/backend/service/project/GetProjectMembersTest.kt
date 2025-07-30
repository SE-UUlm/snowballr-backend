package se.uulm.snowballr.backend.service.project

import io.mockk.coEvery
import io.mockk.every
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class GetProjectMembersTest : MainServiceTest() {
    private val requestId = UUID.randomUUID()
    private val dummyUserUUID = UUID.randomUUID()

    private fun getExampleRequest() = Base.Id
        .newBuilder()
        .setId(requestId.toString())
        .build()

    @Test
    fun `When a server admin requests the project members, then no exception is thrown`() = runTest {
        val request = getExampleRequest()

        val adminUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val projectMemberUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(
            userId = projectMemberUser.id,
            projectId = project.id,
        )
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, projectMemberUser)

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns adminUser
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns
            listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a project member requests the project members, then no exception is thrown`() = runTest {
        val request = getExampleRequest()

        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = requestId)
        val projectMember = DataBuilder.createExampleProjectMember(userId = user.id, projectId = project.id)
        val projectMemberWithUser = ProjectMemberWithUser(projectMember, user)

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns
            listOf(projectMemberWithUser)

        assertDoesNotThrow { mainService.getProjectMembers(request) }
    }

    @Test
    fun `When a non project member requests the project members, then an unauthorized exception is thrown`() = runTest {
        val request = getExampleRequest()

        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject(id = requestId)

        every { GrpcContext.getUserIdFromContext() } returns dummyUserUUID
        coEvery { userRepoMock.getUserById(dummyUserUUID) } returns user
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.getProjectMembers(request) }
    }
}
