package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.fetcher.IFetcher
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.Paper

private val logger = KotlinLogging.logger {}

/**
 * Keeps track of and delegates requests to multiple [IFetcher] instances.
 */
interface IFetcherService {
    /**
     * Get a list of the names of all registered fetchers.
     */
    suspend fun getAvailableFetchers(): Set<String>

    /**
     * Register a fetcher.
     * @param name The name under which a fetcher should be accessible.
     * @param impl An implementation of a fetcher.
     */
    suspend fun registerFetcher(name: String, impl: IFetcher)

    /**
     * Get a set of all option keys the specified fetcher can be configured
     * with. The values remain shapeless and ought to be validated in the
     * fetcher implementation.
     */
    suspend fun getAvailableOptions(fetcher: String): Set<String>

    /**
     * Search for papers using a search query.
     *
     * @param fetcher The name of the fether to be used for this request.
     * @param searchQuery A text to search for.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers best matching the provided searchQuery.
     */
    suspend fun searchPapers(fetcher: String, searchQuery: String, options: Map<String, String>): Set<Paper>

    /**
     * Get the papers which are references of the specified paper.
     * Paper A is a reference of Paper B if it is referred to in B:
     *
     *  Paper A <-referring-- Paper B <-referring-- Paper C
     * reference                                    citation
     *  (past)               (present)              (future)
     *
     * @param fetcher The name of the fether to be used for this request.
     * @param paper The paper of which the references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers believed to be references of the provided paper.
     */
    suspend fun fetchReferences(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper>

    /**
     * Get the papers which are citations of the specified paper.
     * Paper C is a citation of Paper B if it is referring to B:
     *
     *  Paper A <-referring-- Paper B <-referring-- Paper C
     * reference                                    citation
     *  (past)               (present)              (future)
     *
     * @param fetcher The name of the fether to be used for this request.
     * @param paper The paper of which the citations should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers believed to be citations of the provided paper.
     */
    suspend fun fetchCitations(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper>
}

class FetcherService : IFetcherService {
    private val fetchers: HashMap<String, IFetcher> = HashMap()

    private fun ensureFetcher(name: String) {
        if (!fetchers.containsKey(name)) {
            throw SnowballRException.FetcherException.UnknownFetcher(name)
        }
    }

    override suspend fun getAvailableFetchers(): Set<String> = fetchers.keys.toSet()

    @Suppress("TooGenericExceptionCaught")
    override suspend fun registerFetcher(name: String, impl: IFetcher) {
        if (fetchers.containsKey(name)) {
            throw SnowballRException.FetcherException.AlreadyRegistered("Fetcher with name '$name' already registered")
        }

        logger.info { "Successfully registered a fetcher: $name" }

        fetchers.put(name, impl)
    }

    override suspend fun getAvailableOptions(fetcher: String): Set<String> {
        ensureFetcher(fetcher)
        return fetchers.get(fetcher)!!.getAvailableOptions()
    }

    override suspend fun searchPapers(fetcher: String, searchQuery: String, options: Map<String, String>): Set<Paper> {
        ensureFetcher(fetcher)
        return fetchers.get(fetcher)!!.searchPapers(searchQuery, options)
    }

    override suspend fun fetchReferences(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper> {
        ensureFetcher(fetcher)
        return fetchers.get(fetcher)!!.fetchReferences(paper, options)
    }

    override suspend fun fetchCitations(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper> {
        ensureFetcher(fetcher)
        return fetchers.get(fetcher)!!.fetchCitations(paper, options)
    }
}
