package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.fetcher.FetcherManager
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
class FetcherService : IFetcherService {
    private val fetcherManager = FetcherManager()

    override suspend fun getAvailableFetchers(): AvailableFetcherApis = AvailableFetcherApis
        .newBuilder()
        .addAllFetcherApis(fetcherManager.getAvailableFetchers())
        .build()
}
