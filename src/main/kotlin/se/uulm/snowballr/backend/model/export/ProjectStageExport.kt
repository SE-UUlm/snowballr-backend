package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectStageExport(
    @SerialName("id")
    val id: String,
    @SerialName("papers")
    val papers: List<PaperExport>,
)
