package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.service.MainServiceTest
import snowballr.Fetcher.GetAvailableFetcherOptionsRequest
import kotlin.test.assertEquals

class GetAvailableFetcherOptionsTest : MainServiceTest() {
    @Test
    fun `When the fetcherManager returns fetcher options, then the FetcherService returns them properly`() = runTest {
        val fetcherOptions = mapOf("foo" to "bar")
        coEvery { fetcherManagerMock.getAvailableOptions("foobar") } returns fetcherOptions
        assertEquals(
            fetcherOptions,
            mainService.getAvailableFetcherOptions(
                GetAvailableFetcherOptionsRequest.newBuilder().setFetcherName("foobar").build(),
            ).optionsMap,
        )
    }
}
