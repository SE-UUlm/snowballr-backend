package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.model.dto.Paper
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Keeps track of and delegates requests to multiple [IFetcher] instances.
 */
class FetcherManager {
    private val fetchers: ConcurrentHashMap<String, IFetcher> = ConcurrentHashMap()

    private fun getFetcherOrThrow(name: String): IFetcher = fetchers[name] ?: throw UnknownFetcherException(name)

    /**
     * Get the names of all registered fetchers.
     */
    fun getAvailableFetchers(): Set<String> = fetchers.keys.toSet()

    /**
     * Register a fetcher.
     *
     * @param name The name under which a fetcher should be accessible.
     * @param impl An implementation of a fetcher.
     */
    fun registerFetcher(name: String, impl: IFetcher) {
        if (fetchers.putIfAbsent(name, impl) == null) {
            logger.info { "Successfully registered the fetcher $name." }
        } else {
            throw AlreadyRegisteredException(name)
        }
    }

    /**
     * Remove a fetcher.
     *
     * @param name The name of the fetcher which should be removed.
     */
    fun removeFetcher(name: String) {
        if (fetchers.remove(name) != null) {
            logger.info { "Successfully removed the fetcher $name." }
        } else {
            logger.warn { "Could not remove the fetcher $name, as it was not registered before." }
        }
    }

    /**
     * Get a set of all option keys the specified fetcher can be configured
     * with. The values remain shapeless and ought to be validated in the
     * fetcher implementation.
     *
     * @param fetcher The name of the fetcher whose options should be retrieved.
     * @return A set of names for options the fetcher has specified it would accept.
     */
    suspend fun getAvailableOptions(fetcher: String): Set<String> = getFetcherOrThrow(
        fetcher,
    ).getAvailableOptions()

    /**
     * Search for papers using a search query.
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param searchQuery A text to search for.
     * @param options The set of options to be used for this invocation.
     * @return A set of papers best matching the provided searchQuery.
     */
    suspend fun searchPapers(fetcher: String, searchQuery: String, options: Map<String, String>): Set<Paper> =
        getFetcherOrThrow(
            fetcher,
        ).searchPapers(searchQuery, options)

    /**
     * Get the papers that are forward references of the specified paper.
     * Paper A is a forward reference of Paper B if it is referred to in B:
     *
     *    Paper A <-referring-- Paper B <-referring-- Paper C
     *  forward ref.                                backward ref.
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param paper The paper from which the forward references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A (sub)set of papers that are forward references of the provided paper.
     */
    suspend fun fetchForwardReferences(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper> =
        getFetcherOrThrow(fetcher).fetchForwardReferences(paper, options)

    /**
     * Get the papers that are backward references of the specified paper.
     * Paper C is a backward reference of Paper B if it is referring to B:
     *
     *    Paper A <-referring-- Paper B <-referring-- Paper C
     *  forward ref.                                backward ref.
     *
     * @param fetcher The name of the fetcher to be used for this request.
     * @param paper The paper from which the backward references should be fetched.
     * @param options The set of options to be used for this invocation.
     * @return A (sub)set of papers that are backward references of the provided paper.
     */
    suspend fun fetchBackwardReferences(fetcher: String, paper: Paper, options: Map<String, String>): Set<Paper> =
        getFetcherOrThrow(fetcher).fetchBackwardReferences(paper, options)

    class UnknownFetcherException(name: String) : Exception("The fetcher '$name' is not known")
    class AlreadyRegisteredException(name: String) : Exception("The fetcher '$name' is already registered.")
}
