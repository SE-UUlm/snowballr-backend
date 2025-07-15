package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.fetcher.FetcherManager
import snowballr.Main.AvailableFetcherApis
import se.uulm.snowballr.backend.fetcher.PythonPluginFetcher
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension

private val logger = KotlinLogging.logger {}

/**
 * Orchestrates the paper fetching process. This service is responsible for
 * retrieving papers for a specific request. It has to make sure that the
 * correct fetchers (specified in the project settings) are accessed using the
 * correct options (also specified in the project setitngs).
 */
interface IFetcherService {
    /**
     * Service implementation of [SnowballRService.getAvailableFetcherApis].
     */
    suspend fun getAvailableFetchers(): AvailableFetcherApis

    // TODO: WIP. Future PRs will introduce more functionality.
}

/**
 * Default implementation of [IFetcherService].
 */
class FetcherService(
    private val envReader: EnvReader,
) : IFetcherService {
    private val fetcherManager = FetcherManager()

    init {
        PythonPluginFetcher.locateNativeLibrary()
        loadPythonFetcherPlugins(envReader.env.fetcher.pluginDirectory)
    }

    override suspend fun getAvailableFetchers(): AvailableFetcherApis = AvailableFetcherApis
        .newBuilder()
        .addAllFetcherApis(fetcherManager.getAvailableFetchers())
        .build()

    private fun loadPythonFetcherPlugins(pluginDirectory: Path) {
        ensureDirectoryExists(pluginDirectory)
        val pluginFiles = pluginDirectory
            .listDirectoryEntries()
            .filter { it.extension == "py" }
            .toSet()

        ensureDirectoryExists(pluginDirectory.resolve("lib"))
        PythonPluginFetcher.writeDataTypesModule(pluginDirectory.resolve("lib/snowballr.py"))

        logger.info { "Trying to load ${pluginFiles.size} python fetcher plugins" }

        var successful = 0

        for (path in pluginFiles) {
            val name = path.nameWithoutExtension
            try {
                fetcherManager.registerFetcher(name, PythonPluginFetcher.fromFile(name, path, pluginDirectory, fetcherManager))
                successful++
            } catch (e: Exception) {
                logger.atError {
                    message = "A python fetcher plugin could not be loaded: ${path.name}"
                    cause = e
                }
            }
        }

        logger.info { "Successfully loaded $successful python fetcher plugins" }
    }

    private fun ensureDirectoryExists(path: Path): Boolean {
        if (path.isDirectory()) return true

        logger.warn { "Directory '$path' could not be found" }

        try {
            path.createDirectories()
            logger.info { "Created directory '$path'" }
            return true
        } catch (e: Exception) {
            logger.error(e) { "Could not create directory '$path'" }
            return false
        }
    }
}
