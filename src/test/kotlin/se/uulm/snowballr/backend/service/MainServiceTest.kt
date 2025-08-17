package se.uulm.snowballr.backend.service

import com.github.jknack.handlebars.Template
import io.mockk.checkUnnecessaryStub
import io.mockk.clearAllMocks
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
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.model.email.EmailTemplate
import se.uulm.snowballr.backend.repository.IAuthorTableRepo
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IPaperTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.association.IAuthorOfPaperTableRepo
import se.uulm.snowballr.backend.repository.association.ICitationTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReadingListTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewTableRepo
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
 *             coEvery { exampleRepoMock.createExample(any()) } returns example
 *
 *             // Assert service behavior
 *             assertDoesNotThrow { mainService.createExample(request) }
 *         }
 *
 *     @Test
 *     fun `When an error occurs during example creation, then an exception is thrown`() =
 *         runTest {
 *             val request = ExampleOuterClass.Example.Create.getDefaultInstance()
 *
 *             // Mock the behavior of the repositories
 *             coEvery { exampleRepoMock.createExample(any()) } throws TestSpecificException()
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
    val authorOfPaperRepoMock = mockk<IAuthorOfPaperTableRepo>()
    val authorRepoMock = mockk<IAuthorTableRepo>()
    val citationRepoMock = mockk<ICitationTableRepo>()
    val readingListRepoMock = mockk<IReadingListTableRepo>()
    val paperRepoMock = mockk<IPaperTableRepo>()
    val reviewRepoMock = mockk<IReviewTableRepo>()
    val reviewHasCriterionRepoMock = mockk<IReviewHasCriterionTableRepo>()
    val verificationTokenRepoMock = mockk<IVerificationTokenTableRepo>()

    // Custom services / manager / clients mocks
    val jwtServiceMock = mockk<IJwtService>()
    val emailServiceMock = mockk<IEmailService>()
    val fetcherManagerMock = mockk<FetcherManager>()
    val mailerMock = mockk<Mailer>()
    val compilesTemplatesMock = mockk<Map<EmailTemplate, Template>>()

    val allMocks = arrayOf(
        projectRepoMock,
        criterionRepoMock,
        userRepoMock,
        projectMemberRepoMock,
        jwtServiceMock,
        emailServiceMock,
        fetcherManagerMock,
        mailerMock,
        compilesTemplatesMock,
        projectPaperRepoMock,
        authorOfPaperRepoMock,
        authorRepoMock,
        citationRepoMock,
        readingListRepoMock,
        paperRepoMock,
        reviewRepoMock,
        reviewHasCriterionRepoMock,
        verificationTokenRepoMock,
    )

    val mainService: IMainService by inject()

    // Note that we cannot use the list of all mocks to add it to the module.
    private val serviceTestModule = module {
        // Environment dependencies
        single { envServiceMock }
        single { envReaderMock }

        // Repository layer
        single { projectRepoMock }
        single { criterionRepoMock }
        single { userRepoMock }
        single { projectMemberRepoMock }
        single { projectPaperRepoMock }
        single { authorOfPaperRepoMock }
        single { authorRepoMock }
        single { citationRepoMock }
        single { readingListRepoMock }
        single { paperRepoMock }
        single { reviewRepoMock }
        single { reviewHasCriterionRepoMock }
        single { verificationTokenRepoMock }

        // Custom services / managers / clients
        single { jwtServiceMock }
        single { emailServiceMock }
        single { fetcherManagerMock }
        single { mailerMock }
        single { compilesTemplatesMock }

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
}
