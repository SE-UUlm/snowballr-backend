package se.uulm.snowballr.backend.model.dto.project

import snowballr.ProjectOuterClass

/**
 * Matrix containing the paper decision based on reviews made for a paper.
 *
 * This matrix defines how a paper is decided based on its reviews.
 * For this to work, the entries need to be unique and the patterns exhaustive.
 */
data class ReviewDecisionMatrix(
    val numberOfReviewers: Int,
    val patterns: List<DecisionMatrixPattern>,
) {
    companion object {
        fun fromGrpc(decisionMatrix: ProjectOuterClass.ReviewDecisionMatrix) = ReviewDecisionMatrix(
            numberOfReviewers = decisionMatrix.numberOfReviewers,
            patterns = decisionMatrix.patternsList.map { DecisionMatrixPattern.fromGrpc(it) },
        )

        fun parseFrom(bytes: ByteArray): ReviewDecisionMatrix =
            fromGrpc(ProjectOuterClass.ReviewDecisionMatrix.parseFrom(bytes))
    }

    fun toGrpc(): ProjectOuterClass.ReviewDecisionMatrix = ProjectOuterClass.ReviewDecisionMatrix.newBuilder()
        .setNumberOfReviewers(numberOfReviewers)
        .addAllPatterns(patterns.map { it.toGrpc() })
        .build()

    fun toByteArray(): ByteArray = toGrpc().toByteArray()
}
