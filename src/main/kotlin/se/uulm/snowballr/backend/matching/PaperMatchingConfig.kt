package se.uulm.snowballr.backend.matching

data class PaperMatchingConfig(
    val yearTolerance: Int,
    val titleWeight: Double,
    val authorsWeight: Double,
    val abstractWeight: Double,
    val titleMinSimilarity: Double,
    val abstractMinSimilarity: Double,
    val authorsMinSimilarity: Double,
) {
    init {
        require(yearTolerance >= 0) { "The year tolerance must be positive or 0." }
        require(titleWeight >= 0) { "titleWeight must be positive or 0" }
        require(authorsWeight >= 0) { "authorsWeight must be positive or 0" }
        require(abstractWeight >= 0) { "abstractWeight must be positive or 0" }
        val weightSum = titleWeight + authorsWeight + abstractWeight
        require(weightSum > 0.0) { "The sum of the weights must be positive." }
        require(titleMinSimilarity >= 0) { "titleMinSimilarity must be positive or 0." }
        require(abstractMinSimilarity >= 0) { "abstractMinSimilarity must be positive or 0." }
        require(authorsMinSimilarity >= 0) { "authorsMinSimilarity must be positive or 0." }
    }
}
