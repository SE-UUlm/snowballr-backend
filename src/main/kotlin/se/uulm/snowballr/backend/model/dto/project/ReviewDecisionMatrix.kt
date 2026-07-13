package se.uulm.snowballr.backend.model.dto.project

import kotlinx.serialization.json.Json

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
        fun parseFrom(bytes: ByteArray): ReviewDecisionMatrix =
            Json.decodeFromString<ReviewDecisionMatrix>(bytes.decodeToString())
    }

    fun toByteArray(): ByteArray = Json.encodeToString(this).toByteArray()
}
