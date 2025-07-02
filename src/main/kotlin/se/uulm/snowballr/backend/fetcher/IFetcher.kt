package se.uulm.snowballr.backend.fetcher

import se.uulm.snowballr.backend.model.dto.Paper

/**
 * An [IFetcher] abstracts away the API used to acquire new papers during the
 * crawl process.
 *
 * Each fetcher is instanciated once and will be reused between projects. The
 * behavior of the fetcher can be configured using a set of string-to-string
 * key-value pairs. These options reside in the project settings and are thus
 * project specific. An example use-case are bring-your-own access-tokens for
 * public (potentially rate-limited) APIs.
 */
interface IFetcher {
    /**
     * Get a set of all option keys this fetcher can be configured with.
     * The values remain shapeless and ought to be validated in the fetcher
     * implementation.
     */
    suspend fun getAvailableOptions(): Set<String>

    /**
     * Search for papers using a search query.
     *
     * @param searchQuery A text to search for.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers best matching the provided searchQuery.
     */
    suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper>

    /**
     * Get the papers which are references of the specified paper.
     * Paper A is a reference of Paper B if it is referred to in B:
     *
     *  Paper A <-referring-- Paper B <-referring-- Paper C
     * reference                                    citation
     *  (past)               (present)              (future)
     *
     * @param paper The paper of which the references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers believed to be references of the provided paper.
     */
    suspend fun fetchReferences(paper: Paper, options: Map<String, String>): Set<Paper>

    /**
     * Get the papers which are citations of the specified paper.
     * Paper C is a citation of Paper B if it is referring to B:
     *
     *  Paper A <-referring-- Paper B <-referring-- Paper C
     * reference                                    citation
     *  (past)               (present)              (future)
     *
     * @param paper The paper of which the citations should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers believed to be citations of the provided paper.
     */
    suspend fun fetchCitations(paper: Paper, options: Map<String, String>): Set<Paper>
}
