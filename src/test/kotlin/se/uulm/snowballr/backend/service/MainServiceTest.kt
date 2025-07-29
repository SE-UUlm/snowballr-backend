package se.uulm.snowballr.backend.service

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.KoinTest
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class MainServiceTest : KoinTest {
    // Repository layer mocks
    val projectRepoMock = mockk<IProjectTableRepo>(relaxed = true)
    val criterionRepoMock = mockk<ICriterionTableRepo>(relaxed = true)
    val userRepoMock = mockk<IUserTableRepo>(relaxed = true)
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>(relaxed = true)

    // Custom services / manager / clients mocks
    val jwtServiceMock = mockk<IJwtService>(relaxed = true)
    val fetcherManagerMock = mockk<FetcherManager>(relaxed = true)

    val mainService: IMainService by inject()

    private val serviceTestModule = module {
        // Repository layer
        single { projectRepoMock }
        single { criterionRepoMock }
        single { userRepoMock }
        single { projectMemberRepoMock }

        // Custom services / managers / clients
        single { jwtServiceMock }
        single { fetcherManagerMock }

        // The base service layer is the same as in production
        serviceLayerDeps()
    }

    @BeforeAll
    fun setUp() {
        startKoin {
            modules(serviceTestModule)
        }
        mockkObject(GrpcContext)
    }

    @AfterEach
    open fun tearDownTest() {
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        stopKoin()
        unmockkObject(GrpcContext)
    }
}
