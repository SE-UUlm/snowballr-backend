package se.uulm.snowballr.backend.service.projectmember

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.UserOuterClass.UserStatus
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

class RemoveProjectMemberTest : MainServiceTest() {
    private val userEmail = "user@example.com"

    private fun getExampleRequest(projectId: UUID = UUID.randomUUID()) = GrpcProjectMember.Remove.newBuilder()
        .setProjectId(projectId.toString())
        .setUserEmail(userEmail)
        .build()

    @Test
    fun `When the user removes a project member, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToRemove = DataBuilder.createExampleUser(email = userEmail, status = UserStatus.USER_STATUS_ACTIVE)
        val project = DataBuilder.createExampleProject()
        val projectMember1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userToRemove.id)
        val projectMember2 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = UUID.randomUUID())

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.failure(TestSpecificException())
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, userToRemove.id) } returns true
        coJustRun { projectMemberAccessCheckerMock.isAllowedToRemoveMember(currentUser, userToRemove.id, project.id) }
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember1, projectMember2)
        coJustRun { projectAccessCheckerMock.isNotLastProjectAdmin(userToRemove, project.id, any()) }
        coJustRun { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) }

        mainService.removeProjectMember(getExampleRequest(project.id))

        coVerify(exactly = 1) { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) }
    }

    @Test
    fun `When retrieving the user to remove fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.failure(TestSpecificException())
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.removeProjectMember(getExampleRequest(project.id)) }
    }

    @Test
    fun `When the user removes a non-project member, then nothing happens`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToRemove = DataBuilder.createExampleUser(email = userEmail, status = UserStatus.USER_STATUS_ACTIVE)
        val project = DataBuilder.createExampleProject()

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.failure(TestSpecificException())
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, userToRemove.id) } returns false

        mainService.removeProjectMember(getExampleRequest(project.id))

        coVerify(exactly = 0) { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) }
    }

    @Test
    fun `When the user removes a user of a non-existent project, then a ProjectNotFoundException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val userToRemove = DataBuilder.createExampleUser(email = userEmail, status = UserStatus.USER_STATUS_ACTIVE)
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery {
                invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
            } returns Result.failure(TestSpecificException())
            coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
            coEvery { projectMemberRepoMock.isProjectMember(project.id, userToRemove.id) } returns true
            coJustRun {
                projectMemberAccessCheckerMock.isAllowedToRemoveMember(
                    currentUser,
                    userToRemove.id,
                    project.id,
                )
            }
            coEvery { projectRepoMock.doesProjectExistById(project.id) } returns false

            assertThrows<ProjectNotFoundException> { mainService.removeProjectMember(getExampleRequest(project.id)) }
        }

    @Test
    fun `When the user removes the last project member, then the project is soft-deleted`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val userToRemove = DataBuilder.createExampleUser(email = userEmail, status = UserStatus.USER_STATUS_ACTIVE)
        val project = DataBuilder.createExampleProject()
        val projectMember1 = DataBuilder.createExampleProjectMember(projectId = project.id, userId = userToRemove.id)

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.failure(TestSpecificException())
        coEvery { userRepoMock.getUserByEmail(userEmail) } returns Result.success(userToRemove)
        coEvery { projectMemberRepoMock.isProjectMember(project.id, userToRemove.id) } returns true
        coJustRun { projectMemberAccessCheckerMock.isAllowedToRemoveMember(currentUser, userToRemove.id, project.id) }
        coEvery { projectRepoMock.doesProjectExistById(project.id) } returns true
        coEvery { projectMemberRepoMock.getProjectMembers(project.id) } returns listOf(projectMember1)
        coJustRun { projectRepoMock.softDeleteProject(project.id) }
        coJustRun { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) }

        mainService.removeProjectMember(getExampleRequest(project.id))

        coVerify(exactly = 1) { projectRepoMock.softDeleteProject(project.id) }
        coVerify(exactly = 1) { projectMemberRepoMock.removeProjectMember(project.id, userToRemove.id) }
    }

    @Test
    fun `When the user removes an invitation and has access, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val invitationToken = DataBuilder.createExampleInvitationToken()

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.success(invitationToken)
        coJustRun { projectMemberAccessCheckerMock.isAllowedToRemoveInvitation(currentUser, project.id) }
        coJustRun { invitationTokenRepoMock.deleteInvitationToken(invitationToken.token) }

        mainService.removeProjectMember(getExampleRequest(project.id))

        coVerify(exactly = 1) { invitationTokenRepoMock.deleteInvitationToken(invitationToken.token) }
    }

    @Test
    fun `When the user removes an invitation, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject()
        val invitationToken = DataBuilder.createExampleInvitationToken()

        mockCurrentUser(currentUser)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(userEmail, project.id)
        } returns Result.success(invitationToken)
        coEvery {
            projectMemberAccessCheckerMock.isAllowedToRemoveInvitation(currentUser, project.id)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.removeProjectMember(getExampleRequest(project.id)) }
    }
}
