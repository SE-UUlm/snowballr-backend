package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.fetcher.IFetcher
import se.uulm.snowballr.backend.fetcher.LuaPluginFetcher
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.Paper
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

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

class FetcherService(
    private val data: Env.Fetcher,
) : IFetcherService {
    private val fetchers: HashMap<String, IFetcher> = HashMap()

    init {
        loadLuaFetcherPlugins()
    }

    private fun ensureFetcher(name: String) {
        if (!fetchers.containsKey(name)) {
            throw SnowballRException.FetcherException.UnknownFetcher(name)
        }
    }

    override suspend fun getAvailableFetchers(): Set<String> = fetchers.keys.toSet()

    fun registerFetcherSync(name: String, impl: IFetcher) {
        if (fetchers.containsKey(name)) {
            throw SnowballRException.FetcherException.AlreadyRegistered("Fetcher with name '$name' already registered")
        }

        logger.info { "Successfully registered a fetcher: $name" }

        fetchers.put(name, impl)
    }

    override suspend fun registerFetcher(name: String, impl: IFetcher) = registerFetcherSync(name, impl)

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

    @Suppress("TooGenericExceptionCaught")
    private fun loadLuaFetcherPlugins() {
        val pluginFiles = discoverLuaFetcherPluginFiles()
        logger.info { "Trying to load ${pluginFiles.size} lua fetcher plugins" }
        var successfulCount = 0
        pluginFiles.forEach { path ->
            val basename = path.name
                .subSequence(0, path.name.length - ".lua".length)
                .toString()

            try {
                registerFetcherSync(basename, LuaPluginFetcher.fromFile(path))
                successfulCount += 1
            } catch (e: Exception) {
                logger.atError {
                    message = "A lua fetcher plugin could not be loaded: ${path.name}"
                    cause = e
                }
            }
        }
        logger.info { "Successfully loaded ${pluginFiles.size} lua fetcher plugins" }
    }

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    private fun discoverLuaFetcherPluginFiles(): List<Path> {
        if (!data.pluginDirectory.isDirectory()) {
            logger.warn { "Fetcher plugin directory could not be found" }

            try {
                data.pluginDirectory.createDirectories()
                logger.info { "Created fetcher plugin directory" }
                return data.pluginDirectory.listDirectoryEntries()
            } catch (e: Exception) {
                logger.error { "Could not create fetcher plugin directory. Continuing without fetchers" }
            }
        }

        return data
            .pluginDirectory
            .listDirectoryEntries()
            .filter {
                if (it.extension == "lua") {
                    true
                } else {
                    logger.warn { "Ignoring unknown file in lua fetcher plugins directory: $it" }
                    false
                }
            }
    }
}
