package se.uulm.snowballr.backend.model.dto.user

import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.toGrpc
import snowballr.Fetcher.FetcherOptions
import snowballr.ProjectOuterClass
import snowballr.UserSettingsOuterClass

data class UserSettingsWithCriteria(
    val settings: UserSettings,
    val criteria: List<Criterion>,
)

fun UserSettingsWithCriteria.toGrpc(): UserSettingsOuterClass.UserSettings =
    UserSettingsOuterClass.UserSettings.newBuilder()
        .setShowHotkeys(settings.areHotkeysShown)
        .setReviewMode(settings.isReviewModeEnabled)
        .setDefaultCriteria(criteria.toGrpc())
        .setDefaultProjectSettings(
            ProjectOuterClass.Project.Settings.newBuilder()
                .setSimilarityThreshold(settings.similarityThreshold)
                .setDecisionMatrix(settings.decisionMatrix.toGrpc())
                .putAllFetchers(
                    settings.fetchers.mapValues {
                        FetcherOptions
                            .newBuilder()
                            .putAllOptions(it.value)
                            .build()
                    },
                )
                .setSnowballingType(settings.snowballingType.toGrpc())
                .setReviewMaybeAllowed(settings.reviewMaybeAllowed)
                .build(),
        )
        .build()
