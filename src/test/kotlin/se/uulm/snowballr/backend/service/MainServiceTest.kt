package se.uulm.snowballr.backend.service

import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import org.simplejavamail.api.mailer.Mailer
import se.uulm.snowballr.backend.RandomKeyGenerator
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.mail.EmailTemplateManager
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.criterion.CreateCriterionTest
import se.uulm.snowballr.backend.serviceLayerDeps

/**
 * Unit test class for the [MainService] class.
 *
 * This test class provides the setup and teardown logic needed to test [MainService] effectively. It leverages
 * dependency mocking for all repositories to isolate the service and test its functionality without reliance on
 * external systems.
 *
 * An extension of this class can be used to implement specific test cases for the sub-services of
 * [MainService], such as [CreateCriterionTest].
 *
 * Example usage:
 * ```kotlin
 * class CreateExampleTest : MainServiceTest() {
 *     @Test
 *     fun `When an example is correctly created, then no exception is thrown`() =
 *         runTest {
 *             val request = ExampleOuterClass.Example.Create.getDefaultInstance()
 *             val example = ExampleOuterClass.Example.getDefaultInstance()
 *
 *             // Mock the behavior of the repositories
 *             coEvery { exampleRepoMock.createExample(...) } returns Result.success(example)
 *
 *             // Assert service behavior
 *             assertDoesNotThrow { mainService.createExample(request) }
 *         }
 *
 *     @Test
 *     fun `When an error occurs during example creation, then a TestSpecificException is thrown`() =
 *         runTest {
 *             val request = ExampleOuterClass.Example.Create.getDefaultInstance()
 *
 *             // Mock the behavior of the repositories
 *             coEvery { exampleRepoMock.createExample(...) } returns Result.failure(TestSpecificException())
 *
 *             // Assert service behavior
 *             assertThrows<TestSpecificException> { mainService.createExample(request) }
 *         }
 * }
 * ```
 */
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
open class MainServiceTest : KoinTest {
    // Environment dependencies
    val envServiceMock = mockk<IEnvService>()
    val envReaderMock = mockk<EnvReader>()

    // Repository layer mocks
    val projectRepoMock = mockk<IProjectTableRepo>()
    val criterionRepoMock = mockk<ICriterionTableRepo>()
    val userRepoMock = mockk<IUserTableRepo>()
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>()
    val projectPaperRepoMock = mockk<IProjectPaperTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()
    val readingListRepoMock = mockk<IReadingListTableRepo>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val reviewHasCriterionRepoMock = mockk<IReviewHasCriterionTableRepo>()
    val verificationTokenRepoMock = mockk<IVerificationTokenTableRepo>()
    val invitationTokenRepoMock = mockk<IInvitationTokenTableRepo>()

    // Custom services / manager / clients mocks
    val jwtManagerMock = mockk<IJwtManager>()
    val emailManagerMock = mockk<IEmailManager>()
    val fetcherManagerMock = mockk<FetcherManager>()
    val mailerMock = mockk<Mailer>()
    val emailTemplateManagerMock = mockk<EmailTemplateManager>()

    val allMocks = arrayOf(
        projectRepoMock,
        criterionRepoMock,
        userRepoMock,
        projectMemberRepoMock,
        jwtManagerMock,
        emailManagerMock,
        fetcherManagerMock,
        mailerMock,
        emailTemplateManagerMock,
        projectPaperRepoMock,
        citationRepoMock,
        readingListRepoMock,
        paperRepoMock,
        reviewRepoMock,
        reviewHasCriterionRepoMock,
        verificationTokenRepoMock,
        invitationTokenRepoMock,
    )

    val mainService: IMainService by inject()

    private fun mockEnv() {
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns ""
        every { miscellaneousMock.logLevel } returns "DEBUG"

        val encryptionMock = mockk<Env.Encryption>()
        val (privateKeyBase64, publicKeyBase64) = RandomKeyGenerator.generateKeyPair()
        every { encryptionMock.jwtPrivateKeyBase64 } returns privateKeyBase64
        every { encryptionMock.jwtPublicKeyBase64 } returns publicKeyBase64

        val smtpMock = mockk<Env.SMTP>()
        every { smtpMock.smtpHost } returns ""
        every { smtpMock.smtpPort } returns 0
        every { smtpMock.smtpUser } returns ""
        every { smtpMock.smtpPassword } returns ""
        every { smtpMock.smtpTransportLoggingOnlyEnabled } returns true
        every { smtpMock.smtpSenderName } returns ""
        every { smtpMock.smtpSenderEmail } returns ""

        val lifetimeMock = mockk<Env.Lifetime>()
        every { lifetimeMock.sensitiveInformationRetentionDays } returns 30
        every { lifetimeMock.invitationTokenLifeTimeInDays } returns 7
        every { lifetimeMock.verificationTokenLifeTimeInDays } returns 1

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock
        every { envMock.encryption } returns encryptionMock
        every { envMock.smtp } returns smtpMock
        every { envMock.lifetime } returns lifetimeMock

        every { envReaderMock.env } returns envMock
    }

    // Note that we cannot use the list of all mocks to add it to the module.
    private val serviceTestModule = module {
        // Environment dependencies
        single { envServiceMock }
        single { envReaderMock }

        // Mock env variables
        mockEnv()

        // Repository layer
        single { projectRepoMock }
        single { criterionRepoMock }
        single { userRepoMock }
        single { projectMemberRepoMock }
        single { projectPaperRepoMock }
        single { citationRepoMock }
        single { readingListRepoMock }
        single { paperRepoMock }
        single { reviewRepoMock }
        single { reviewHasCriterionRepoMock }
        single { verificationTokenRepoMock }
        single { invitationTokenRepoMock }

        // Custom services / managers / clients
        single { jwtManagerMock }
        single { emailManagerMock }
        single { fetcherManagerMock }
        single { mailerMock }
        single { emailTemplateManagerMock }

        // The base service layer is the same as in production
        serviceLayerDeps()
    }

    @BeforeEach
    fun setUpTest() {
        startKoin {
            modules(serviceTestModule)
        }
        mockkObject(GrpcContext)
    }

    @AfterEach
    open fun tearDownTest() {
        checkUnnecessaryStub(*allMocks)
        clearAllMocks()
        stopKoin()
    }

    /**
     * Mock the current user that is passed through the [withUser] helper.
     */
    protected fun mockCurrentUser(currentUser: User) {
        every { GrpcContext.getUserIdFromContext() } returns currentUser.id
        coEvery { userRepoMock.getUserById(currentUser.id) } returns Result.success(currentUser)
    }
}
