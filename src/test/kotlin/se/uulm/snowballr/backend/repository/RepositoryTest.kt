package se.uulm.snowballr.backend.repository

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.mockk.clearAllMocks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.testcontainers.containers.PostgreSQLContainer
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.table.UserTable
import snowballr.UserOuterClass
import java.sql.Connection
import java.util.UUID
import javax.sql.DataSource

/**
 * Base class for unit tests that interact with the in-memory H2 database.
 * This class manages the lifecycle of the database and provides setup
 * for database schema creation and teardown for schema cleanup between tests.
 *
 * The class leverages [TestDatabase] to provide a suspension-friendly way
 * to perform database transactions during tests. For an example see [ProjectTableRepoTest].
 *
 * Example usage:
 * ```kotlin
 * class ExampleTest : H2DatabaseTest(arrayOf(ExampleTable)) {
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
 *                 assertThat(example.name).isEqualTo("Test Example")
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
    private val postgres = PostgreSQLContainer<Nothing>("postgres:16.1-alpine3.19")

    /** User for testing. This prevents having to create a user for each test. */
    protected var testUserId: UUID = UUID.randomUUID()

    /**
     * Implementation of [IDatabase] providing a suspension-friendly context for database transactions.
     *
     * This class uses the [newSuspendedTransaction] function to execute database operations
     * within a coroutine context, ensuring that all interactions occur within a transactional scope.
     *
     * Transactions executed by this class are performed with a [Dispatchers.IO] context
     * and use the [Connection.TRANSACTION_SERIALIZABLE] isolation level to ensure data consistency.
     */
    protected class TestDatabase(var dataSource: DataSource) : IDatabase {
        override suspend fun <T> query(dispatcher: CoroutineDispatcher, block: suspend Transaction.() -> T): T =
            newSuspendedTransaction(
                dispatcher,
                Database.connect(dataSource),
                transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            ) {
                block()
            }

        fun <T> queryBlocking(block: suspend Transaction.() -> T): T = runBlocking {
            query {
                block()
            }
        }
    }

    @BeforeAll
    fun setUp() {
        postgres.start()
        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }
        db.dataSource = HikariDataSource(config)
        RepositoryHelper.db = db
    }

    @BeforeEach
    fun setUpTest() {
        db.queryBlocking {
            SchemaUtils.create(*tables)

            if (needsTestUser) {
                initTestUser()
            }
        }
    }

    private fun initTestUser() {
        SchemaUtils.create(UserTable)
        // Create the test user
        val userId =
            UserTable.insertAndGetId {
                it[email] = "test.user@example.com"
                it[firstName] = "Test"
                it[lastName] = "User"
                it[passwordHash] = "hashedPassword"
                it[role] = UserOuterClass.UserRole.USER_ROLE_ADMIN
                it[status] = UserOuterClass.UserStatus.USER_STATUS_ACTIVE
            }
        testUserId = userId.value
    }

    @AfterEach
    fun tearDownTest() {
        db.queryBlocking {
            SchemaUtils.drop(*tables)
            if (needsTestUser) {
                SchemaUtils.drop(UserTable)
            }
        }
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        postgres.stop()
    }
}
