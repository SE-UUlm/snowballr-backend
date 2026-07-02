package se.uulm.snowballr.backend.model.dto.paper

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.table.PaperTable

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
