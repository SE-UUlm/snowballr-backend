package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.fetcher.FetcherManager
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import snowballr.Fetcher.AvailableFetchers

interface IFetcherService {
    /**
     * Service implementation of [SnowballRService.getAvailableFetchers].
     */
    suspend fun getAvailableFetchers(): AvailableFetchers
}

/**
 * Orchestrates the paper fetching process. This service is responsible for
 * retrieving papers for a specific request. It has to make sure that the
 * correct fetchers (specified in the project settings) are accessed using the
 * correct options (also specified in the project settings).
 *
 * @param fetcherManager The [FetcherManager] that manages the available fetchers.
 */
class FetcherService(
    private val fetcherManager: FetcherManager,
) : IFetcherService {
    override suspend fun getAvailableFetchers(): AvailableFetchers = AvailableFetchers
        .newBuilder()
        .addAllFetcherNames(fetcherManager.getAvailableFetchers())
        .build()
}
