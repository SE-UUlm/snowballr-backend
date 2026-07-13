package se.uulm.snowballr.backend.integration

import io.mockk.CapturingSlot
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coJustRun
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.AfterEach
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
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.TestDatabase
import se.uulm.snowballr.backend.accessCheckerDeps
import se.uulm.snowballr.backend.auth.AuthenticationManager
import se.uulm.snowballr.backend.auth.CookieManager
import se.uulm.snowballr.backend.auth.IAuthenticationManager
import se.uulm.snowballr.backend.auth.ICookieManager
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.JwtManager
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.mail.EmailManager
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.mailServiceDeps
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.repository.RepositoryHelper
import se.uulm.snowballr.backend.repositoryLayerDeps
import se.uulm.snowballr.backend.service.IAuthenticationService
import se.uulm.snowballr.backend.service.ICriterionService
import se.uulm.snowballr.backend.service.IExportService
import se.uulm.snowballr.backend.service.IFetcherService
import se.uulm.snowballr.backend.service.IInvitationService
import se.uulm.snowballr.backend.service.IPaperService
import se.uulm.snowballr.backend.service.IProjectMemberService
import se.uulm.snowballr.backend.service.IProjectPaperService
import se.uulm.snowballr.backend.service.IProjectService
import se.uulm.snowballr.backend.service.IReadingListService
import se.uulm.snowballr.backend.service.IReviewService
import se.uulm.snowballr.backend.service.IUserService
import se.uulm.snowballr.backend.serviceLayerDeps
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@Tag("integration")
open class IntegrationTest : KoinTest {
    protected val db = TestDatabase()

    protected val envReaderMock = mockk<EnvReader>()
    protected val emailManagerMock = mockk<EmailManager>()
    protected val fetcherOrchestratorMock = mockk<IFetcherOrchestrator>()
    protected val fetcherManagerMock = mockk<IFetcherManager>()

    private val allMocks = arrayOf(
        envReaderMock,
        emailManagerMock,
        fetcherManagerMock,
    )

    protected val authenticationService: IAuthenticationService by inject()
    protected val criterionService: ICriterionService by inject()
    protected val exportService: IExportService by inject()
    protected val invitationService: IInvitationService by inject()
    protected val paperService: IPaperService by inject()
    protected val projectMemberService: IProjectMemberService by inject()
    protected val projectPaperService: IProjectPaperService by inject()
    protected val projectService: IProjectService by inject()
    protected val readingListService: IReadingListService by inject()
    protected val reviewService: IReviewService by inject()
    protected val userService: IUserService by inject()
    protected val fetcherService: IFetcherService by inject()

    private val integrationTestModule = module {
        single { envReaderMock }

        // Mock Env.Encryption since it's always called when initializing the JwtManager
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
        every { envReaderMock.env.encryption } returns Env.Encryption(privateKeyBase64, publicKeyBase64)
        // Mock Env.Lifetime since it's used by the InvitationService
        every { envReaderMock.env.lifetime } returns Env.Lifetime(30, 7, 1)

        // Test database
        single<IDatabase> { db }

        // Other mocks
        single<IEmailManager> { emailManagerMock }
        single<IFetcherManager> { fetcherManagerMock }
        single<IFetcherOrchestrator> { fetcherOrchestratorMock }
        // FetcherOrchestrator is not yet part of the integration tests
        coJustRun { fetcherOrchestratorMock.enqueue(any()) }

        // The other layers are the same as in production
        repositoryLayerDeps()
        mailServiceDeps()
        customServicesDeps()
        accessCheckerDeps()
        serviceLayerDeps()
    }

    // User for testing. This prevents having to create a user for each test.
    protected var testUserId: UUID = UUID.randomUUID()

    private fun Module.customServicesDeps() {
        singleOf(::JwtManager) {
            createdAtStart()
            bind<IJwtManager>()
        }
        singleOf(::CookieManager) { bind<ICookieManager>() }
        singleOf(::AuthenticationManager) { bind<IAuthenticationManager>() }
    }

    @BeforeEach
    fun setUpTest() {
        db.setUp()
        RepositoryHelper.db = db
        startKoin {
            modules(integrationTestModule)
        }

        db.setUpTest(needsTestUser = true) { testUserId = it }
        RequestContext.bind(RequestContext(userId = testUserId))
    }

    @AfterEach
    fun tearDownTest() {
        RequestContext.unbind()
        db.tearDownTest()
        checkUnnecessaryStub(*allMocks)
        clearAllMocks()

        db.tearDown()
        db.close()
        stopKoin()
    }

    /**
     * Creates a paper with the passed data.
     *
     * @param title The title of the created paper. Defaults to "Test Paper".
     * @param externalId The external ID of the created paper. Defaults to null.
     */
    protected suspend fun createPaper(title: String = "Test Paper", externalId: String? = null): PaperResponse {
        val request = CreatePaperRequest(
            title = title,
            externalId = externalId,
            abstract = "Abstract text",
            year = 2024,
            publisher = "Publisher",
            publicationType = "Journal",
            publicationName = "Journal Name",
            authors = emptyList(),
            fetcherMetadata = emptyMap(),
        )
        return paperService.createPaper(request)
    }

    /**
     * Registers a user with the passed data and verifies their account. This enables using other users for tests.
     *
     * @param user The user data to register with. The email must be unique, otherwise the registration will fail.
     * @return The registered user.
     */
    protected suspend fun addUser(user: User): User {
        val verificationToken = slot<String>()
        val link = "https://example.com/verify"
        coEvery { emailManagerMock.createVerificationLink(capture(verificationToken)) } returns link
        every { envReaderMock.env.lifetime.verificationTokenLifeTimeInDays } returns 1
        val verificationData = EmailData.EmailVerification(user.firstName, link, "tomorrow")
        coJustRun { emailManagerMock.sendVerificationEmail(any(), verificationData) }

        // Register user
        val registerUserRequest = RegisterRequest(
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            password = "SecureP@ssw0rd!",
        )
        userService.register(registerUserRequest)

        // Verify the user's email
        authenticationService.verifyEmail(verificationToken.captured)

        // Retrieve the user to ensure it was added successfully
        return userService.getUserByEmail(user.email)
    }

    /**
     * Invites a user to a project. This enables creating invitations.
     *
     * @param project The project to which the user should be invited to.
     * @param user The user that should be invited to the passed project.
     * @param acceptInvitation Whether the invitation should be accepted after being sent. If true, the user will be
     * added to the project.
     */
    protected suspend fun inviteUserToProject(project: Project, user: User, acceptInvitation: Boolean = false) {
        val invitationToken = inviteHelper(project, user.firstName, user.email)

        if (acceptInvitation) {
            invitationService.acceptProjectInvitation(invitationToken.captured)
        }
    }

    /**
     * Invites a user to a project by their email. This enables creating invitations for users that are not registered
     * yet, which is not possible with [inviteUserToProject].
     *
     * @param project The project to which the user should be invited to.
     * @param email The email of the user that should be invited to the passed project.
     */
    protected suspend fun inviteEmailToProject(project: Project, email: String) {
        inviteHelper(project, "User", email)
    }

    /**
     * Runs code as another user. This enables making requests as another user to test external events.
     *
     * @param userId The ID of the user that should execute the requests in [block].
     * @param block The code that is executed on behalf of the user with the passed [userId].
     */
    protected suspend fun actAsUser(userId: UUID, block: suspend () -> Unit) {
        RequestContext.current().userId = userId
        block()
        RequestContext.current().userId = testUserId
    }

    private suspend fun inviteHelper(
        project: Project,
        inviteeFirstName: String,
        inviteeEmail: String,
    ): CapturingSlot<String> {
        val invitationToken = slot<String>()
        val link = "https://example.com/accept-invitation"
        coEvery { emailManagerMock.createAcceptProjectInvitationLink(capture(invitationToken)) } returns link
        every { envReaderMock.env.lifetime.invitationTokenLifeTimeInDays } returns 7
        val invitationData =
            EmailData.AcceptProjectInvitation(inviteeFirstName, "Test User", project.name, link, "in 7 days")
        coJustRun { emailManagerMock.sendAcceptProjectInvitationEmail(any(), invitationData) }

        invitationService.inviteUserToProject(project.id, inviteeEmail)

        return invitationToken
    }

    /**
     * Adds the [paper] to the [project] in stage 0.
     */
    protected suspend fun addToProject(project: Project, paper: PaperResponse) =
        projectPaperService.addPaperToProject(project.id, paper.id, 0)
}
