package se.uulm.snowballr.backend.model.outgoing.project

import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import java.time.OffsetDateTime
import java.util.UUID

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val status: ProjectStatus,
    val currentStage: Int,
    val maxStage: Int,
    val similarityThreshold: Float,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val reviewDecisionMatrix: ReviewDecisionMatrix,
    val fetchers: FetcherMap,
    val currentStageStartedAt: OffsetDateTime,
) {
    companion object {
        fun fromProject(project: Project) = ProjectResponse(
            id = project.id,
            name = project.name,
            status = project.status,
            currentStage = project.currentStage,
            maxStage = project.maxStage,
            similarityThreshold = project.similarityThreshold,
            snowballingType = project.snowballingType,
            reviewMaybeAllowed = project.reviewMaybeAllowed,
            reviewDecisionMatrix = project.reviewDecisionMatrix,
            fetchers = project.fetchers,
            currentStageStartedAt = project.currentStageStartedAt,
        )
    }
}
