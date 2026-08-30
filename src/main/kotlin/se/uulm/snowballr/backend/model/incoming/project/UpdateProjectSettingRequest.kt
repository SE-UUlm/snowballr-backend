package se.uulm.snowballr.backend.model.incoming.project

import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse

data class UpdateProjectSettingRequest(
    val similarityThreshold: Float,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val fetchers: FetcherMap,
    val decisionMatrix: ReviewDecisionMatrix,
) {
    companion object {
        fun fromProject(project: Project) = UpdateProjectSettingRequest(
            similarityThreshold = project.settings.similarityThreshold,
            snowballingType = project.settings.snowballingType,
            reviewMaybeAllowed = project.settings.reviewMaybeAllowed,
            fetchers = project.settings.fetchers,
            decisionMatrix = project.settings.reviewDecisionMatrix,
        )

        fun fromProjectResponse(project: ProjectResponse) = UpdateProjectSettingRequest(
            similarityThreshold = project.settings.similarityThreshold,
            snowballingType = project.settings.snowballingType,
            reviewMaybeAllowed = project.settings.reviewMaybeAllowed,
            fetchers = project.settings.fetchers,
            decisionMatrix = project.settings.reviewDecisionMatrix,
        )
    }
}
