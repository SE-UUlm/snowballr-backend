package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory

@Serializable
data class CriterionExport(
    @SerialName("id")
    val id: String,
    @SerialName("tag")
    val tag: String,
    @SerialName("name")
    val name: String,
    @SerialName("description")
    val description: String,
    @SerialName("category")
    val category: CriterionCategory,
)
