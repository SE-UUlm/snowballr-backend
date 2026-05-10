package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import se.uulm.snowballr.backend.access.IInvitationAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.InvitationService
import se.uulm.snowballr.backend.service.withUser

/**
 * Base test class for the [InvitationService].
 */
sealed class InvitationServiceTest : BaseServiceTest() {
    val userRepoMock = mockk<IUserTableRepo>()
    val projectRepoMock = mockk<IProjectTableRepo>()
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val invitationTokenRepoMock = mockk<IInvitationTokenTableRepo>()
    val emailManagerMock = mockk<IEmailManager>()
    val envReaderMock = mockk<EnvReader>()
    val invitationAccessCheckerMock = mockk<IInvitationAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    private val allMocks = arrayOf(
        userRepoMock,
        projectRepoMock,
        projectMemberRepoMock,
        invitationTokenRepoMock,
        emailManagerMock,
        envReaderMock,
        invitationAccessCheckerMock,
        projectAccessCheckerMock,
    )

    val service = InvitationService(
        userRepo = userRepoMock,
        projectRepo = projectRepoMock,
        projectMemberRepo = projectMemberRepoMock,
        invitationTokenRepo = invitationTokenRepoMock,
        emailManager = emailManagerMock,
        envReader = envReaderMock,
        accessChecker = invitationAccessCheckerMock,
        projectAccessChecker = projectAccessCheckerMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
