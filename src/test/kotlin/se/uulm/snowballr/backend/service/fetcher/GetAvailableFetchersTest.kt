package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.service.MainServiceTest

class GetAvailableFetchersTest : MainServiceTest() {
    @Test
    fun `When the fetcherManager has fetchers registered, then the FetcherService returns them properly`() = runTest {
        val fetchers = setOf("foo")
        coEvery { fetcherManagerMock.getAvailableFetchers() } returns fetchers
        assertEquals(fetchers, mainService.getAvailableFetchers().fetcherNamesList.toSet())
    }
}
