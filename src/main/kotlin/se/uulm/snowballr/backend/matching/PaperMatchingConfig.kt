package se.uulm.snowballr.backend.matching

data class PaperMatchingConfig(
    val yearTolerance: Int,
    val titleWeight: Double,
    val authorsWeight: Double,
    val abstractWeight: Double,
) {
    init {
        val weightSum = titleWeight + authorsWeight + abstractWeight
        require(weightSum > 0.0) { "The sum of the weights must be positive." }
        require(yearTolerance >= 0) { "The year tolerance must be positive or 0." }
    }
}
