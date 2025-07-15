package se.uulm.snowballr.backend.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.fetcher.IFetcher
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.testCoroutine
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.assertEquals

private val examplePaper = Paper(
    UUID.randomUUID(),
    "Title",
    "ExternalId",
    "Abstract",
    Instant.fromEpochSeconds(0),
    "Publisher",
    "PublicationType",
    "PublicationName",
    UUID.randomUUID(),
    OffsetDateTime.now(),
    null,
    null,
)

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetcherServiceTest {
    private val threadContext = newSingleThreadContext("Test thread")
    private var fetcherService = FetcherService()

    @BeforeAll
    fun setThreadDispatcher() {
        Dispatchers.setMain(threadContext)
    }

    @AfterAll
    fun cleanThreadDispatcher() {
        Dispatchers.resetMain()
        threadContext.close()
    }

    @BeforeEach
    fun createNewFetcherService() {
        fetcherService = FetcherService()
    }

    @Test
    fun `When a fetcher is registered, then it is listed as available`() = testCoroutine {
        fetcherService.registerFetcher("foo", mockk())
        assertEquals(setOf("foo"), fetcherService.getAvailableFetchers())
    }

    @Test
    fun `When a fetcher is removed, then it is no longer listed as available`() = testCoroutine {
        fetcherService.registerFetcher("foo", mockk())
        assertEquals(setOf("foo"), fetcherService.getAvailableFetchers())
        fetcherService.removeFetcher("foo")
        assertEquals(setOf(), fetcherService.getAvailableFetchers())
    }

    @Test
    fun `When a fetcher is registered, then getAvailableOptions is delegated properly`() = testCoroutine {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.getAvailableOptions() } returns setOf("bar")
        fetcherService.registerFetcher("foo", fetcherMock)

        assertEquals(setOf("bar"), fetcherService.getAvailableOptions("foo"))
        coVerify { fetcherMock.getAvailableOptions() }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then searchPapers is delegated properly`() = testCoroutine {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.searchPapers(any(), any()) } returns setOf(examplePaper)
        fetcherService.registerFetcher("foo", fetcherMock)

        assertEquals(setOf(examplePaper), fetcherService.searchPapers("foo", "bar", mapOf("x" to "y")))
        coVerify { fetcherMock.searchPapers("bar", mapOf("x" to "y")) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchForwardReferences is delegated properly`() = testCoroutine {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.fetchForwardReferences(any(), any()) } returns setOf(examplePaper)
        fetcherService.registerFetcher("foo", fetcherMock)

        assertEquals(setOf(examplePaper), fetcherService.fetchForwardReferences("foo", examplePaper, mapOf("x" to "y")))
        coVerify { fetcherMock.fetchForwardReferences(examplePaper, mapOf("x" to "y")) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchBackwardReferences is delegated properly`() = testCoroutine {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.fetchBackwardReferences(any(), any()) } returns setOf(examplePaper)
        fetcherService.registerFetcher("foo", fetcherMock)

        assertEquals(
            setOf(examplePaper),
            fetcherService.fetchBackwardReferences("foo", examplePaper, mapOf("x" to "y")),
        )
        coVerify { fetcherMock.fetchBackwardReferences(examplePaper, mapOf("x" to "y")) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is not registered, then delegated calls will throw`() = testCoroutine {
        fetcherService.registerFetcher("foo", mockk())

        assertThrows<Exception> { fetcherService.getAvailableOptions("bar") }
        assertThrows<Exception> { fetcherService.searchPapers("bar", "", mapOf()) }
        assertThrows<Exception> { fetcherService.fetchForwardReferences("bar", examplePaper, mapOf()) }
        assertThrows<Exception> { fetcherService.fetchBackwardReferences("bar", examplePaper, mapOf()) }
    }

    @Test
    fun `When a fetcher is not registered, then removing it will do nothing`() = testCoroutine {
        assertDoesNotThrow { fetcherService.removeFetcher("foo") }
    }

    @Test
    fun `When a fetcher is already registered, then an exception will be thrown`() = testCoroutine {
        fetcherService.registerFetcher("foo", mockk())
        assertThrows<Exception> { fetcherService.registerFetcher("foo", mockk()) }
    }
}
