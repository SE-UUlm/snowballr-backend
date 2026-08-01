package se.uulm.snowballr.backend.matching

data class PaperMatchingConfig(
    val yearTolerance: Int,
    val titleWeight: Double,
    val authorsWeight: Double,
    val abstractWeight: Double,
)
