package se.uulm.snowballr.backend.model.incoming.user

import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import java.util.UUID

data class UpdateUserSettingsRequest(
    val areHotkeysShown: Boolean,
    val isReviewModeEnabled: Boolean,
    val criteriaIds: List<UUID>,
    val similarityThreshold: Float,
    val decisionMatrix: ReviewDecisionMatrix,
    val fetchers: FetcherMap,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
)
