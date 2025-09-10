package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.model.dto.Paper
import java.time.OffsetDateTime

/**
 * Represents the database table "paper" and provides a mapping for managing paper-related entities in the database.
 *
 * Columns:
 * - [title]: Represents the title of the paper as a [String].
 * - [externalId]: Represents an optional unique external identifier of the paper as a nullable [String].
 * - [abstract]: Represents the abstract of the paper as a [String].
 * - [year]: Represents the publication year of the paper as an [Int].
 * - [publisher]: Represents the publisher of the paper as a nullable [String].
 * - [publicationType]: Represents the type of publication as a nullable [String].
 * - [publicationName]: Represents the name of the publication where the paper is published, as a nullable [String].
 * - [pdfId]: Represents a reference to the [PdfTable] where the PDF data for the paper is stored.
 * - [createdAt]: Represents the timestamp of when the paper was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the paper was last modified as an [OffsetDateTime].
 * - [modifiedBy]: A foreign key referencing the user table, representing the user who last modified the paper.
 */
object PaperTable : UUIDTable("paper") {
    val title = text("title")
    val externalId = text("external_id").uniqueIndex().nullable()
    val abstract = text("abstract")
    val year = integer("year")
    val publisher = text("publisher")
    val publicationType = text("publication_type")
    val publicationName = text("publication_name")

    /**
     * Optional reference to the PDF of the paper.
     *
     * - `onDelete=SET_NULL` so that when the PDF is deleted, the reference is set to null
     * - `onUpdate=CASCADE` so that when the PDF ID is updated, the foreign key ID is updated too
     */
    val pdfId = optReference("pdf_id", PdfTable, ReferenceOption.SET_NULL, ReferenceOption.CASCADE)

    // Metadata

    val fetcherMetadata = stringMap("fetcher_metadata")
    val createdAt = createdAt()
    val modifiedAt = modifiedAt()
    val modifiedBy = modifiedBy()
}

/**
 * Creates a [Paper] from this [ResultRow].
 */
fun ResultRow.toPaper() = Paper(
    id = this[PaperTable.id].value,
    title = this[PaperTable.title],
    externalId = this[PaperTable.externalId],
    abstract = this[PaperTable.abstract],
    year = this[PaperTable.year],
    publisher = this[PaperTable.publisher],
    publicationType = this[PaperTable.publicationType],
    publicationName = this[PaperTable.publicationName],
    pdfId = this[PaperTable.pdfId]?.value,
    fetcherMetadata = this[PaperTable.fetcherMetadata],
    createdAt = this[PaperTable.createdAt],
    modifiedAt = this[PaperTable.modifiedAt],
    modifiedBy = this[PaperTable.modifiedBy]?.value,
)
