package se.uulm.snowballr.backend.service.invitations

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Base
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

class GetPendingInvitationsForProjectTest : MainServiceTest() {
    private val projectId = UUID.randomUUID()

    private fun createRequest(id: UUID = projectId) = Base.Id.newBuilder().setId(id.toString()).build()

    @Test
    fun `When a server admin requests the pending invitations for a project, then no exception is thrown`() = runTest {
        val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)

        mockCurrentUser(currentUser)
        coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
        coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
        coEvery { invitationTokenRepoMock.getActiveInvitationTokensForProject(projectId) } returns emptyList()

        assertDoesNotThrow { mainService.getPendingInvitationsForProject(createRequest()) }
    }

    @Test
    fun `When a project member requests the pending invitations for a project, then no exception is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)
            val projectMember = DataBuilder.createExampleProjectMember(userId = currentUser.id, projectId = projectId)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns listOf(projectMember)
            coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
            coEvery { invitationTokenRepoMock.getActiveInvitationTokensForProject(projectId) } returns emptyList()

            assertDoesNotThrow { mainService.getPendingInvitationsForProject(createRequest()) }
        }

    @Test
    fun `When a non registered user is invited, then they appear as well in the list of pending invitations but without complete user details`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val registeredUser = DataBuilder.createExampleUser(
                email = "registered@example.com",
                status = UserStatus.USER_STATUS_ACTIVE,
            )
            val invitationTokenForRegisteredUser = DataBuilder.createExampleInvitationToken(
                projectId = projectId,
                email = registeredUser.email,
            )
            val notRegisteredEmail = "unregistered@example.com"
            val invitationTokenForNotRegisteredUser = DataBuilder.createExampleInvitationToken(
                projectId = projectId,
                email = notRegisteredEmail,
            )

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()
            coEvery { projectRepoMock.doesProjectExistById(projectId) } returns true
            coEvery { invitationTokenRepoMock.getActiveInvitationTokensForProject(projectId) } returns listOf(
                invitationTokenForRegisteredUser,
                invitationTokenForNotRegisteredUser,
            )
            coEvery { userRepoMock.getUserByEmail(registeredUser.email) } returns Result.success(registeredUser)
            coEvery {
                userRepoMock.getUserByEmail(notRegisteredEmail)
            } returns Result.failure(UserNotFoundByEmailException(notRegisteredEmail))

            val pendingInvitations = assertDoesNotThrow { mainService.getPendingInvitationsForProject(createRequest()) }

            val invitationForRegisteredUser = pendingInvitations.usersList.find { it.email == registeredUser.email }
            val invitationForNotRegisteredUser = pendingInvitations.usersList.find { it.email == notRegisteredEmail }
            assertEquals(2, pendingInvitations.usersList.size)
            assertNotNull(invitationForRegisteredUser)
            assertNotNull(invitationForNotRegisteredUser)

            // Registered user should have user details
            assertEquals(registeredUser.id.toString(), invitationForRegisteredUser?.id)
            assertEquals(UserStatus.USER_STATUS_ACTIVE, invitationForRegisteredUser?.status)
            assertEquals(registeredUser.firstName, invitationForRegisteredUser?.firstName)

            // Not registered user should not have user details
            assertEquals("", invitationForNotRegisteredUser?.id)
            assertEquals(UserStatus.USER_STATUS_UNSPECIFIED, invitationForNotRegisteredUser?.status)
            assertEquals("", invitationForNotRegisteredUser?.firstName)
        }

    @Test
    fun `When a non project member requests the pending invitations for a project, then an UnauthorizedException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_DEFAULT)

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(projectId) } returns emptyList()

            assertThrows<UnauthorizedException> { mainService.getPendingInvitationsForProject(createRequest()) }
        }

    @Test
    fun `When the pending invitations for a nonexistent project are requested, then a NotFoundException is thrown`() =
        runTest {
            val currentUser = DataBuilder.createExampleUser(role = UserRole.USER_ROLE_ADMIN)
            val nonExistentProjectId = UUID.randomUUID()

            mockCurrentUser(currentUser)
            coEvery { projectMemberRepoMock.getProjectMembers(nonExistentProjectId) } returns emptyList()
            coEvery { projectRepoMock.doesProjectExistById(nonExistentProjectId) } returns false

            assertThrows<NotFoundException> {
                mainService.getPendingInvitationsForProject(
                    createRequest(nonExistentProjectId),
                )
            }
        }
}
