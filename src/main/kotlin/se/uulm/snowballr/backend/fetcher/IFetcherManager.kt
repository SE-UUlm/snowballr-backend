package se.uulm.snowballr.backend.fetcher

class FetcherException(stderr: String) : Exception(stderr)

/**
 * Delegate requests to multiple fetchers using their names.
 */
interface IFetcherManager {
    /**
     * Get the names of all registered fetchers.
     */
    fun getAvailableFetchers(): Set<String>

    /**
     * Get the names of all option keys and their according default values the
     * specified fetcher can be configured with. The values remain shapeless
     * and ought to be validated in the fetcher implementation.
     *
     * @param fetcher The name of the fetcher whose options should be retrieved.
     * @return A set of names for options the fetcher has specified it would accept.
     */
    suspend fun getAvailableOptions(fetcher: String): Map<String, String>

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
