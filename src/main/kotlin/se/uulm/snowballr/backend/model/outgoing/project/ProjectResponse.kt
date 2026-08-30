package se.uulm.snowballr.backend.model.outgoing.project

import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import java.time.OffsetDateTime
import java.util.UUID

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val status: ProjectStatus,
    val currentStage: Int,
    val maxStage: Int,
    val settings: ProjectSettings,
    val currentStageStartedAt: OffsetDateTime,
) {
    companion object {
        fun fromProject(project: Project) = ProjectResponse(
            id = project.id,
            name = project.name,
            status = project.status,
            currentStage = project.currentStage,
            maxStage = project.maxStage,
            settings = project.settings,
            currentStageStartedAt = project.currentStageStartedAt,
        )
    }
}
