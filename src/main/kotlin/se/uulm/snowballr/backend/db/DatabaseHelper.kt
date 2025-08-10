package se.uulm.snowballr.backend.db

import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.Transaction
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.PdfTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.table.association.AuthorOfPaperTable
import se.uulm.snowballr.backend.table.association.CitationTable
import se.uulm.snowballr.backend.table.association.InvitationTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReadingListTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.association.ReviewTable

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
        AuthorTable,
        CriterionTable,
        VerificationTokenTable,
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

    /**
     * Adds all required extensions for DB to work.
     */
    fun Transaction.addExtensions() {
        exec("CREATE EXTENSION IF NOT EXISTS hstore;")
        exec("CREATE EXTENSION IF NOT EXISTS pg_trgm;")
    }

    /**
     * Removes all extensions.
     */
    fun Transaction.removeExtensions() {
        exec("DROP EXTENSION IF EXISTS hstore;")
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
