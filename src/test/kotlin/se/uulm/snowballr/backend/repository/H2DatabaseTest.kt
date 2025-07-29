package se.uulm.snowballr.backend.repository

import io.mockk.clearAllMocks
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.table.UserTable
import snowballr.UserOuterClass
import java.sql.Connection
import java.util.UUID

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
open class H2DatabaseTest(
    val tables: Array<Table> = emptyArray(),
    val needsTestUser: Boolean = false,
) {
    private val connection = Database.connect("jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;IGNORECASE=true;")
    protected val db = TestDatabase()

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
    class TestDatabase : IDatabase {
        override suspend fun <T> dbQuery(dispatcher: CoroutineDispatcher, block: suspend Transaction.() -> T): T =
            newSuspendedTransaction(
                dispatcher,
                transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            ) {
                block()
            }
    }

    @BeforeAll
    fun setUp() {
        RepositoryHelper.db = db
    }

    @BeforeEach
    fun databaseSetUp() {
        runBlocking {
            db.dbQuery {
                SchemaUtils.create(*tables)

                // Create the user table and a test entity if requested
                if (needsTestUser) {
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
            }
        }
    }

    @AfterEach
    fun databaseTearDown() {
        runBlocking {
            db.dbQuery {
                SchemaUtils.drop(*tables)
                if (needsTestUser) {
                    SchemaUtils.drop(UserTable)
                }
            }
        }
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        TransactionManager.closeAndUnregister(connection)
    }
}
