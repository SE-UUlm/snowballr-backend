package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GetAvailableFetchersTest : FetcherServiceTest() {
    @Test
    fun `When the fetcherManager has fetchers registered, then the FetcherService returns them properly`() = runTest {
        val fetchers = setOf("foo")

        coEvery { fetcherManagerMock.getAvailableFetchers() } returns fetchers

        assertEquals(fetchers, service.getAvailableFetchers().fetcherNamesList.toSet())
    }
}
