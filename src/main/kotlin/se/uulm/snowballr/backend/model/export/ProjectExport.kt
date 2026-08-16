package se.uulm.snowballr.backend.model.export

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProjectExport(
    @SerialName("name")
    val name: String,
    @SerialName("members")
    val members: List<ProjectMemberExport>,
    @SerialName("stages")
    val stages: List<ProjectStageExport>,
    @SerialName("criteria")
    val criteria: List<CriterionExport>,
    @SerialName("created_at")
    val createdAt: String,
)
