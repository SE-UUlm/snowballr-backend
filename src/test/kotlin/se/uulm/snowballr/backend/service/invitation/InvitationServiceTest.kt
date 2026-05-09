package se.uulm.snowballr.backend.service.invitation

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.assertNotNull
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.access.IInvitationAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.mockEnvWithDefaultValues
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.IInvitationService
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
    val invitationAccessCheckerMock = mockk<IInvitationAccessChecker>()
    val projectAccessCheckerMock = mockk<IProjectAccessChecker>()

    // Environment dependencies
    val envServiceMock = mockk<IEnvService>()
    val envReaderMock = mockk<EnvReader>()

    private val allMocks = arrayOf(
        userRepoMock,
        projectRepoMock,
        projectMemberRepoMock,
        invitationTokenRepoMock,
        emailManagerMock,
        invitationAccessCheckerMock,
        projectAccessCheckerMock,
        envServiceMock,
        envReaderMock,
    )

    val service: IInvitationService by inject()

    private val module = module {
        // Environment dependencies
        single { envServiceMock }
        single { envReaderMock }

        // Mock env variables
        every { envReaderMock.env } returns mockEnvWithDefaultValues()
        // Assert env so that it is not recognized as an unnecessary stub
        assertNotNull(envReaderMock.env)

        single { userRepoMock }
        single { projectRepoMock }
        single { projectMemberRepoMock }
        single { invitationTokenRepoMock }
        single { emailManagerMock }
        single { invitationAccessCheckerMock }
        single { projectAccessCheckerMock }
    }

    override fun getModule(): Module = module

    override fun getAllMocks(): Array<Any> = allMocks

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
