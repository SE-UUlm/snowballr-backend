package se.uulm.snowballr.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.Schema
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import se.uulm.snowballr.backend.auth.DummyUser
import se.uulm.snowballr.backend.db.DatabaseHelper.addExtensions
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.table.UserTable
import java.sql.Connection
import org.jetbrains.exposed.v1.jdbc.Database as JdbcDatabase

private val logger = KotlinLogging.logger { }

/**
 * The default transaction isolation level used for database operations.
 */
private const val ISOLATION_LEVEL = Connection.TRANSACTION_SERIALIZABLE

/**
 * The name of the database schema that is used for storing all SnowballR data.
 */
private const val SCHEMA_NAME = "snowballr"

/**
 * Username used for database connections.
 */
private const val DB_USER = "postgres"

/**
 * Interface defining a database abstraction layer for executing queries within transactional contexts.
 *
 * This interface provides a method for running database operations inside a coroutine-compatible
 * transactional block. It abstracts the lower-level details of connecting to the database and
 * managing transaction lifecycles, allowing for simpler and more testable database interactions.
 */
interface IDatabase {
    suspend fun <T> query(
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        transactionIsolation: Int = Connection.TRANSACTION_SERIALIZABLE,
        block: suspend JdbcTransaction.() -> T,
    ): T
}

/**
 * Handles the database connection, schema initialization, and provides a mechanism for executing
 * database queries within a transactional context.
 *
 * @param envReader Env var reader to retrieve configuration details for the database, e.g., the password.
 */
class Database(
    private val envReader: EnvReader,
) : IDatabase {
    private val dataSource: HikariDataSource

    init {
        logger.info { "Connecting to database" }
        dataSource = initDataSource(envReader.env.database)
        transaction(JdbcDatabase.connect(dataSource)) {
            setUpDatabase()
            seedDummyUserIfEnabled()
        }
        logger.info { "Database connection established" }
    }

    private fun JdbcTransaction.setUpDatabase() {
        // Schema
        val schema = Schema(SCHEMA_NAME, DB_USER)
        SchemaUtils.createSchema(schema)
        SchemaUtils.setSchema(schema)

        // Extensions
        addExtensions()

        // Tables
        DatabaseHelper.addAllTables()
    }

    private fun initDataSource(data: Env.Database): HikariDataSource {
        val config =
            HikariConfig().apply {
                username = DB_USER
                password = data.password
                schema = SCHEMA_NAME
                transactionIsolation = ISOLATION_LEVEL.toString()
                dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
                addDataSourceProperty("serverName", data.host)
                validate()
            }
        return HikariDataSource(config)
    }

    override suspend fun <T> query(
        dispatcher: CoroutineDispatcher,
        transactionIsolation: Int,
        block: suspend JdbcTransaction.() -> T,
    ): T = withContext(dispatcher) {
        suspendTransaction(
            JdbcDatabase.connect(dataSource),
            transactionIsolation,
        ) {
            block()
        }
    }

    /**
     * Seeds the database with a dummy user if the environment configuration requires it.
     *
     * This method checks the `seedUserEnabled` flag. If true, it ensures the dummy user exists.
     * If false, it ensures the dummy user is deleted, keeping the database clean.
     */
    private fun seedDummyUserIfEnabled() {
        val seedUserEnabled = envReader.env.database.seedUserEnabled

        val existentId = UserTable
            .select(UserTable.id)
            .where { UserTable.email eq DummyUser.email }
            .map { it[UserTable.id].value }
            .singleOrNull()

        if (seedUserEnabled) {
            if (existentId == null) {
                // If seeding is enabled and a user doesn't exist, create it.
                DummyUser.id = UserTable.insertAndGetId {
                    it[email] = DummyUser.email
                    it[firstName] = DummyUser.firstName
                    it[lastName] = DummyUser.lastName
                    it[passwordHash] = DummyUser.passwordHash
                    it[role] = DummyUser.role
                    it[status] = DummyUser.status
                }.value
                logger.info { "Dummy User seeded with ID: ${DummyUser.id}" }
            } else {
                // If the user already exists, update the static ID.
                DummyUser.id = existentId
                logger.info { "Dummy User already exists with ID: ${DummyUser.id}" }
            }
        } else if (existentId != null) {
            // If seeding is disabled and a user exists, delete it.
            UserTable.deleteWhere { UserTable.id eq existentId }
            logger.info { "Dummy user deleted as seeding is disabled." }
        }
    }
}
