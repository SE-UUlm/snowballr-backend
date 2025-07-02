package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.model.dto.Author
import java.time.OffsetDateTime

/**
 * Represents the database table "author" and provides a mapping for managing author-related entities in the database.
 *
 * Columns:
 * - [firstName]: Represents the first name of the author as [String].
 * - [lastName]: Represents the last name of the author as [String].
 * - [orcid]: Represents the ORCID identifier of the author as a nullable [String] (see https://orcid.org/).
 * - [createdAt]: Represents the timestamp of when the author was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the author was last modified as an [OffsetDateTime].
 */
object AuthorTable : UUIDTable("author") {
    val firstName = text("first_name")
    val lastName = text("last_name")
    val orcid = text("orcid").nullable()

    // Metadata

    val createdAt = createdAt()
    val modifiedAt = modifiedAt()

    // Methods

    /**
     * Creates an [Author] from this [ResultRow].
     */
    fun ResultRow.toAuthor() = Author(
        id = this[id].value,
        firstName = this[firstName],
        lastName = this[lastName],
        orcid = this[orcid],
        createdAt = this[createdAt],
        modifiedAt = this[modifiedAt],
    )
}
