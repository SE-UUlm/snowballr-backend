package se.uulm.snowballr.backend.fetcher

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.mockk
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.DataBuilder
import kotlin.test.assertEquals

private val examplePaper = DataBuilder.createExamplePaper()

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FetcherManagerTest {
    private val threadContext = newSingleThreadContext("Test thread")
    private var fetcherManager = FetcherManager()

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
        coEvery { fetcherMock.getAvailableOptions() } returns setOf("bar")
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(setOf("bar"), fetcherManager.getAvailableOptions("foo"))
        coVerify { fetcherMock.getAvailableOptions() }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then searchPapers is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.searchPapers(any(), any()) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(setOf(examplePaper), fetcherManager.searchPapers("foo", "bar", mapOf("x" to "y")))
        coVerify { fetcherMock.searchPapers("bar", mapOf("x" to "y")) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchForwardReferences is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.fetchForwardReferences(any(), any()) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(setOf(examplePaper), fetcherManager.fetchForwardReferences("foo", examplePaper, mapOf("x" to "y")))
        coVerify { fetcherMock.fetchForwardReferences(examplePaper, mapOf("x" to "y")) }
        confirmVerified(fetcherMock)
    }

    @Test
    fun `When a fetcher is registered, then fetchBackwardReferences is delegated properly`() = runTest {
        val fetcherMock = mockk<IFetcher>()
        coEvery { fetcherMock.fetchBackwardReferences(any(), any()) } returns setOf(examplePaper)
        fetcherManager.registerFetcher("foo", fetcherMock)

        assertEquals(
            setOf(examplePaper),
            fetcherManager.fetchBackwardReferences("foo", examplePaper, mapOf("x" to "y")),
        )
        coVerify { fetcherMock.fetchBackwardReferences(examplePaper, mapOf("x" to "y")) }
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
