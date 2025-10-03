package se.uulm.snowballr.backend.service.invitations

import io.mockk.coEvery
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.util.UUID

class InviteUserToProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    @Test
    fun `When the project is not found, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val projectId = UUID.randomUUID()
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(projectId.toString())
            .build()

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(request) }
    }

    @Test
    fun `When the project is not active, then a FailedPreconditionException is thrown`() = runTest {
        val project = DataBuilder.createExampleProject(status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED)
        val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id.toString())
            .build()

        mockCurrentUser(currentUser)
        coEvery {
            projectMemberRepoMock.getAllProjectAdmins(project.id)
        } returns listOf(DataBuilder.createExampleProjectMember(userId = currentUser.id))
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)

        assertThrows<FailedPreconditionException> { mainService.inviteUserToProject(request) }
    }

    @Test
    fun `When the current user is not a project admin or server admin, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
            val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
                .setProjectId(projectId.toString())
                .build()

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.inviteUserToProject(request) }
        }

    @Test
    fun `When saving the invitation token fails, then a TestSpecificException is thrown`() = runTest {
        val project = DataBuilder.createExampleProject(status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE)
        val projectAdmin = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id.toString())
            .setUserEmail("test@example.com")
            .build()

        mockCurrentUser(projectAdmin)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(
            DataBuilder.createExampleProjectMember(userId = projectAdmin.id),
        )
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(request) }
    }

    @Test
    fun `When sending the invitation email fails, then a TestSpecificException is thrown`() = runTest {
        val project = DataBuilder.createExampleProject(status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE)
        val projectAdmin = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val invitedUser = DataBuilder.createExampleUser()
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id.toString())
            .setUserEmail(invitedUser.email)
            .build()

        mockCurrentUser(projectAdmin)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(
            DataBuilder.createExampleProjectMember(userId = projectAdmin.id),
        )
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) } returns Unit
        coEvery { userRepoMock.getUserByEmail(invitedUser.email) } returns Result.success(invitedUser)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        coEvery { emailManagerMock.sendAcceptProjectInvitationEmail(any(), any()) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(request) }
    }

    @Test
    @Suppress("LongMethod")
    fun `When the invitation is valid, then a token is saved and an email is sent`() = runTest {
        val project = DataBuilder.createExampleProject(status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE)
        val serverAdmin = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_ADMIN)
        val invitedUser = DataBuilder.createExampleUser()
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id.toString())
            .setUserEmail(invitedUser.email)
            .build()
        val tokenSlot = slot<String>()
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()

        mockCurrentUser(serverAdmin)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns emptyList()
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery {
            invitationTokenRepoMock.saveInvitationToken(
                invitedUser.email,
                project.id,
                capture(tokenSlot),
            )
        } returns Unit
        coEvery { userRepoMock.getUserByEmail(invitedUser.email) } returns Result.success(invitedUser)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } answers {
            "https://link/${firstArg<String>()}"
        }
        coEvery {
            emailManagerMock.sendAcceptProjectInvitationEmail(
                invitedUser.email,
                capture(emailDataSlot),
            )
        } returns Unit

        assertDoesNotThrow { mainService.inviteUserToProject(request) }

        val capturedToken = tokenSlot.captured
        assertThat(capturedToken).isNotBlank()
        val capturedEmailData = emailDataSlot.captured
        val expectedInvitationLink = "https://link/$capturedToken"
        assertEquals(invitedUser.firstName, capturedEmailData.firstName)
        assertEquals(project.name, capturedEmailData.projectName)
        assertEquals(expectedInvitationLink, capturedEmailData.acceptanceLink)
    }

    @Test
    fun `When the invited user does not exist, then the first name defaults to 'User'`() = runTest {
        val project = DataBuilder.createExampleProject(status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE)
        val projectAdmin = DataBuilder.createExampleUser(role = UserOuterClass.UserRole.USER_ROLE_DEFAULT)
        val invitedEmail = "non-existent-user@example.com"
        val request = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id.toString())
            .setUserEmail(invitedEmail)
            .build()
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()

        mockCurrentUser(projectAdmin)
        coEvery { projectMemberRepoMock.getAllProjectAdmins(project.id) } returns listOf(
            DataBuilder.createExampleProjectMember(userId = projectAdmin.id),
        )
        coEvery { projectRepoMock.getProjectById(project.id) } returns Result.success(project)
        coEvery { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) } returns Unit
        val userByEmailNotFoundException =
            NotFoundException(EntityType.USER, invitedEmail, identifierType = IdentifierType.EMAIL)
        coEvery { userRepoMock.getUserByEmail(invitedEmail) } returns Result.failure(userByEmailNotFoundException)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        coEvery { emailManagerMock.sendAcceptProjectInvitationEmail(any(), capture(emailDataSlot)) } returns Unit

        assertDoesNotThrow { mainService.inviteUserToProject(request) }

        assertEquals("User", emailDataSlot.captured.firstName)
    }
}
