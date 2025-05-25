package se.uulm.snowballr.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import java.sql.Connection

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
    suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T
}

/**
 * Handles the database connection, schema initialization, and provides a mechanism for executing
 * database queries within a transactional context.
 *
 * @param data Configuration details for the database, i.e., the password.
 */
class Database(
    private val data: Env.Database,
) : IDatabase {
    private val dataSource: HikariDataSource

    init {
        logger.info { "Connecting to database" }
        dataSource = initDataSource(data)
        transaction(Database.connect(dataSource)) {
            val schema = Schema(SCHEMA_NAME, DB_USER)
            SchemaUtils.createSchema(schema)
            SchemaUtils.setSchema(schema)
            SchemaUtils.create(UserTable, ProjectTable, CriterionTable)
        }
        logger.info { "Database connection established" }
    }

    private fun initDataSource(data: Env.Database): HikariDataSource {
        val config =
            HikariConfig().apply {
                username = DB_USER
                password = data.password
                schema = SCHEMA_NAME
                transactionIsolation = ISOLATION_LEVEL.toString()
                dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
                validate()
            }
        return HikariDataSource(config)
    }

    override suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T =
        newSuspendedTransaction(
            Dispatchers.IO,
            Database.connect(dataSource),
            Connection.TRANSACTION_SERIALIZABLE,
        ) {
            block()
        }
}
