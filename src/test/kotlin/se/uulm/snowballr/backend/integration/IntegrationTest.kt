package se.uulm.snowballr.backend.integration

import com.zaxxer.hikari.HikariDataSource
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.slot
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.createdAtStart
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.TestDatabase
import se.uulm.snowballr.backend.auth.AuthenticationManager
import se.uulm.snowballr.backend.auth.CookieManager
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.JwtManager
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.fetcher.PythonPluginFetcherManager
import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.mailServiceDeps
import se.uulm.snowballr.backend.mockEnvWithDefaultValues
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.repository.RepositoryHelper
import se.uulm.snowballr.backend.repositoryLayerDeps
import se.uulm.snowballr.backend.service.IMainService
import se.uulm.snowballr.backend.serviceLayerDeps
import snowballr.Authentication
import snowballr.ProjectOuterClass
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Tag("integration")
open class IntegrationTest : KoinTest {
    // Initialize DB with empty dataSource only to set it in the setUp method
    protected val db = TestDatabase(HikariDataSource())

    // Environment dependencies
    private val envServiceMock = mockk<IEnvService>()
    protected val envReaderMock = mockk<EnvReader>()
    protected val emailManagerMock = mockk<EmailManager>()

    private val allMocks = arrayOf(
        envServiceMock,
        envReaderMock,
        emailManagerMock,
    )

    protected val mainService: IMainService by inject()

    private val integrationTestModule = module {
        // Environment dependencies
        single { envServiceMock }
        single { envReaderMock }

        // Mock env variables
        every { envReaderMock.env } returns mockEnvWithDefaultValues()

        // Test database
        single<IDatabase> { db }

        // Other mocks
        single<IEmailManager> { emailManagerMock }

        // The other layers are the same as in production
        repositoryLayerDeps()
        mailServiceDeps()
        customServicesDeps()
        serviceLayerDeps()
    }

    /** User for testing. This prevents having to create a user for each test. */
    protected var testUserId: UUID = UUID.randomUUID()

    private fun Module.customServicesDeps() {
        singleOf(::JwtManager) {
            createdAtStart()
            bind<IJwtManager>()
        }
        singleOf(::PythonPluginFetcherManager) { bind<IFetcherManager>() }
        singleOf(::CookieManager) { bind<ICookieManager>() }
        singleOf(::AuthenticationManager) { bind<IAuthenticationManager>() }
    }

    @BeforeAll
    fun setUp() {
        db.setUp()
        RepositoryHelper.db = db
        startKoin {
            modules(integrationTestModule)
        }
    }

    @BeforeEach
    fun setUpTest() {
        db.setUpTest(needsTestUser = true) { testUserId = it }
        mockkObject(GrpcContext)
        every { GrpcContext.getUserIdFromContext() } returns testUserId
    }

    @AfterEach
    fun tearDownTest() {
        db.tearDownTest()
        checkUnnecessaryStub(*allMocks)
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        db.tearDown()
        stopKoin()
    }

    protected suspend fun addUser(user: User) {
        val verificationToken = slot<String>()
        val link = "https://example.com/verify"
        coEvery { emailManagerMock.createVerificationLink(capture(verificationToken)) } returns link
        val verificationData = EmailData.EmailVerification(user.firstName, link, "tomorrow")
        coEvery { emailManagerMock.sendVerificationEmail(any(), verificationData) } returns Unit

        // Register user
        val registerUserRequest = Authentication.RegisterRequest.newBuilder()
            .setFirstName(user.firstName)
            .setLastName(user.lastName)
            .setEmail(user.email)
            .setPassword("SecureP@ssw0rd!")
            .build()
        mainService.register(registerUserRequest)

        // Verify the user's email
        val verifyEmailRequest = Authentication.VerifyEmailRequest.newBuilder()
            .setToken(verificationToken.captured)
            .build()
        mainService.verifyEmail(verifyEmailRequest)
    }

    protected suspend fun inviteUserToProject(project: ProjectOuterClass.Project, user: User) {
        val invitationToken = slot<String>()
        val link = "https://example.com/accept-invitation"
        coEvery { emailManagerMock.createAcceptProjectInvitationLink(capture(invitationToken)) } returns link
        val invitationData =
            EmailData.AcceptProjectInvitation(user.firstName, "Test User", project.name, link, "in 7 days")
        coEvery { emailManagerMock.sendAcceptProjectInvitationEmail(any(), invitationData) } returns Unit

        val inviteUserRequest = ProjectOuterClass.Project.Member.Invite.newBuilder()
            .setProjectId(project.id)
            .setUserEmail(user.email)
            .build()
        mainService.inviteUserToProject(inviteUserRequest)
    }
}
