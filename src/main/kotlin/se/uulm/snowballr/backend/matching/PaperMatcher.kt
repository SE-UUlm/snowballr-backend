package se.uulm.snowballr.backend.matching

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.math.abs

private val logger = KotlinLogging.logger { }

interface IPaperMatcher {
    /**
     * The [PaperMatchingConfig] used by the matcher.
     */
    val config: PaperMatchingConfig

    /**
     * Returns a similarity score in [0.0, 1.0] between two [FetcherPaper]s.
     *
     * Scoring rules:
     * - If both papers share at least one [ExternalId], returns 1.0 immediately.
     * - If both papers have at least one conflicting [ExternalId], returns 0.0 immediately.
     *   - A conflict means same type, but different value.
     * - If the publication years differ by more than the configured threshold, returns 0.0 immediately.
     * - Otherwise, the score is a weighted combination of:
     *   - **Title**: normalized Levenshtein similarity.
     *   - **Authors**: Jaccard similarity on author name tokens. Omitted when either list is empty.
     *   - **Abstract**: normalized Levenshtein similarity. Omitted when either abstract is blank.
     *   Weights are renormalized to sum to 1 after any components are dropped.
     */
    fun similarity(a: FetcherPaper, b: FetcherPaper): Double

    /**
     * Deduplicates a set of [FetcherPaper]s using greedy grouping.
     *
     * Papers are iterated in set order. Each paper is placed into the first existing group whose
     * representative (first member) has a [similarity] score >= [threshold] with it. If no such
     * group exists, a new group is started. Each group is then merged into a single [FetcherPaper]
     * where the first member's field values take precedence and metadata is union-merged with the
     * first member winning on key conflicts.
     *
     * @param papers the input papers, possibly containing duplicates.
     * @param threshold minimum [similarity] score for two papers to be considered duplicates.
     * @return a deduplicated set with one merged representative per group.
     */
    fun deduplicatePapers(papers: Set<FetcherPaper>, threshold: Float): Set<FetcherPaper>

    /**
     * Finds the best matching [Paper] in [candidates] for the given [fetched] paper.
     *
     * All candidates whose [similarity] score with [fetched] is >= [threshold] are considered;
     * the one with the highest score is returned. Returns `null` if [candidates] is empty or no
     * candidate meets the threshold.
     *
     * @param fetched the paper to match against the candidates.
     * @param candidates existing DB papers to search, typically pre-filtered by year.
     * @param threshold minimum [similarity] score required for a candidate to qualify.
     * @return the highest-scoring candidate above [threshold], or `null` if none qualifies.
     */
    fun findMatch(fetched: FetcherPaper, candidates: List<Paper>, threshold: Float): Paper?
}

/**
 * The [IPaperMatcher] implementation.
 */
class PaperMatcher(override val config: PaperMatchingConfig) : IPaperMatcher {
    companion object {
        // Definition of the default config that is used for the matching algorithm
        val defaultConfig = PaperMatchingConfig(
            // A year difference of greater than one eliminates a paper from being similar
            yearTolerance = 1,
            // The title similarity accounts for 60% of the total similarity
            titleWeight = 0.6,
            // The authors similarity accounts for 30% of the total similarity
            authorsWeight = 0.3,
            // The abstract similarity accounts for 10% of the total similarity
            abstractWeight = 0.1,
        )

        const val DELTA = 1e-6
    }

    override fun similarity(a: FetcherPaper, b: FetcherPaper): Double {
        if (haveSameExternalId(a, b)) return 1.0
        if (haveConflictingExternalId(a, b)) return 0.0
        if (abs(a.year - b.year) > config.yearTolerance) return 0.0

        data class Component(val weight: Double, val score: Double)

        val components = mutableListOf<Component>()

        if (a.title.isNotBlank() && b.title.isNotBlank()) {
            components.add(
                Component(
                    weight = config.titleWeight,
                    score = Levenshtein.getNormalizedDistance(a.title.trim().lowercase(), b.title.trim().lowercase()),
                ),
            )
        }

        val authorsA = a.authors.filter { it.isNotBlank() }
        val authorsB = b.authors.filter { it.isNotBlank() }
        if (authorsA.isNotEmpty() && authorsB.isNotEmpty()) {
            components.add(
                Component(
                    weight = config.authorsWeight,
                    score = jaccardSimilarity(
                        Tokenization.authorSetTokens(authorsA),
                        Tokenization.authorSetTokens(authorsB),
                    ),
                ),
            )
        }

        if (a.abstract.isNotBlank() && b.abstract.isNotBlank()) {
            components.add(
                Component(
                    weight = config.abstractWeight,
                    score = Levenshtein.getNormalizedDistance(
                        a.abstract.trim().lowercase(),
                        b.abstract.trim().lowercase(),
                    ),
                ),
            )
        }

        val totalWeight = components.sumOf { it.weight }
        return components.sumOf { it.score * it.weight / totalWeight }
    }

    override fun deduplicatePapers(papers: Set<FetcherPaper>, threshold: Float): Set<FetcherPaper> {
        val groups = mutableListOf<MutableList<FetcherPaper>>()

        for (paper in papers) {
            val group = groups.firstOrNull { isSimilarityAboveThreshold(similarity(paper, it[0]), threshold) }
            if (group != null) {
                group.add(paper)
            } else {
                groups.add(mutableListOf(paper))
            }
        }

        return groups.map { mergeFetcherPapers(it) }.toSet()
    }

    override fun findMatch(fetched: FetcherPaper, candidates: List<Paper>, threshold: Float): Paper? = candidates
        .asSequence()
        .map { candidate -> candidate to similarity(fetched, candidate.toFetcherPaper()) }
        .filter { (_, score) -> isSimilarityAboveThreshold(score, threshold) }
        .sortedBy { (paper, _) -> paper.createdAt }
        .maxByOrNull { (_, score) -> score }
        ?.first

    /**
     * Returns true if both [FetcherPaper]s have at least one equal external ID.
     *
     * External IDs with a blank value are filtered out.
     */
    private fun haveSameExternalId(a: FetcherPaper, b: FetcherPaper): Boolean {
        val (externalIdsA, externalIdsB) = normalizeExternalIds(a, b)

        return externalIdsA.any { externalIdsB.contains(it) }
    }

    /**
     * Returns true if both [FetcherPaper]s have at least one conflicting external ID.
     *
     * Two external IDs are conflicting if they both have the same type, but a different value.
     * External IDs with a blank value are filtered out.
     */
    private fun haveConflictingExternalId(a: FetcherPaper, b: FetcherPaper): Boolean {
        val (externalIdsA, externalIdsB) = normalizeExternalIds(a, b)

        return externalIdsA.any { exA ->
            externalIdsB.any { exB -> exA.type == exB.type && exA.value != exB.value }
        }
    }

    private fun normalizeExternalIds(a: FetcherPaper, b: FetcherPaper): Pair<List<ExternalId>, List<ExternalId>> {
        val externalIdsA = a.externalIds.filter { it.value.isNotBlank() }
        val externalIdsB = b.externalIds.filter { it.value.isNotBlank() }

        return externalIdsA to externalIdsB
    }

    /**
     * Returns the Jaccard Similarity of two sets of strings.
     *
     * The similarity is calculated based on the size of the intersection of both sets.
     * If both sets are empty the similarity is 1, if only one is empty, the similarity is 0.
     */
    private fun jaccardSimilarity(a: Set<String>, b: Set<String>): Double {
        if (a.isEmpty() && b.isEmpty()) return 1.0
        if (a.isEmpty() || b.isEmpty()) return 0.0
        return a.intersect(b).size.toDouble() / a.union(b).size
    }

    /**
     * Merges the paper information of the [group] of papers.
     *
     * The first in the [group] takes precedence over the others.
     */
    private fun mergeFetcherPapers(group: List<FetcherPaper>): FetcherPaper {
        val first = group.first()

        // Merge metadata; first overwrites last
        val mergedMetadata = mutableMapOf<String, String>()
        for (paper in group.reversed()) {
            mergedMetadata.putAll(paper.fetcherMetadata)
        }

        // Merge external IDs; first overwrites last
        val externalIds = mutableMapOf<ExternalIdType, String>()
        for (paper in group.reversed()) {
            externalIds.putAll(paper.externalIds.associate { Pair(it.type, it.value) })
        }

        val result = FetcherPaper(
            title = group.firstNotNullOfOrNull { it.title.ifBlank { null } } ?: first.title,
            externalIds = externalIds.map { ExternalId(it.key, it.value) },
            abstract = group.firstNotNullOfOrNull { it.abstract.ifBlank { null } } ?: first.abstract,
            year = group.firstOrNull { it.year > 0 }?.year ?: first.year,
            publisher = group.firstNotNullOfOrNull { it.publisher.ifBlank { null } } ?: first.publisher,
            publicationType = group.firstNotNullOfOrNull { it.publicationType.ifBlank { null } }
                ?: first.publicationType,
            publicationName = group.firstNotNullOfOrNull { it.publicationName.ifBlank { null } }
                ?: first.publicationName,
            authors = group.firstOrNull { it.authors.isNotEmpty() }?.authors ?: first.authors,
            fetcherMetadata = mergedMetadata,
        )

        if (group.size > 1) {
            logger.debug {
                "Deduplication result: $result; Sources: $group"
            }
        }

        return result
    }

    private fun Author.isNotBlank() = this.firstName.isNotBlank() || this.lastName.isNotBlank()

    private fun isSimilarityAboveThreshold(similarity: Double, threshold: Float) = threshold - similarity < DELTA
}
