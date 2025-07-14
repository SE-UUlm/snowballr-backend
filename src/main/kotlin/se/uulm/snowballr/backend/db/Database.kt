package se.uulm.snowballr.backend.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.Schema
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import se.uulm.snowballr.backend.auth.DummyUser
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.PdfTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.AuthorOfPaperTable
import se.uulm.snowballr.backend.table.association.CitationTable
import se.uulm.snowballr.backend.table.association.InvitationTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReadingListTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.association.ReviewTable
import se.uulm.snowballr.backend.table.toUser
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

// TODO: remove dummy user when user management is implemented
var dummyUserId: String? = null

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
 * @param envReader Env var reader to retrieve configuration details for the database, e.g., the password.
 */
class Database(
    private val envReader: EnvReader,
) : IDatabase {
    private val dataSource: HikariDataSource

    init {
        logger.info { "Connecting to database" }
        dataSource = initDataSource(envReader.env.database)
        transaction(Database.connect(dataSource)) {
            val schema = Schema(SCHEMA_NAME, DB_USER)
            SchemaUtils.createSchema(schema)
            SchemaUtils.setSchema(schema)
            SchemaUtils.create(
                // Non-many-to-many tables
                UserTable,
                PdfTable,
                ProjectTable,
                PaperTable,
                AuthorTable,
                CriterionTable,
                // Many-to-many tables
                ProjectPaperTable,
                AuthorOfPaperTable,
                CitationTable,
                ReadingListTable,
                ProjectMemberTable,
                InvitationTable,
                ReviewTable,
                ReviewHasCriterionTable,
            )

            useDummyUser()
            dummyUserId = DummyUser.id.toString()
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
                addDataSourceProperty("serverName", data.host)
                validate()
            }
        return HikariDataSource(config)
    }

    override suspend fun <T> dbQuery(block: suspend Transaction.() -> T): T = newSuspendedTransaction(
        Dispatchers.IO,
        Database.connect(dataSource),
        Connection.TRANSACTION_SERIALIZABLE,
    ) {
        block()
    }

    /**
     * Initializes a dummy user in the database if the environment variable is set to use a dummy user
     * and the dummy user does not already exist.
     *
     * The dummy user is created with predefined credentials and role, which can be used for testing purposes.
     */
    fun useDummyUser() {
        if (!envReader.env.miscellaneous.useDummyUser) {
            return
        }

        val existingId = UserTable
            .selectAll()
            .where { UserTable.email eq DummyUser.email }
            .map { it.toUser().id }
            .singleOrNull()

        DummyUser.id = existingId ?: UserTable.insertAndGetId {
            it[email] = DummyUser.email
            it[firstName] = DummyUser.firstName
            it[lastName] = DummyUser.lastName
            it[passwordHash] = DummyUser.passwordHash
            it[role] = DummyUser.role
            it[status] = DummyUser.status
        }.value
    }
}
