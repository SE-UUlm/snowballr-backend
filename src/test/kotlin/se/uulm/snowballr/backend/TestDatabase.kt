package se.uulm.snowballr.backend

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.testcontainers.postgresql.PostgreSQLContainer
import se.uulm.snowballr.backend.db.DatabaseHelper
import se.uulm.snowballr.backend.db.DatabaseHelper.addExtensions
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.table.UserTable
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.Database as JdbcDatabase

/**
 * A test database implementation that uses Testcontainers to manage a PostgreSQL instance.
 * This class provides a convenient way to perform database operations in tests, allowing
 * for both blocking and non-blocking queries using coroutines.
 *
 * The [setUp] method initializes the PostgreSQL container and configures the data source
 * for database connections. The [query] method allows executing database transactions
 * within a specified coroutine dispatcher, while the [queryBlocking] method provides
 * a blocking interface for executing transactions.
 */
class TestDatabase : IDatabase {
    private val postgres = PostgreSQLContainer("postgres:16.1-alpine3.19")
    private lateinit var dataSource: HikariDataSource
    private lateinit var exposedDatabase: JdbcDatabase

    override suspend fun <T> query(
        dispatcher: CoroutineDispatcher,
        transactionIsolation: Int,
        block: suspend JdbcTransaction.() -> T,
    ): T = withContext(dispatcher) {
        suspendTransaction(exposedDatabase, transactionIsolation = transactionIsolation) {
            block()
        }
    }

    /**
     * Starts the PostgreSQL container and sets up the data source for database connections.
     *
     * Use this method in the @BeforeAll setup method.
     */
    fun setUp() {
        postgres.start()
        val config = HikariConfig().apply {
            jdbcUrl = postgres.jdbcUrl
            username = postgres.username
            password = postgres.password
        }
        dataSource = HikariDataSource(config)
        exposedDatabase = JdbcDatabase.connect(dataSource)
    }

    /**
     * Sets up the database schema by adding necessary extensions and tables. If [needsTestUser] is true,
     * it also initializes a test user in the database, which can be used for authentication and authorization tests.
     * The ID of the test user is passed to the provided [getTestUserId] callback for use in tests.
     *
     * Use this method in the @BeforeEach setup method, passing the required tables and whether a test user is needed.
     */
    fun setUpTest(tables: Array<Table>? = null, needsTestUser: Boolean, getTestUserId: (UUID) -> Unit) {
        queryBlocking {
            addExtensions()
            addAllTables(tables)

            if (needsTestUser) {
                val id = initTestUser()
                getTestUserId(id)
            }
        }
    }

    /**
     * Cleans up the database schema by dropping the specified tables.
     *
     * Use this method in the @AfterEach teardown method, passing the tables to be dropped.
     */
    fun tearDownTest(tables: Array<Table>? = null) {
        queryBlocking {
            dropAllTables(tables)
            // This call seems to provoke "ERROR: cache lookup failed for type 16386"
            // PSQLException errors in the test suite. Leaving this commented out for now.
            // removeExtensions()
        }
    }

    /**
     * Stops the PostgreSQL container, cleaning up any resources used during testing.
     *
     * Use this method in the @AfterAll teardown method.
     */
    fun tearDown() {
        postgres.stop()
    }

    private fun addAllTables(tables: Array<Table>? = null) {
        if (tables == null) {
            DatabaseHelper.addAllTables()
        } else {
            DatabaseHelper.addAllTables(tables)
        }
    }

    private fun dropAllTables(tables: Array<Table>? = null) {
        if (tables == null) {
            DatabaseHelper.dropAllTables()
        } else {
            DatabaseHelper.dropAllTables(tables)
        }
    }

    private fun initTestUser(): UUID {
        // Create the test user
        val userId =
            UserTable.insertAndGetId {
                it[email] = "test.user@example.com"
                it[firstName] = "Test"
                it[lastName] = "User"
                it[passwordHash] = "hashedPassword"
                it[role] = UserRole.USER_ROLE_ADMIN
                it[status] = UserStatus.USER_STATUS_ACTIVE
            }
        return userId.value
    }

    private fun <T> queryBlocking(block: suspend JdbcTransaction.() -> T): T = runBlocking {
        query {
            block()
        }
    }

    override fun close() {
        dataSource.close()
    }
}
