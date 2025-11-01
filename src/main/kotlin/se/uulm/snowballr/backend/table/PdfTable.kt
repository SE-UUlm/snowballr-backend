package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import se.uulm.snowballr.backend.model.dto.Pdf

/**
 * Represents the database table "pdf" and provides a mapping for managing PDF-related data in the database.
 *
 * Columns:
 * - [data]: Represents the binary data of the PDF, stored as an [ExposedBlob].
 */
object PdfTable : UUIDTable("pdf") {
    val data = blob("data")
}

/**
 * Creates a [Pdf] from this [ResultRow].
 */
fun ResultRow.toPdf() = Pdf(
    this[PdfTable.id].value,
    this[PdfTable.data].bytes,
)
