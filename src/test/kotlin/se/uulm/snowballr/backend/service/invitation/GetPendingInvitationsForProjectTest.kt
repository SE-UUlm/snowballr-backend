package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.coJustRun
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException

class GetPendingInvitationsForProjectTest : InvitationServiceTest() {
    @Test
    fun `When a user requests the pending invitations for a project and has access, then the correct values are returned`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.ADMIN)
            val project = DataBuilder.createExampleProject()
            val registeredUser = DataBuilder.createExampleUser(
                email = "registered@example.com",
                status = UserStatus.ACTIVE,
            )
            val invitationTokenForRegisteredUser = DataBuilder.createExampleInvitationToken(
                projectId = project.id,
                email = registeredUser.email,
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                invitationTokenRepoMock.getActiveInvitationTokensForProject(project.id)
            } returns listOf(invitationTokenForRegisteredUser)
            coEvery { userRepoMock.getUserByEmail(registeredUser.email) } returns Result.success(registeredUser)

            val result = service.getPendingInvitationsForProject(project.id)

            assertEquals(1, result.size)
            val resultElement = result.first()
            assertEquals(registeredUser.email, resultElement.email)
        }

    @Test
    fun `When a non registered user is invited, then they appear as well in the list of pending invitations but without complete user details`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()
            val registeredUser = DataBuilder.createExampleUser(
                email = "registered@example.com",
                status = UserStatus.ACTIVE,
            )
            val invitationTokenForRegisteredUser = DataBuilder.createExampleInvitationToken(
                projectId = project.id,
                email = registeredUser.email,
            )
            val notRegisteredEmail = "unregistered@example.com"
            val invitationTokenForNotRegisteredUser = DataBuilder.createExampleInvitationToken(
                projectId = project.id,
                email = notRegisteredEmail,
            )

            mockCurrentUser(currentUser)
            coJustRun { projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id) }
            coEvery {
                invitationTokenRepoMock.getActiveInvitationTokensForProject(project.id)
            } returns listOf(invitationTokenForRegisteredUser, invitationTokenForNotRegisteredUser)
            coEvery { userRepoMock.getUserByEmail(registeredUser.email) } returns Result.success(registeredUser)
            coEvery {
                userRepoMock.getUserByEmail(notRegisteredEmail)
            } returns Result.failure(UserNotFoundByEmailException(notRegisteredEmail))

            val pendingInvitations = service.getPendingInvitationsForProject(project.id)

            val invitationForRegisteredUser = pendingInvitations.find { it.email == registeredUser.email }
            val invitationForNotRegisteredUser = pendingInvitations.find { it.email == notRegisteredEmail }
            assertEquals(2, pendingInvitations.size)
            assertNotNull(invitationForRegisteredUser)
            assertNotNull(invitationForNotRegisteredUser)

            // Registered user should have user details
            assertEquals(registeredUser.id, invitationForRegisteredUser?.userId)
            assertEquals(UserStatus.ACTIVE, invitationForRegisteredUser?.status)
            assertEquals(registeredUser.firstName, invitationForRegisteredUser?.firstName)

            // Not registered user should not have user details
            assertEquals(null, invitationForNotRegisteredUser?.userId)
            assertEquals(UserStatus.ACTIVE_UNCONFIRMED, invitationForNotRegisteredUser?.status)
            assertEquals("", invitationForNotRegisteredUser?.firstName)
        }

    @Test
    fun `When a user requests the pending invitations for a project, but has no access, then a TestSpecificException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser()
            val project = DataBuilder.createExampleProject()

            mockCurrentUser(currentUser)
            coEvery {
                projectAccessCheckerMock.isAllowedToReadProject(currentUser, project.id)
            } throws TestSpecificException()

            assertThrows<TestSpecificException> { service.getPendingInvitationsForProject(project.id) }
        }
}
