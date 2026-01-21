package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.fetcher.FetcherMap
import snowballr.CriterionOuterClass
import snowballr.Fetcher.FetcherOptions
import snowballr.ProjectOuterClass
import snowballr.UserSettingsOuterClass
import java.util.UUID

data class UserSettings(
    val areHotkeysShown: Boolean,
    val isReviewModeEnabled: Boolean,
    val criteriaIds: List<UUID>,
    val similarityThreshold: Float,
    val decisionMatrix: ProjectOuterClass.ReviewDecisionMatrix,
    val fetchers: FetcherMap,
    val snowballingType: ProjectOuterClass.SnowballingType,
    val reviewMaybeAllowed: Boolean,
)

/**
 * Creates a [UserSettingsOuterClass.UserSettings] from this [UserSettings].
 */
fun UserSettings.toGrpcUserSettings(
    criteria: List<CriterionOuterClass.Criterion>,
): UserSettingsOuterClass.UserSettings = UserSettingsOuterClass.UserSettings
    .newBuilder()
    .setShowHotkeys(this.areHotkeysShown)
    .setReviewMode(this.isReviewModeEnabled)
    .setDefaultCriteria(
        CriterionOuterClass.Criterion.List.newBuilder()
            .addAllCriteria(criteria)
            .build(),
    )
    .setDefaultProjectSettings(
        ProjectOuterClass.Project.Settings.newBuilder()
            .setSimilarityThreshold(this.similarityThreshold)
            .setDecisionMatrix(this.decisionMatrix)
            .putAllFetchers(
                this.fetchers.mapValues {
                    FetcherOptions
                        .newBuilder()
                        .putAllOptions(it.value)
                        .build()
                },
            )
            .setSnowballingType(this.snowballingType)
            .setReviewMaybeAllowed(this.reviewMaybeAllowed)
            .build(),
    )
    .build()
