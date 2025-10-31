package se.uulm.snowballr.backend.service.invitations

import io.mockk.coEvery
import io.mockk.coVerify
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
import snowballr.UserOuterClass.UserRole
import java.util.UUID
import kotlin.reflect.KFunction

class InviteUserToProjectTest : MainServiceTest() {
    private val invitedUserEmail = "invited.user@example.com"
    private val projectId = UUID.randomUUID()
    private val validInviteUserRequest = ProjectOuterClass.Project.Member.Invite.newBuilder()
        .setUserEmail(invitedUserEmail)
        .setProjectId(projectId.toString())

    @Suppress("ReturnCount")
    private fun mockInviteUserToProject(
        useAdminUser: Boolean = true,
        invitedUserEmail: String = this.invitedUserEmail,
        projectId: UUID = this.projectId,
        stopBefore: KFunction<*>? = null,
    ) {
        val currentUser = DataBuilder.createExampleUser(
            role = if (useAdminUser) {
                UserRole.USER_ROLE_ADMIN
            } else {
                UserRole.USER_ROLE_DEFAULT
            },
        )
        val invitedUser = DataBuilder.createExampleUser(email = invitedUserEmail)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectAdmin = DataBuilder.createExampleProjectMember(
            projectId = project.id,
            userId = currentUser.id,
            role = ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN,
        )

        mockCurrentUser(currentUser)
        if (stopBefore == projectMemberRepoMock::getAllProjectAdmins) {
            return
        }
        coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns listOf(projectAdmin)
        if (stopBefore == projectRepoMock::getProjectById) {
            return
        }
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.success(project)
        if (stopBefore == invitationTokenRepoMock::getInvitationTokenByEmailAndProjectId) {
            return
        }
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(invitedUserEmail, projectId)
        } returns Result.failure(TestSpecificException())
        if (stopBefore == invitationTokenRepoMock::saveInvitationToken) {
            return
        }
        coEvery { invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, any()) } returns Unit
        if (stopBefore == userRepoMock::getUserByEmail) {
            return
        }
        coEvery { userRepoMock.getUserByEmail(invitedUserEmail) } returns Result.success(invitedUser)
        coEvery { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        if (stopBefore == emailManagerMock::sendAcceptProjectInvitationEmail) {
            return
        }
        coEvery { emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, any()) } returns Unit
    }

    @Test
    fun `When neither a server admin nor a project admin invites another user, then an UnauthorizedException is thrown`() =
        runTest {
            mockInviteUserToProject(useAdminUser = false, stopBefore = projectMemberRepoMock::getAllProjectAdmins)
            coEvery { projectMemberRepoMock.getAllProjectAdmins(projectId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.inviteUserToProject(validInviteUserRequest.build()) }
            coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
        }

    @Test
    fun `When the project is not found, then a TestSpecificException is thrown`() = runTest {
        mockInviteUserToProject(stopBefore = projectRepoMock::getProjectById)
        coEvery { projectRepoMock.getProjectById(projectId) } returns Result.failure(TestSpecificException())

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When the project is not active, then a FailedPreconditionException is thrown`() = runTest {
        val inactiveProject = DataBuilder.createExampleProject(
            id = projectId,
            status = ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED,
        )

        mockInviteUserToProject(stopBefore = projectRepoMock::getProjectById)
        coEvery { projectRepoMock.getProjectById(inactiveProject.id) } returns Result.success(inactiveProject)

        assertThrows<FailedPreconditionException> { mainService.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When a user is already invited, then no exception is thrown, but also no invitation sent`() = runTest {
        val invitationToken = DataBuilder.createExampleInvitationToken(email = invitedUserEmail)
        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::getInvitationTokenByEmailAndProjectId)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(invitedUserEmail, projectId)
        } returns Result.success(invitationToken)

        assertDoesNotThrow { mainService.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When saving the invitation token fails, then a TestSpecificException is thrown`() = runTest {
        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::saveInvitationToken)
        coEvery {
            invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, any())
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { emailManagerMock.sendAcceptProjectInvitationEmail(any(), any()) }
    }

    @Test
    fun `When sending the invitation email fails, then a TestSpecificException is thrown`() = runTest {
        mockInviteUserToProject(stopBefore = emailManagerMock::sendAcceptProjectInvitationEmail)
        coEvery {
            emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, any())
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.inviteUserToProject(validInviteUserRequest.build()) }
    }

    @Test
    @Suppress("LongMethod")
    fun `When the invitation is valid, then a token is saved and an email is sent`() = runTest {
        val invitedUser = DataBuilder.createExampleUser(email = invitedUserEmail)
        val tokenSlot = slot<String>()
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()

        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::saveInvitationToken)
        coEvery {
            invitationTokenRepoMock.saveInvitationToken(
                invitedUserEmail,
                projectId,
                capture(tokenSlot),
            )
        } returns Unit
        coEvery { userRepoMock.getUserByEmail(invitedUserEmail) } returns Result.success(invitedUser)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } answers {
            "https://link/${firstArg<String>()}"
        }
        coEvery {
            emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, capture(emailDataSlot))
        } returns Unit

        assertDoesNotThrow { mainService.inviteUserToProject(validInviteUserRequest.build()) }

        val capturedToken = tokenSlot.captured
        assertThat(capturedToken).isNotBlank()
        val capturedEmailData = emailDataSlot.captured
        val expectedInvitationLink = "https://link/$capturedToken"
        assertEquals(invitedUser.firstName, capturedEmailData.firstName)
        assertEquals(expectedInvitationLink, capturedEmailData.acceptanceLink)
    }

    @Test
    fun `When the invited user does not exist, then the first name defaults to 'User'`() = runTest {
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()
        val emailOfNonExistingUser = "non-existing-user@example.com"

        mockInviteUserToProject(invitedUserEmail = emailOfNonExistingUser, stopBefore = userRepoMock::getUserByEmail)

        val emailNotFoundException =
            NotFoundException(EntityType.USER, invitedUserEmail, identifierType = IdentifierType.EMAIL)
        coEvery { userRepoMock.getUserByEmail(emailOfNonExistingUser) } returns Result.failure(emailNotFoundException)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        coEvery { emailManagerMock.sendAcceptProjectInvitationEmail(any(), capture(emailDataSlot)) } returns Unit

        val inviteNonExistentUserRequest = validInviteUserRequest.setUserEmail(emailOfNonExistingUser)

        assertDoesNotThrow { mainService.inviteUserToProject(inviteNonExistentUserRequest.build()) }

        assertEquals("User", emailDataSlot.captured.firstName)
    }
}
