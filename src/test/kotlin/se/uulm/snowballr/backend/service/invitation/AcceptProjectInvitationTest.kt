package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.coVerify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import se.uulm.snowballr.backend.TestSpecificException
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass
import java.time.OffsetDateTime
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

class AcceptProjectInvitationTest : InvitationServiceTest() {
    @Test
    fun `When the invitation token is not found, then a InvitationTokenNotFoundException is thrown`() = runTest {
        val request = GrpcProjectMember.Accept.newBuilder().setToken("non-existent-token").build()
        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(any()) } returns Result.failure(
            InvitationTokenNotFoundException(),
        )

        assertThrows<InvitationTokenNotFoundException> { service.acceptProjectInvitation(request) }
    }

    @Test
    fun `When the invitation token has expired, then a InvitationTokenNotFoundException is thrown`() = runTest {
        val expiredToken = DataBuilder.createExampleInvitationToken(
            expiresAt = OffsetDateTime.now().minusDays(1),
        )
        val request = GrpcProjectMember.Accept.newBuilder().setToken(expiredToken.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(expiredToken.token) } returns Result.success(
            expiredToken,
        )
        coJustRun { invitationTokenRepoMock.deleteInvitationToken(expiredToken.token) }

        assertThrows<InvitationTokenNotFoundException> { service.acceptProjectInvitation(request) }
    }

    @Test
    fun `When the user associated with the token is not registered, then a FailedPreconditionException is thrown`() =
        runTest {
            val token = DataBuilder.createExampleInvitationToken()
            val request = GrpcProjectMember.Accept.newBuilder().setToken(token.token).build()

            coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
            coEvery { userRepoMock.getUserByEmail(token.email) } returns
                Result.failure(UserNotFoundByEmailException(token.email))

            assertThrows<FailedPreconditionException> { service.acceptProjectInvitation(request) }
        }

    @Test
    fun `When the user has not verified their email, then a FailedPreconditionException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val request = GrpcProjectMember.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)

        assertThrows<FailedPreconditionException> { service.acceptProjectInvitation(request) }
    }

    @Test
    fun `When adding the user to the project fails, then a TestSpecificException is thrown`() = runTest {
        val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE)
        val token = DataBuilder.createExampleInvitationToken(email = user.email)
        val request = GrpcProjectMember.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
        coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.acceptProjectInvitation(request) }
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
        val request = GrpcProjectMember.Accept.newBuilder().setToken(token.token).build()

        coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
        coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
        coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } returns userMember
        coEvery { invitationTokenRepoMock.deleteInvitationToken(token.token) } throws TestSpecificException()

        assertThrows<TestSpecificException> { service.acceptProjectInvitation(request) }
    }

    @Test
    fun `When a valid token is provided and all operations succeed, then the token is successfully deleted afterwards`() =
        runTest {
            val user = DataBuilder.createExampleUser(status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE)
            val token = DataBuilder.createExampleInvitationToken(email = user.email)
            val userMember = ProjectMember(
                projectId = token.projectId,
                userId = user.id,
                role = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT,
                createdAt = OffsetDateTime.now(),
                modifiedAt = null,
            )
            val request = GrpcProjectMember.Accept.newBuilder().setToken(token.token).build()

            coEvery { invitationTokenRepoMock.getInvitationTokenByValue(token.token) } returns Result.success(token)
            coEvery { userRepoMock.getUserByEmail(token.email) } returns Result.success(user)
            coEvery { projectMemberRepoMock.addUserToProject(user.id, token.projectId) } returns userMember
            coJustRun { invitationTokenRepoMock.deleteInvitationToken(token.token) }

            service.acceptProjectInvitation(request)

            coVerify(exactly = 1) { invitationTokenRepoMock.deleteInvitationToken(token.token) }
        }
}
