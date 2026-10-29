package se.uulm.snowballr.backend.model.incoming.user

import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.user.User
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
) {
    companion object {
        fun fromUser(user: User) = UpdateUserSettingsRequest(
            areHotkeysShown = user.settings.areHotkeysShown,
            isReviewModeEnabled = user.settings.isReviewModeEnabled,
            criteriaIds = user.settings.criteriaIds,
            similarityThreshold = user.settings.defaultProjectSettings.similarityThreshold,
            decisionMatrix = user.settings.defaultProjectSettings.reviewDecisionMatrix,
            fetchers = user.settings.defaultProjectSettings.fetchers,
            snowballingType = user.settings.defaultProjectSettings.snowballingType,
            reviewMaybeAllowed = user.settings.defaultProjectSettings.reviewMaybeAllowed,
        )
    }
}
