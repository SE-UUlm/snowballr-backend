package se.uulm.snowballr.backend.repository

import io.mockk.clearAllMocks
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import se.uulm.snowballr.backend.db.IDatabase
import java.sql.Connection

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
 * @ExperimentalCoroutinesApi
 * @DelicateCoroutinesApi
 * class ExampleTest : H2DatabaseTest(arrayOf(ExampleTable)) {
 *     private val repo = ExampleTableRepo(db)
 *
 *     @Nested
 *     inner class CreateExample {
 *         @Test
 *         fun `When an example is created, then the passed values are correctly assigned`() =
 *             testCoroutine {
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
 */
@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
open class H2DatabaseTest(
    val tables: Array<Table> = emptyArray(),
) {
    private val threadContext = newSingleThreadContext("Coroutine thread")
    private val connection = Database.connect("jdbc:h2:mem:test_db;DB_CLOSE_DELAY=-1;IGNORECASE=true;")
    protected val db = TestDatabase()

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
        override suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
            newSuspendedTransaction(
                Dispatchers.IO,
                transactionIsolation = Connection.TRANSACTION_SERIALIZABLE,
            ) {
                block()
            }
    }

    @BeforeAll
    fun setUp() {
        Dispatchers.setMain(threadContext)
    }

    @BeforeEach
    fun databaseSetUp() {
        runBlocking {
            db.dbQuery {
                SchemaUtils.create(*tables)
            }
        }
    }

    @AfterEach
    fun databaseTearDown() {
        runBlocking {
            db.dbQuery {
                SchemaUtils.drop(*tables)
            }
        }
        clearAllMocks()
    }

    @AfterAll
    fun tearDown() {
        TransactionManager.closeAndUnregister(connection)
        Dispatchers.resetMain()
        threadContext.close()
    }
}
