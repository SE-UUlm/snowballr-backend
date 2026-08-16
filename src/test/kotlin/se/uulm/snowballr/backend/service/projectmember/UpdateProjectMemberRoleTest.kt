package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.DataBuilder.createExampleUser
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectMemberNotFoundException
import se.uulm.snowballr.backend.model.incoming.projectmember.UpdateProjectMemberRoleRequest
import java.util.UUID

class UpdateProjectMemberRoleTest : ProjectMemberServiceTest() {
    private fun getRequest(userId: UUID, projectId: UUID, newRole: MemberRole = MemberRole.ADMIN) =
        UpdateProjectMemberRoleRequest(projectId = projectId, userId = userId, newRole = newRole)

    @Test
    fun `When a user updates a member role, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(user.id, project.id)

        mockCurrentUser(currentUser)
        coEvery {
            projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateProjectMemberRole(request) }
    }

    @Test
    fun `When a user updates a member role of a non-existent user, then a TestSpecificException is thrown`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(user.id, project.id)

        mockCurrentUser(currentUser)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id) }
        coEvery { userRepoMock.getUserById(user.id) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { service.updateProjectMemberRole(request) }
    }

    @Test
    fun `When retrieving the project member fails, then a FailedPreconditionException is thrown`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()

        val request = getRequest(user.id, project.id)

        mockCurrentUser(currentUser)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id) }
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery {
            projectMemberRepoMock.getProjectMemberByComposedId(project.id, user.id)
        } returns Result.failure(ProjectMemberNotFoundException(user.id, project.id))

        assertThrows<FailedPreconditionException> { service.updateProjectMemberRole(request) }
    }

    @Test
    fun `When a user demotes the last project admin, then a TestSpecificException is thrown`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.ADMIN)

        val request = getRequest(user.id, project.id, MemberRole.DEFAULT)

        mockCurrentUser(currentUser)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id) }
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery {
            projectMemberRepoMock.getProjectMemberByComposedId(project.id, user.id)
        } returns Result.success(projectMember)
        coEvery {
            projectAccessCheckerMock.isNotLastProjectAdmin(user, project.id, any())
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.updateProjectMemberRole(request) }
    }

    @Test
    fun `When a user re-promotes an admin, then last-admin check is skipped`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.ADMIN)

        val request = getRequest(user.id, project.id, MemberRole.ADMIN)

        mockCurrentUser(currentUser)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id) }
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery {
            projectMemberRepoMock.getProjectMemberByComposedId(project.id, user.id)
        } returns Result.success(projectMember)
        coEvery {
            projectMemberRepoMock.updateProjectMemberRole(project.id, user.id, MemberRole.ADMIN)
        } returns projectMember

        service.updateProjectMemberRole(request)

        coVerify(exactly = 0) { projectAccessCheckerMock.isNotLastProjectAdmin(user, project.id, any()) }
    }

    @Test
    fun `When multiple admins exist and one is demoted, then no exception is thrown`() = runTest {
        val currentUser = createExampleUser()
        val user = createExampleUser()
        val project = DataBuilder.createExampleProject()
        val projectMember = DataBuilder.createExampleProjectMember(role = MemberRole.DEFAULT)

        val request = getRequest(user.id, project.id, MemberRole.ADMIN)

        mockCurrentUser(currentUser)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToUpdateMemberRole(currentUser, project.id) }
        coEvery { userRepoMock.getUserById(user.id) } returns Result.success(user)
        coEvery {
            projectMemberRepoMock.getProjectMemberByComposedId(project.id, user.id)
        } returns Result.success(projectMember)
        coEvery {
            projectMemberRepoMock.updateProjectMemberRole(project.id, user.id, MemberRole.ADMIN)
        } returns projectMember

        service.updateProjectMemberRole(request)

        coVerify(exactly = 0) { projectAccessCheckerMock.isNotLastProjectAdmin(user, project.id, any()) }
    }
}
