package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.fetcher.PythonPluginLoader
import snowballr.Main.AvailableFetcherApis

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
@DelicateCoroutinesApi
class FetcherService(
    private val envReader: EnvReader,
) : IFetcherService {
    private val fetcherManager = FetcherManager()

    @Suppress("UnusedPrivateProperty")
    private val pythonPluginLoader = PythonPluginLoader(envReader.env.plugins.pluginDirectory, fetcherManager)

    override suspend fun getAvailableFetchers(): AvailableFetcherApis = AvailableFetcherApis
        .newBuilder()
        .addAllFetcherApis(fetcherManager.getAvailableFetchers())
        .build()
}
