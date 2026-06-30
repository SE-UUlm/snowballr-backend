package se.uulm.snowballr.backend.model.incoming.project

import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.fetcher.FetcherMap

data class UpdateProjectSettingRequest(
    val similarityThreshold: Float,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val fetchers: FetcherMap,
    val decisionMatrix: ReviewDecisionMatrix,
) {
    companion object {
        fun fromProject(project: Project) = UpdateProjectSettingRequest(
            similarityThreshold = project.similarityThreshold,
            snowballingType = project.snowballingType,
            reviewMaybeAllowed = project.reviewMaybeAllowed,
            fetchers = project.fetchers,
            decisionMatrix = project.reviewDecisionMatrix,
        )
    }
}
