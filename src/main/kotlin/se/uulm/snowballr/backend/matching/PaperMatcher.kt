package se.uulm.snowballr.backend.matching

import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.paper.toFetcherPaper
import se.uulm.snowballr.backend.model.fetcher.FetcherMetadata
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import kotlin.math.abs

interface IPaperMatcher {
    /**
     * Returns a similarity score in [0.0, 1.0] between two [FetcherPaper]s.
     *
     * Scoring rules:
     * - If both papers share at least one [ExternalId], returns 1.0 immediately.
     * - If the publication years differ by more than one, returns 0.0 immediately.
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

    /**
     * Merges fetcher metadata from a freshly fetched paper into the existing DB paper's metadata.
     *
     * The DB paper's values take precedence on key conflicts: keys present in [dbPaper] always
     * overwrite the same keys from [fetched]. Keys present only in [fetched] are added.
     *
     * @param dbPaper the existing paper stored in the database.
     * @param fetched the newly fetched paper whose metadata may contain new keys.
     * @return a merged [FetcherMetadata] map.
     */
    fun mergeMetadata(dbPaper: Paper, fetched: FetcherPaper): FetcherMetadata
}

/**
 * The [IPaperMatcher] implementation.
 *
 * @param config The weight config that is used by [similarity] to determine how each component accounts to the overall
 * similarity.
 */
class PaperMatcher(private val config: PaperMatchingConfig) : IPaperMatcher {
    override fun similarity(a: FetcherPaper, b: FetcherPaper): Double {
        if (haveSameExternalId(a, b)) return 1.0
        if (abs(a.year - b.year) > config.yearTolerance) return 0.0

        data class Component(val weight: Double, val score: Double)

        val components = mutableListOf<Component>()

        components.add(
            Component(
                weight = config.titleWeight,
                score = Levenshtein.getNormalizedDistance(a.title.trim().lowercase(), b.title.trim().lowercase()),
            ),
        )

        if (a.authors.isNotEmpty() && b.authors.isNotEmpty()) {
            components.add(
                Component(
                    weight = config.authorsWeight,
                    score = jaccardSimilarity(
                        Tokenization.authorSetTokens(a.authors),
                        Tokenization.authorSetTokens(b.authors),
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
            val group = groups.firstOrNull { similarity(paper, it[0]) >= threshold }
            if (group != null) {
                group.add(paper)
            } else {
                groups.add(mutableListOf(paper))
            }
        }

        return groups.map { mergeFetcherPapers(it) }.toSet()
    }

    override fun findMatch(fetched: FetcherPaper, candidates: List<Paper>, threshold: Float): Paper? = candidates
        .map { candidate -> candidate to similarity(fetched, candidate.toFetcherPaper()) }
        .filter { (_, score) -> score >= threshold }
        .maxByOrNull { (_, score) -> score }
        ?.first

    override fun mergeMetadata(dbPaper: Paper, fetched: FetcherPaper): FetcherMetadata {
        val merged = fetched.fetcherMetadata.toMutableMap()
        merged.putAll(dbPaper.fetcherMetadata)
        return merged
    }

    /**
     * Returns true if both [FetcherPaper]s have at least one equal external ID.
     */
    private fun haveSameExternalId(a: FetcherPaper, b: FetcherPaper) = a.externalIds.any { b.externalIds.contains(it) }

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

        return FetcherPaper(
            title = group.firstNotNullOfOrNull { it.title.ifBlank { null } } ?: first.title,
            externalIds = externalIds.map { ExternalId(it.key, it.value) },
            abstract = group.firstNotNullOfOrNull { it.abstract.ifBlank { null } } ?: first.abstract,
            year = first.year,
            publisher = group.firstNotNullOfOrNull { it.publisher.ifBlank { null } } ?: first.publisher,
            publicationType = group.firstNotNullOfOrNull { it.publicationType.ifBlank { null } }
                ?: first.publicationType,
            publicationName = group.firstNotNullOfOrNull { it.publicationName.ifBlank { null } }
                ?: first.publicationName,
            authors = group.firstOrNull { it.authors.isNotEmpty() }?.authors ?: first.authors,
            fetcherMetadata = mergedMetadata,
        )
    }
}
