package se.uulm.snowballr.backend.repository

import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import org.jetbrains.exposed.v1.core.Table
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import se.uulm.snowballr.backend.TestDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.env.IEnvService
import se.uulm.snowballr.backend.mockEnvWithDefaultValues
import se.uulm.snowballr.backend.table.UserTable
import java.util.UUID

/**
 * Base class for unit tests that interact with a test PostgreSQL database.
 * This class manages the lifecycle of the database and provides setup
 * for database schema creation and teardown for schema cleanup between tests.
 *
 * The class leverages [TestDatabase] to provide a suspension-friendly way
 * to perform database transactions during tests. For an example see [ProjectTableRepoTest].
 *
 * Example usage:
 * ```kotlin
 * class ExampleTest : RepositoryTest(arrayOf(ExampleTable)) {
 *     private val repo = ExampleTableRepo(db)
 *
 *     @Nested
 *     inner class CreateExample {
 *         @Test
 *         fun `When an example is created, then the passed values are correctly assigned`() =
 *             runTest {
 *                 // Use the repo property to perform database operations within a coroutine context
 *                 val request = ExampleOuterClass.Example.Create.newBuilder()
 *                     .setName("Test Example")
 *                     .build()
 *                 val example = repo.createExample(request)
 *
 *                 // Assert that the example was created correctly
 *                 assertEquals("Test Example", example.name)
 *             }
 *     }
 * }
 * ```
 *
 * @property tables An array of database tables to be managed during the test lifecycle.
 * @property needsTestUser Whether a test user is required, which can be used in a test in the form of [testUserId].
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class RepositoryTest(
    val tables: Array<Table> = emptyArray(),
    val needsTestUser: Boolean = false,
) {
    // Initialize DB with empty dataSource only to set it in the setUp method
    protected val db = TestDatabase(HikariDataSource())

    // Environment dependencies
    private val envServiceMock = mockk<IEnvService>()
    protected val envReaderMock = mockk<EnvReader>()

    private val repositoryTestModule = module {
        // Environment dependencies
        single { envServiceMock }
        single { envReaderMock }

        // Mock env variables
        every { envReaderMock.env } returns mockEnvWithDefaultValues()
    }

    /** User for testing. This prevents having to create a user for each test. */
    protected var testUserId: UUID = UUID.randomUUID()

    private fun getTestTables() = if (needsTestUser) arrayOf(*tables, UserTable) else tables

    @BeforeAll
    fun setUp() {
        db.setUp()
        RepositoryHelper.db = db
        startKoin {
            modules(repositoryTestModule)
        }
    }

    @BeforeEach
    fun setUpTest() {
        db.setUpTest(getTestTables(), needsTestUser) { testUserId = it }
    }

    @AfterEach
    fun tearDownTest() {
        db.tearDownTest(getTestTables())
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        db.tearDown()
        stopKoin()
    }
}
