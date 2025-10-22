package se.uulm.snowballr.backend.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.table.PaperTable
import snowballr.PaperOuterClass.Author as GrpcAuthor

/**
 * Author DTO of [PaperTable].
 */
@Serializable
data class Author(
    @SerialName("first_name")
    val firstName: String,
    @SerialName("last_name")
    val lastName: String,
)

/**
 * Creates a [GrpcAuthor] from this [Author].
 */
fun Author.toGrpcAuthor(): GrpcAuthor = GrpcAuthor.newBuilder()
    .setFirstName(firstName)
    .setLastName(lastName)
    .build()

/**
 * Creates a list of [GrpcAuthor] from this list of [Author].
 */
fun List<Author>.toGrpcAuthors(): List<GrpcAuthor> = this.map(Author::toGrpcAuthor)
