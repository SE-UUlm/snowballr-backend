package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import snowballr.Base
import snowballr.Fetcher

class GetAvailableFetchersTest : FetcherServiceTest() {
    @Test
    fun `When the fetcherManager has fetchers registered, then the FetcherService returns them properly`() = runTest {
        val links = listOf(
            Base.Link.newBuilder()
                .setLabel("Homepage")
                .setUrl("https://example.com/")
                .build(),
        )
        val optionsSchema = mapOf(
            "API_KEY" to Fetcher.FetcherOptionSchema.newBuilder()
                .setDescription("Test API key")
                .setRequired(true)
                .setIsSecret(true)
                .build(),
            "QUERY_LIMIT" to Fetcher.FetcherOptionSchema.newBuilder()
                .setDescription("Limit of query result")
                .setRequired(false)
                .setIsSecret(false)
                .setDefaultValue("25")
                .build(),
        )

        val fetchers = setOf(
            Fetcher.FetcherInformation.newBuilder()
                .setId("test")
                .setName("Test Fetcher")
                .addAllLinks(links)
                .putAllOptionsSchema(optionsSchema)
                .build(),
        )

        coEvery { fetcherManagerMock.getAvailableFetchers() } returns fetchers

        val result = service.getAvailableFetchers().fetchersList.toSet()

        assertEquals(fetchers, result)
    }
}
