package se.uulm.snowballr.backend.service.fetcher

import io.mockk.mockk
import org.koin.core.module.Module
import org.koin.dsl.module
import org.koin.test.inject
import se.uulm.snowballr.backend.fetcher.IFetcherManager
import se.uulm.snowballr.backend.service.BaseServiceTest
import se.uulm.snowballr.backend.service.FetcherService
import se.uulm.snowballr.backend.service.IFetcherService

/**
 * Base test class for the [FetcherService].
 */
sealed class FetcherServiceTest : BaseServiceTest() {
    val fetcherManagerMock = mockk<IFetcherManager>()

    val allMocks: Array<Any> = arrayOf(fetcherManagerMock)

    val service: IFetcherService by inject()

    private val module = module {
        single { fetcherManagerMock }
    }

    override fun getModule(): Module = module

    override fun getAllMocks(): Array<Any> = allMocks
}
