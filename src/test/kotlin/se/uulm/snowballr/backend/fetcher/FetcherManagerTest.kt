package se.uulm.snowballr.backend.fetcher

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder

class FetcherManagerTest {
    private var fetcherManager = FetcherManager()
    private val examplePaper = DataBuilder.createExamplePaper()
    private val exampleOptions = mapOf("foo" to "bar")

    @BeforeEach
    fun createNewFetcherService() {
        fetcherManager = FetcherManager()
    }

    @Test
    fun `When a fetcher is registered, then it is listed as available`() = runTest {
        fetcherManager.registerFetcher("foo", mockk())
        assertEquals(setOf("foo"), fetcherManager.getAvailableFetchers())
    }

    @Test
    fun `When a fetcher is removed, then it is no longer listed as available`() = runTest {
        fetcherManager.registerFetcher("foo", mockk())
        assertEquals(setOf("foo"), fetcherManager.getAvailableFetchers())

        fetcherManager.removeFetcher("foo")
        assertEquals(emptySet(), fetcherManager.getAvailableFetchers())
    }

    @Test
    fun `When a fetcher is registered, then getAvailableOptions is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.getAvailableOptions() } returns mapOf("bar" to "test")
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(mapOf("bar" to "test"), fetcherManager.getAvailableOptions("foo"))
        coVerify { fetcherMock.getAvailableOptions() }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then searchPapers is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.searchPapers("bar", exampleOptions) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(
            setOf(examplePaper),
            fetcherManager.searchPapers("foo", "bar", exampleOptions),
        )
        coVerify { fetcherMock.searchPapers("bar", exampleOptions) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchForwardReferences is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        val referencingPaper = DataBuilder.createExamplePaper()

        coEvery { fetcherMock.fetchForwardReferences(referencingPaper, exampleOptions) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(
            setOf(examplePaper),
            fetcherManager.fetchForwardReferences("foo", referencingPaper, exampleOptions),
        )
        coVerify { fetcherMock.fetchForwardReferences(referencingPaper, exampleOptions) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchBackwardReferences is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        val paperWithReference = DataBuilder.createExamplePaper()

        coEvery { fetcherMock.fetchBackwardReferences(paperWithReference, exampleOptions) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(
            setOf(examplePaper),
            fetcherManager.fetchBackwardReferences("foo", paperWithReference, exampleOptions),
        )
        coVerify { fetcherMock.fetchBackwardReferences(paperWithReference, exampleOptions) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is not registered, then delegated calls will throw`() = runTest {
        fetcherManager.registerFetcher("foo", mockk())

        assertThrows<Exception> { fetcherManager.getAvailableOptions("bar") }
        assertThrows<Exception> { fetcherManager.searchPapers("bar", "", emptyMap()) }
        assertThrows<Exception> { fetcherManager.fetchForwardReferences("bar", examplePaper, emptyMap()) }
        assertThrows<Exception> { fetcherManager.fetchBackwardReferences("bar", examplePaper, emptyMap()) }
    }

    @Test
    fun `When a fetcher is not registered, then removing it will do nothing`() = runTest {
        assertDoesNotThrow { fetcherManager.removeFetcher("foo") }
    }

    @Test
    fun `When a fetcher is already registered, then an exception will be thrown`() = runTest {
        fetcherManager.registerFetcher("foo", mockk())
        assertThrows<Exception> { fetcherManager.registerFetcher("foo", mockk()) }
    }
}
