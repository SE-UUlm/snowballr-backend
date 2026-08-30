package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper

/**
 * Settings DTO of a [Project].
 */
data class ProjectSettings(
    /**
     * The threshold that a similarity between two [FetcherPaper]s must have to be considered equal.
     */
    val similarityThreshold: Float,
    /**
     * The type of snowballing that is used by the [Project].
     */
    val snowballingType: SnowballingType,
    /**
     * Whether [ReviewDecision.MAYBE] is allowed as decision for a [Review].
     */
    val reviewMaybeAllowed: Boolean,
    /**
     * The matrix that is used to calculate the [PaperDecision] for a [ProjectPaper].
     */
    val reviewDecisionMatrix: ReviewDecisionMatrix,
    /**
     * The fetchers (and their options) that are used by the [Project] in the fetcher orchestration process.
     */
    val fetchers: FetcherMap,
)
