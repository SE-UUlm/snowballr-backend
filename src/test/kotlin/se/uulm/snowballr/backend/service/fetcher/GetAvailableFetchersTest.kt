package se.uulm.snowballr.backend.service.fetcher

import io.mockk.coEvery
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.fetcher.FetcherInformation
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.fetcher.FetcherOptionsSchema
import se.uulm.snowballr.backend.model.fetcher.Link

class GetAvailableFetchersTest : FetcherServiceTest() {
    @Test
    fun `When the fetcherManager has fetchers registered, then the FetcherService returns them properly`() = runTest {
        val links = listOf(Link("HomePage", "https://example.com/"))
        val optionsSchema = mapOf(
            "API_KEY" to FetcherOptionsSchema(
                name = "ApiKey",
                description = "Test API key",
                isRequired = true,
                isSecret = true,
                defaultValue = null,
            ),
            "QUERY_LIMIT" to FetcherOptionsSchema(
                name = "QueryLimit",
                description = "Limit of query result",
                isRequired = false,
                isSecret = false,
                defaultValue = "25",
            ),
        )

        val fetchers = setOf(
            FetcherInformationWithId(
                id = "test",
                information = FetcherInformation(
                    name = "Test Fetcher",
                    description = "",
                    links = links,
                    optionsSchema = optionsSchema,
                ),
            ),
        )

        coEvery { fetcherManagerMock.getAvailableFetchers() } returns fetchers

        val result = service.getAvailableFetchers()

        assertEquals(fetchers, result)
    }
}
