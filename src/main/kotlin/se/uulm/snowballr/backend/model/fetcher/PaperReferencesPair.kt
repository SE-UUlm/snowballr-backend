package se.uulm.snowballr.backend.model.fetcher

import se.uulm.snowballr.backend.model.dto.paper.Paper

/**
 * Represents a pair of backward and forward references of type [T].
 */
data class PaperReferencesPair<T>(
    val backwardRefs: Set<T>,
    val forwardRefs: Set<T>,
) {
    val allRefs get() = backwardRefs + forwardRefs
}

/**
 * Pair of refs as [FetcherPaper]s.
 */
typealias FetchingResults = PaperReferencesPair<FetcherPaper>

/**
 * Pair of refs as [Paper]s.
 */
typealias PaperCreationResults = PaperReferencesPair<Paper>
