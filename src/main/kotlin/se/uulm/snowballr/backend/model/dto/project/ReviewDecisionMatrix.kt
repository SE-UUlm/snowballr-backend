package se.uulm.snowballr.backend.model.dto.project

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Matrix containing the paper decision based on reviews made for a paper.
 *
 * This matrix defines how a paper is decided based on its reviews.
 * For this to work, the entries need to be unique and the patterns exhaustive.
 */
@Serializable
data class ReviewDecisionMatrix(
    @SerialName("number_of_reviewers")
    val numberOfReviewers: Int,
    @SerialName("patterns")
    val patterns: List<DecisionMatrixPattern>,
) {
    companion object {
        fun parseFrom(bytes: ByteArray): ReviewDecisionMatrix =
            Json.decodeFromString<ReviewDecisionMatrix>(bytes.decodeToString())
    }

    fun toByteArray(): ByteArray = Json.encodeToString(this).toByteArray()
}
