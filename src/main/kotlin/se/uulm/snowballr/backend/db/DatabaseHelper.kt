package se.uulm.snowballr.backend.db

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.jdbc.JdbcTransaction
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.PdfTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.table.association.CitationTable
import se.uulm.snowballr.backend.table.association.PaperHasExternalIdTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReadingListTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable

/**
 * Helper for the [Database] to manage all tables and extensions.
 */
@Suppress("SpreadOperator")
object DatabaseHelper {
    private val allTables: Array<Table> = arrayOf(
        // Non-many-to-many tables
        UserTable,
        PdfTable,
        ProjectTable,
        PaperTable,
        CriterionTable,
        VerificationTokenTable,
        InvitationTokenTable,
        // Many-to-many tables
        ProjectPaperTable,
        CitationTable,
        ReadingListTable,
        ProjectMemberTable,
        ReviewTable,
        ReviewHasCriterionTable,
        PaperHasExternalIdTable,
    )

    /**
     * Adds all required extensions for DB to work.
     */
    fun JdbcTransaction.addExtensions() {
        exec("CREATE EXTENSION IF NOT EXISTS hstore;")
        exec("CREATE EXTENSION IF NOT EXISTS pg_trgm;")
    }

    /**
     * Removes all extensions.
     */
    fun JdbcTransaction.removeExtensions() {
        exec("DROP EXTENSION IF EXISTS hstore;")
        exec("DROP EXTENSION IF EXISTS pg_trgm;")
    }

    /**
     * Adds all SnowballR tables to the current DB.
     */
    fun addAllTables(tables: Array<Table> = allTables) = SchemaUtils.create(*tables)

    /**
     * Drops all SnowballR tables from the current DB.
     */
    fun dropAllTables(tables: Array<Table> = allTables) = SchemaUtils.drop(*tables)
}
