package se.uulm.snowballr.backend.service.invitations

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.time.OffsetDateTime

class AcceptProjectInvitationTest : MainServiceTest() {
    @Test
    fun `When the invitation token is not found, then a InvitationTokenNotFoundException is thrown`() = runTest {
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken("non-existent-token").build()
        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(any()) } returns Result.failure(
            InvitationTokenNotFoundException(),
        )

        assertThrows<InvitationTokenNotFoundException> { mainService.acceptProjectInvitation(request) }
    }

    @Test
    fun `When the invitation token has expired, then a InvitationTokenNotFoundException is thrown`() = runTest {
        val expiredToken = DataBuilder.createExampleInvitationToken(
            expiresAt = OffsetDateTime.now().minusDays(1),
        )
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(expiredToken.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(expiredToken.token) } returns Result.success(
            expiredToken,
        )
        coEvery { invitationTokenRepoMock.deleteInvitationToken(expiredToken.token) } returns Unit

        assertThrows<InvitationTokenNotFoundException> { mainService.acceptProjectInvitation(request) }
    }

    @Test
    fun `When the user associated with the token is not registered, then a TestSpecificException is thrown`() =
        runTest {
            val token = DataBuilder.createExampleInvitationToken()
            val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(token.token).build()

            coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
            coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.failure(TestSpecificException())

            assertThrows<TestSpecificException> { mainService.acceptProjectInvitation(request) }
        }

    @Test
    fun `When the user has not verified their email, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)

        assertThrows<FailedPreconditionException> { mainService.acceptProjectInvitation(request) }
    }

    @Test
    fun `When adding the user to the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
        coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.acceptProjectInvitation(request) }
    }

    @Test
    fun `When deleting the invitation token fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val userMember = ProjectMember(
            projectId = token.projectId,
            userId = user.id,
            role = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT,
            createdAt = OffsetDateTime.now(),
            modifiedAt = null,
        )
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
        coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } returns userMember
        coEvery { invitationTokenRepoMock.deleteInvitationToken(token.token) } throws TestSpecificException()

        assertThrows<TestSpecificException> { mainService.acceptProjectInvitation(request) }
    }

    @Test
    fun `When a valid token is provided and all operations succeed, then no exception is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val userMember = ProjectMember(
            projectId = token.projectId,
            userId = user.id,
            role = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT,
            createdAt = OffsetDateTime.now(),
            modifiedAt = null,
        )
        val request = ProjectOuterClass.Project.Member.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
        coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } returns userMember
        coEvery { invitationTokenRepoMock.deleteInvitationToken(token.token) } returns Unit

        assertDoesNotThrow { mainService.acceptProjectInvitation(request) }
    }
}
