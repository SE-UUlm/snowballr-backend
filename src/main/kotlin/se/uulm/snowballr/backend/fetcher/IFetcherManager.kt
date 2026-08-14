package se.uulm.snowballr.backend.fetcher

import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper

/**
 * Delegate requests to multiple fetchers using their names.
 *
 * Implementations are expected to normalize (see [se.uulm.snowballr.backend.normalization.PaperNormalizer])
 * every [FetcherPaper] they return, so that callers can rely on consistent formatting regardless of which fetcher
 * produced the data.
 */
interface IFetcherManager {
    /**
     * Get the information of all registered fetchers.
     */
    suspend fun getAvailableFetchers(): Set<FetcherInformationWithId>

    /**
     * Search for papers using a search query.
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param searchQuery A text to search for.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers best matching the provided searchQuery.
     */
    suspend fun searchPapers(fetcher: String, searchQuery: String, options: Map<String, String>): Set<FetcherPaper>

    /**
     * Get the papers that are forward references of the specified paper.
     * Paper C is a forward reference of Paper B if it cites B.
     *
     * ```
     *     Paper A <-citing-- Paper B <-citing-- Paper C
     *  backward ref.                          forward ref.
     * ```
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param paper The paper from which the forward references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A (sub)set of papers that are forward references of the provided paper.
     */
    suspend fun fetchForwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper>

    /**
     * Get the papers that are backward references of the specified paper.
     * Paper A is a backward reference of Paper B if it is cited by B:
     *
     * ```
     *     Paper A <-citing-- Paper B <-citing-- Paper C
     *  backward ref.                          forward ref.
     * ```
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param paper The paper from which the backward references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A (sub)set of papers that are backward references of the provided paper.
     */
    suspend fun fetchBackwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper>
}
