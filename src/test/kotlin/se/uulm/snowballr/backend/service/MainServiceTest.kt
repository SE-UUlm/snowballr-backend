package se.uulm.snowballr.backend.service

import io.mockk.clearAllMocks
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.criterion.CreateCriterionTest

/**
 * Unit test class for the [MainService] class.
 *
 * This test class provides the setup and teardown logic needed to test [MainService] effectively in a
 * controlled coroutine environment. It leverages dependency mocking for all repositories to isolate the service and
 * test its functionality without reliance on external systems.
 *
 * An extension of this class can be used to implement specific test cases for the sub-services of
 * [MainService], such as [CreateCriterionTest].
 *
 * Example usage:
 * ```kotlin
 * @ExperimentalCoroutinesApi
 * @DelicateCoroutinesApi
 * class CreateExampleTest : MainServiceTest() {
 *     @Test
 *     fun `When an example is correctly created, then no exception is thrown`() =
 *         testCoroutine {
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
 *         testCoroutine {
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
@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class MainServiceTest {
    private val threadContext = newSingleThreadContext("Test thread")

    val projectRepoMock = mockk<IProjectTableRepo>(relaxed = true)
    val criterionRepoMock = mockk<ICriterionTableRepo>(relaxed = true)
    val userRepoMock = mockk<IUserTableRepo>(relaxed = true)
    val projectMemberRepoMock = mockk<IProjectMemberTableRepo>(relaxed = true)
    val jwtServiceMock = mockk<IJwtService>(relaxed = true)
    val mainService =
        MainService(
            projectRepoMock,
            criterionRepoMock,
            userRepoMock,
            projectMemberRepoMock,
            jwtServiceMock,
        )

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(threadContext)
        mockkObject(GrpcContext)
    }

    @AfterEach
    open fun tearDownTest() {
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
        threadContext.close()
        unmockkObject(GrpcContext)
    }
}
