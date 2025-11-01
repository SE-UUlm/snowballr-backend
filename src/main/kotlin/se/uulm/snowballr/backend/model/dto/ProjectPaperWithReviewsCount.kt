package se.uulm.snowballr.backend.model.dto

/**
 * Composite class that contains a [ProjectPaper] and the number of [Review]s that have been given to it.
 */
data class ProjectPaperWithReviewsCount(
    val projectPaper: ProjectPaper,
    val reviewsCount: Int,
) : Comparable<ProjectPaperWithReviewsCount> {
    /**
     * Compares this [ProjectPaperWithReviewsCount] to another [ProjectPaperWithReviewsCount].
     *
     * The comparison is done in the following order:
     * 1. By [ProjectPaper.stage] in ascending order
     * 2. By [reviewsCount] in ascending order
     */
    override fun compareTo(other: ProjectPaperWithReviewsCount): Int {
        val compareByStage = projectPaper.stage.compareTo(other.projectPaper.stage)

        if (compareByStage != 0) {
            return compareByStage
        }

        return reviewsCount.compareTo(other.reviewsCount)
    }
}
