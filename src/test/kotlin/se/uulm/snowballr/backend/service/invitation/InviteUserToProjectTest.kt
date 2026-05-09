package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.coJustRun
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
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.MemberRole
import java.util.UUID
import kotlin.reflect.KFunction

class InviteUserToProjectTest : InvitationServiceTest() {
    private val invitedUserEmail = "invited.user@example.com"
    private val projectId = UUID.randomUUID()
    private val validInviteUserRequest = ProjectOuterClass.Project.Member.Invite.newBuilder()
        .setUserEmail(invitedUserEmail)
        .setProjectId(projectId.toString())

    @Suppress("ReturnCount")
    private fun mockInviteUserToProject(
        invitedUserEmail: String = this.invitedUserEmail,
        projectId: UUID = this.projectId,
        stopBefore: KFunction<*>? = null,
    ) {
        val currentUser = DataBuilder.createExampleUser()
        val invitedUser = DataBuilder.createExampleUser(email = invitedUserEmail)
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectAdmin = DataBuilder.createExampleProjectMember(
            projectId = project.id,
            userId = currentUser.id,
            role = MemberRole.MEMBER_ROLE_ADMIN,
        )
        val projectAdminWithUser = DataBuilder.createExampleProjectMemberWithUser(projectAdmin, currentUser)

        mockCurrentUser(currentUser)
        val projectResult = Result.success(project)
        coEvery { projectRepoMock.getProjectById(projectId) } returns projectResult
        if (stopBefore == projectMemberRepoMock::getAllProjectAdmins) {
            return
        }
        coJustRun { invitationAccessCheckerMock.isAllowedToInviteUserToProject(currentUser, projectId, projectResult) }
        if (stopBefore == projectMemberRepoMock::getProjectMembersWithUsers) {
            return
        }
        coEvery { projectMemberRepoMock.getProjectMembersWithUsers(projectId) } returns listOf(projectAdminWithUser)
        if (stopBefore == invitationTokenRepoMock::getInvitationTokenByEmailAndProjectId) {
            return
        }
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(invitedUserEmail, projectId)
        } returns Result.failure(TestSpecificException())
        if (stopBefore == invitationTokenRepoMock::saveInvitationToken) {
            return
        }
        coJustRun { invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, any()) }
        if (stopBefore == userRepoMock::getUserByEmail) {
            return
        }
        coEvery { userRepoMock.getUserByEmail(invitedUserEmail) } returns Result.success(invitedUser)
        coEvery { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        if (stopBefore == emailManagerMock::sendAcceptProjectInvitationEmail) {
            return
        }
        coJustRun { emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, any()) }
    }

    @Test
    fun `When a user invites another user, but has no access, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectResult = Result.success(project)

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coEvery {
            invitationAccessCheckerMock.isAllowedToInviteUserToProject(currentUser, project.id, projectResult)
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When retrieving the project fails, then a TestSpecificException is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser()
        val project = DataBuilder.createExampleProject(id = projectId)
        val projectResult = Result.failure<Project>(TestSpecificException())

        mockCurrentUser(currentUser)
        coEvery { projectRepoMock.getProjectById(project.id) } returns projectResult
        coJustRun {
            invitationAccessCheckerMock.isAllowedToInviteUserToProject(currentUser, project.id, projectResult)
        }

        assertThrows<TestSpecificException> { service.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When a user is already invited, then no exception is thrown, but also no invitation sent`() = runTest {
        val invitationToken = DataBuilder.createExampleInvitationToken(email = invitedUserEmail)
        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::getInvitationTokenByEmailAndProjectId)
        coEvery {
            invitationTokenRepoMock.getInvitationTokenByEmailAndProjectId(invitedUserEmail, projectId)
        } returns Result.success(invitationToken)

        assertDoesNotThrow { service.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(any(), any(), any()) }
    }

    @Test
    fun `When saving the invitation token fails, then a TestSpecificException is thrown`() = runTest {
        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::saveInvitationToken)
        coEvery {
            invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, any())
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.inviteUserToProject(validInviteUserRequest.build()) }
        coVerify(exactly = 0) { emailManagerMock.sendAcceptProjectInvitationEmail(any(), any()) }
    }

    @Test
    fun `When sending the invitation email fails, then a TestSpecificException is thrown`() = runTest {
        mockInviteUserToProject(stopBefore = emailManagerMock::sendAcceptProjectInvitationEmail)
        coEvery {
            emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, any())
        } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.inviteUserToProject(validInviteUserRequest.build()) }
    }

    @Test
    fun `When the invitation is valid, then a token is saved and an email is sent`() = runTest {
        val invitedUser = DataBuilder.createExampleUser(email = invitedUserEmail)
        val tokenSlot = slot<String>()
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()

        mockInviteUserToProject(stopBefore = invitationTokenRepoMock::saveInvitationToken)
        coJustRun { invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, capture(tokenSlot)) }
        coEvery { userRepoMock.getUserByEmail(invitedUserEmail) } returns Result.success(invitedUser)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } answers {
            "https://link/${firstArg<String>()}"
        }
        coJustRun { emailManagerMock.sendAcceptProjectInvitationEmail(invitedUserEmail, capture(emailDataSlot)) }

        assertDoesNotThrow { service.inviteUserToProject(validInviteUserRequest.build()) }

        val capturedToken = tokenSlot.captured
        assertThat(capturedToken).isNotBlank()
        val capturedEmailData = emailDataSlot.captured
        val expectedInvitationLink = "https://link/$capturedToken"
        assertEquals(invitedUser.firstName, capturedEmailData.inviteeFirstName)
        assertEquals(expectedInvitationLink, capturedEmailData.acceptanceLink)
    }

    @Test
    fun `When the invited user does not exist, then the first name defaults to 'User'`() = runTest {
        val emailDataSlot = slot<EmailData.AcceptProjectInvitation>()
        val emailOfNonExistentUser = "non-existing-user@example.com"

        mockInviteUserToProject(invitedUserEmail = emailOfNonExistentUser, stopBefore = userRepoMock::getUserByEmail)

        val emailNotFoundException = UserNotFoundByEmailException(emailOfNonExistentUser)
        coEvery { userRepoMock.getUserByEmail(emailOfNonExistentUser) } returns Result.failure(emailNotFoundException)
        every { emailManagerMock.createAcceptProjectInvitationLink(any()) } returns "http://invitation-link"
        coJustRun { emailManagerMock.sendAcceptProjectInvitationEmail(any(), capture(emailDataSlot)) }

        val inviteNonExistentUserRequest = validInviteUserRequest.setUserEmail(emailOfNonExistentUser)

        assertDoesNotThrow { service.inviteUserToProject(inviteNonExistentUserRequest.build()) }

        assertEquals("User", emailDataSlot.captured.inviteeFirstName)
    }

    @Test
    fun `When the invitation email refers to an already existent member, then it is not invited and the call is successful`() =
        runTest {
            val projectMemberWithUser = DataBuilder.createExampleProjectMemberWithUser(
                DataBuilder.createExampleProjectMember(projectId),
                DataBuilder.createExampleUser(email = invitedUserEmail),
            )

            mockInviteUserToProject(stopBefore = projectMemberRepoMock::getProjectMembersWithUsers)
            coEvery {
                projectMemberRepoMock.getProjectMembersWithUsers(projectId)
            } returns listOf(projectMemberWithUser)

            assertDoesNotThrow { service.inviteUserToProject(validInviteUserRequest.build()) }

            coVerify(exactly = 0) { invitationTokenRepoMock.saveInvitationToken(invitedUserEmail, projectId, any()) }
        }
}
