package se.uulm.snowballr.backend.service.fetcher

import io.mockk.mockk
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.FetcherService

/**
 * Base test class for the [FetcherService].
 */
sealed class FetcherServiceTest : BaseServiceTest() {
    val fetcherManagerMock = mockk<IFetcherManager>()

    private val allMocks: Array<Any> = arrayOf(fetcherManagerMock)

    val service = FetcherService(
        fetcherManager = fetcherManagerMock,
    )

    override fun getAllMocks(): Array<Any> = allMocks
}
