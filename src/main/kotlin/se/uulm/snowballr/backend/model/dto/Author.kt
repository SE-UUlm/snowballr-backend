package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.AuthorTable
import snowballr.PaperOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [AuthorTable].
 */
data class Author(
    val id: UUID,
    val firstName: String,
    val lastName: String,
    val orcid: String?,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)

/**
 * Creates a [PaperOuterClass.Author] from this [Author].
 */
fun Author.toGrpcAuthor(): PaperOuterClass.Author = with(
    PaperOuterClass.Author
        .newBuilder(),
) {
    setFirstName(this.firstName)
    setLastName(this.lastName)
    this.orcid?.let { setOrcid(it) }
    build()
}
