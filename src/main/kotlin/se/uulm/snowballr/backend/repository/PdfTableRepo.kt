package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Pdf
import se.uulm.snowballr.backend.table.PdfTable
import se.uulm.snowballr.backend.table.toPdf
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [PdfTable].
 *
 * This interface provides abstraction for handling persistence and retrieval operations for PDFs. By using this
 * interface, the functionality for managing PDFs can remain decoupled from the specifics of the database layer.
 */
interface IPdfTableRepo {
    /**
     * Returns a [Result] containing the PDF by its ID or a [se.uulm.snowballr.backend.model.SnowballRException.NotFoundException]
     * if the PDF with the passed [id] doesn't exist.
     */
    suspend fun getPdfById(id: UUID): Result<Pdf>

    /**
     * Creates a new PDF in the database with the provided data.
     *
     * @param data The binary data of the PDF.
     * @return The created [Pdf] entity.
     */
    suspend fun createPdf(data: ByteArray): Pdf

    /**
     * Deletes a PDF by its ID.
     *
     * @param id The ID of the PDF to delete.
     */
    suspend fun deletePdfById(id: UUID)
}

/**
 * Repository implementation for managing the [PdfTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for PDFs by leveraging the
 * database abstraction defined in [IDatabase]. It facilitates CRUD operations on PDFs and ensures
 * database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class PdfTableRepo(
    private val db: IDatabase,
) : IPdfTableRepo {
    private fun getPdfByIdOrNull(id: UUID): Pdf? = PdfTable.getEntityByIdOrNull(id, ResultRow::toPdf)

    override suspend fun getPdfById(id: UUID): Result<Pdf> = db.query {
        getEntityByKeyAsResult(::getPdfByIdOrNull, EntityType.PDF, id)
    }

    override suspend fun createPdf(data: ByteArray): Pdf = db.query {
        PdfTable.insertAndGet(ResultRow::toPdf, EntityType.PDF) {
            it[PdfTable.data] = ExposedBlob(data)
        }
    }

    override suspend fun deletePdfById(id: UUID) {
        db.query {
            PdfTable.deleteWhere { PdfTable.id eq id }
        }
    }
}
