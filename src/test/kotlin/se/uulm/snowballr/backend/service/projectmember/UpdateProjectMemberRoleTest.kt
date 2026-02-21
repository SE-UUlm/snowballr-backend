package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectMemberNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.MemberRole
import snowballr.UserOuterClass.UserRole
import java.util.UUID

class UpdateProjectMemberRoleTest : MainServiceTest() {
    @Test
    fun `When a server admin updates a member role of a user in a non-existent project, then a ProjectNotFoundException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val nonExistentProjectId = UUID.randomUUID()

            val request = ProjectOuterClass.Project.Member.Update
                .newBuilder()
                .setProjectId(nonExistentProjectId.toString())
                .setUserId(userToBeUpdated.id.toString())
                .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
                .build()

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(nonExistentProjectId) } returns emptyList()
            coEvery {
                projectRepoMock.getProjectById(nonExistentProjectId)
            } returns Result.failure(TestSpecificException())

            assertThrows<ProjectNotFoundException> { mainService.updateProjectMemberRole(request) }
        }

    @Test
    fun `When a server admin updates a member role of a non-existent user, then a TestSpecificException is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val nonExistentUserId = UUID.randomUUID()
            val project = DataBuilder.createExampleProject()

            val request = ProjectOuterClass.Project.Member.Update
                .newBuilder()
                .setProjectId(project.id.toString())
                .setUserId(nonExistentUserId.toString())
                .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
                .build()

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { userRepoMock.getUserById(nonExistentUserId) } returns
                Result.failure(TestSpecificException())

            assertThrows<TestSpecificException> { mainService.updateProjectMemberRole(request) }
        }

    @Test
    fun `When a server admin updates another member's role, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMemberToBeUpdated =
            DataBuilder.createExampleProjectMember(userId = userToBeUpdated.id, projectId = project.id)

        val newRole = MemberRole.MEMBER_ROLE_ADMIN
        val updatedUser = DataBuilder.createExampleProjectMember(
            userId = userToBeUpdated.id,
            projectId = project.id,
            role = newRole,
        )

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(userToBeUpdated.id.toString())
            .setNewRole(newRole)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { userRepoMock.getUserById(userToBeUpdated.id) } returns Result.success(userToBeUpdated)
        coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, userToBeUpdated.id) } returns
            Result.success(projectMemberToBeUpdated)
        coEvery { projectMemberRepoMock.updateProjectMemberRole(project.id, userToBeUpdated.id, newRole) } returns
            updatedUser

        assertDoesNotThrow { mainService.updateProjectMemberRole(request) }
    }

    @Test
    fun `When a project admin updates another member's role, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val projectAdminMember = DataBuilder.createExampleProjectMember(
            role = MemberRole.MEMBER_ROLE_ADMIN,
            userId = user.id,
        )
        val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectMemberToBeUpdated = DataBuilder.createExampleProjectMember(
            userId = userToBeUpdated.id,
            projectId = project.id,
        )

        val newRole = MemberRole.MEMBER_ROLE_ADMIN
        val updatedUser = DataBuilder.createExampleProjectMember(
            userId = userToBeUpdated.id,
            projectId = project.id,
            role = newRole,
        )

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(userToBeUpdated.id.toString())
            .setNewRole(newRole)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdminMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { userRepoMock.getUserById(userToBeUpdated.id) } returns Result.success(userToBeUpdated)
        coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, userToBeUpdated.id) } returns
            Result.success(projectMemberToBeUpdated)
        coEvery { projectMemberRepoMock.updateProjectMemberRole(project.id, userToBeUpdated.id, newRole) } returns
            updatedUser

        assertDoesNotThrow { mainService.updateProjectMemberRole(request) }
    }

    @Test
    fun `When a project member updates another member's role, then an UnauthorizedException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(userToBeUpdated.id.toString())
            .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()

        assertThrows<UnauthorizedException> { mainService.updateProjectMemberRole(request) }
    }

    @Test
    fun `When an admin updates the member role of a user who is not a member of the project, then a FailedPrecondition is thrown`() =
        runTest {
            val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val project = DataBuilder.createExampleProject()

            val request = ProjectOuterClass.Project.Member.Update
                .newBuilder()
                .setProjectId(project.id.toString())
                .setUserId(userToBeUpdated.id.toString())
                .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
                .build()

            mockCurrentUser(user)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
            coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
            coEvery { userRepoMock.getUserById(userToBeUpdated.id) } returns Result.success(userToBeUpdated)
            coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, userToBeUpdated.id) } returns
                Result.failure(ProjectMemberNotFoundException(user.id, project.id))

            assertThrows<FailedPreconditionException> { mainService.updateProjectMemberRole(request) }
        }

    @Test
    fun `When a server admin demotes the last project admin, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectAdminMember = DataBuilder.createExampleProjectMember(
            role = MemberRole.MEMBER_ROLE_ADMIN,
            userId = userToBeUpdated.id,
        )

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(userToBeUpdated.id.toString())
            .setNewRole(MemberRole.MEMBER_ROLE_DEFAULT)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdminMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { userRepoMock.getUserById(userToBeUpdated.id) } returns Result.success(userToBeUpdated)
        coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, userToBeUpdated.id) } returns
            Result.success(projectAdminMember)

        assertThrows<FailedPreconditionException> { mainService.updateProjectMemberRole(request) }
    }

    @Test
    fun `When multiple admins exist and one is demoted, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val user1ToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val user2ToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectAdminMember1 = DataBuilder.createExampleProjectMember(
            role = MemberRole.MEMBER_ROLE_ADMIN,
            userId = user1ToBeUpdated.id,
        )
        val projectAdminMember2 = DataBuilder.createExampleProjectMember(
            role = MemberRole.MEMBER_ROLE_ADMIN,
            userId = user2ToBeUpdated.id,
        )

        val newRole = MemberRole.MEMBER_ROLE_DEFAULT
        val updatedUser = DataBuilder.createExampleProjectMember(
            userId = user1ToBeUpdated.id,
            projectId = project.id,
            role = newRole,
        )

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(user1ToBeUpdated.id.toString())
            .setNewRole(newRole)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(
            projectAdminMember1, projectAdminMember2,
        )
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { userRepoMock.getUserById(user1ToBeUpdated.id) } returns Result.success(user1ToBeUpdated)
        coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, user1ToBeUpdated.id) } returns
            Result.success(projectAdminMember1)
        coEvery {
            projectMemberRepoMock.updateProjectMemberRole(project.id, user1ToBeUpdated.id, newRole)
        } returns updatedUser

        assertDoesNotThrow { mainService.updateProjectMemberRole(request) }
    }

    @Test
    fun `When an admin member is updated to admin again, then last-admin check is skipped`() = runTest {
        val user = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
        val userToBeUpdated = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
        val project = DataBuilder.createExampleProject()
        val projectAdminMember = DataBuilder.createExampleProjectMember(
            role = MemberRole.MEMBER_ROLE_ADMIN,
            userId = userToBeUpdated.id,
            projectId = project.id,
        )

        val request = ProjectOuterClass.Project.Member.Update
            .newBuilder()
            .setProjectId(project.id.toString())
            .setUserId(userToBeUpdated.id.toString())
            .setNewRole(MemberRole.MEMBER_ROLE_ADMIN)
            .build()

        mockCurrentUser(user)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(projectAdminMember)
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { userRepoMock.getUserById(userToBeUpdated.id) } returns Result.success(userToBeUpdated)
        coEvery { projectMemberRepoMock.getProjectMemberByComposedId(project.id, userToBeUpdated.id) } returns
            Result.success(projectAdminMember)
        coEvery {
            projectMemberRepoMock.updateProjectMemberRole(project.id, userToBeUpdated.id, MemberRole.MEMBER_ROLE_ADMIN)
        } returns projectAdminMember

        assertDoesNotThrow { mainService.updateProjectMemberRole(request) }
        coVerify(exactly = 1) { projectMemberRepoMock.getAllProjectAdmins(project.id) }
    }
}
