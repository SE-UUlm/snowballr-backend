package se.uulm.snowballr.backend.fetcher

import jep.python.PyObject
import jep.SharedInterpreter
import jep.Jep
import kotlinx.datetime.Instant
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.AfterAll
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.fetcher.FetcherManager
import java.time.OffsetDateTime
import java.util.UUID
import java.net.InetSocketAddress
import java.nio.file.Path
import com.sun.net.httpserver.HttpServer
import com.sun.net.httpserver.HttpHandler
import com.sun.net.httpserver.HttpExchange
import io.mockk.mockk
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified

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
internal class PythonPluginFetcherTest {

    private val threadContext = newSingleThreadContext("Test thread")

    @BeforeAll
    fun setThreadDispatcher() {
        Dispatchers.setMain(threadContext)
    }

    @BeforeAll
    fun locateJepLibrary() {
        PythonPluginFetcher.locateNativeLibrary()
    }

    @AfterAll
    fun cleanThreadDispatcher() {
        Dispatchers.resetMain()
        threadContext.close()
    }

    private fun withNewInterpreter(block: (Jep) -> Unit) {
        val interp = SharedInterpreter()
        interp.exec(PythonPluginFetcher.dataTypesModuleContent())
        interp.use(block)
    }

    @Test
    fun `When an options-map is convert to a PyObject, then python should be able to correctly access it`() {
        withNewInterpreter { interp ->
            val opts = mapOf("foo" to "bar")
            interp.set("opts", opts.toPyObject(interp))
            assertEquals("bar", interp.getValue("""opts["foo"]""", String::class.java))
        }
    }

    @Test
    fun `When a python dictionary is convert to an options-map, then java should be able to correctly access it`() {
        withNewInterpreter { interp ->
            val opts = interp.getValue("""{"foo": "bar"}""", PyObject::class.java).toOptionsMap()
            assertEquals("bar", opts.get("foo"))
        }
    }

    @Test
    fun `When a options-map is convert to a PyObject and back, then it should stay the same`() {
        withNewInterpreter { interp ->
            val options = mapOf("foo" to "bar")
            assertEquals(options, options.toPyObject(interp).toOptionsMap())
        }
    }

    @Test
    fun `When a paper is convert to a python paper, then python should be able to correctly access it`() {
        withNewInterpreter { interp ->
            interp.set("paper", examplePaper.toPyObject(interp))
            assertEquals(examplePaper.title, interp.getValue("paper.title", String::class.java))
            assertEquals(examplePaper.externalId, interp.getValue("paper.externalId", String::class.java))
            assertEquals(examplePaper.abstract, interp.getValue("paper.abstract", String::class.java))
            assertEquals(
                examplePaper.publishedAt?.epochSeconds,
                interp.getValue("paper.publishedAt", Integer::class.java)?.toLong(),
            )
            assertEquals(examplePaper.publisher, interp.getValue("paper.publisher", String::class.java))
            assertEquals(examplePaper.publicationType, interp.getValue("paper.publicationType", String::class.java))
            assertEquals(examplePaper.publicationName, interp.getValue("paper.publicationName", String::class.java))
        }
    }

    @Test
    fun `When a python paper is convert to a paper, then java should be able to correctly access it`() {
        withNewInterpreter { interp ->
            val paper = interp.getValue(
                """
                Paper(
                    "Title",
                    "Abstract",
                    "ExternalId",
                    0,
                    "Publisher",
                    "PublicationType",
                    "PublicationName"
                )
                """.trimIndent(),
                PyObject::class.java,
            ).toPaper()
            with(paper) {
                assertEquals("Title", title)
                assertEquals("Abstract", abstract)
                assertEquals("ExternalId", externalId)
                assertEquals(0, publishedAt?.epochSeconds)
                assertEquals("Publisher", publisher)
                assertEquals("PublicationType", publicationType)
                assertEquals("PublicationName", publicationName)
            }
        }
    }

    @Test
    fun `When a paper is convert to a python paper and back, then it should stay the same`() {
        withNewInterpreter { interp ->
            val converted = examplePaper.toPyObject(interp).toPaper()
            with(converted) {
                assertEquals(examplePaper.title, title)
                assertEquals(examplePaper.abstract, abstract)
                assertEquals(examplePaper.externalId, externalId)
                assertEquals(examplePaper.publishedAt?.epochSeconds, publishedAt?.epochSeconds)
                assertEquals(examplePaper.publisher, publisher)
                assertEquals(examplePaper.publicationType, publicationType)
                assertEquals(examplePaper.publicationName, publicationName)
            }
        }
    }

    @Test
    fun `When python returns a string set, then java can access it as a string set`() {
        withNewInterpreter { interp ->
            val set = interp.getValue("""{"foo", "bar"}""", PyObject::class.java).toSet<String>(interp)
            assertEquals(setOf("foo", "bar"), set)
        }
    }

    @Test
    fun `When python returns a paper set, then java can access it as a paper set`() {
        withNewInterpreter { interp ->
            val set = interp.getValue("""{Paper("foo", "bar"), Paper("x", "y")}""", PyObject::class.java).toSet<PyObject>(interp).map { it.toPaper() }.toSet()

            assertEquals(2, set.size)

            assert(set.any { it.title == "foo" && it.abstract == "bar" })
            assert(set.any { it.title == "x" && it.abstract == "y" })
        }
    }

    @Test
    fun `When a python fetcher specifies options, then java should read them correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test", """
            availableOptions = [
                "foo",
                "bar"
            ]
        """.trimIndent(), Path.of("."), mockk())

        assertEquals(setOf("foo", "bar"), fetcher.getAvailableOptions())
    }

    @Test
    fun `When a python fetcher's searchPapers returns papers, then java should read them correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def searchPapers(searchQuery, options):
                return {
                    Paper("foo", "bar"),
                    Paper("x", "y"),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.searchPapers("", mapOf())

        assertEquals(2, papers.size)
        assert(papers.any { it.title == "foo" && it.abstract == "bar" })
        assert(papers.any { it.title == "x" && it.abstract == "y" })
    }

    @Test
    fun `When java passes a searchQuery to a python fetcher's searchPapers, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def searchPapers(searchQuery, options):
                return {
                    Paper(searchQuery, ""),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.searchPapers("foo", mapOf())

        assertEquals(1, papers.size)
        assert(papers.any { it.title == "foo" })
    }

    @Test
    fun `When java passes an option to a python fetcher's searchPapers, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def searchPapers(searchQuery, options):
                return {
                    Paper(options["foo"], options["x"]),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.searchPapers("", mapOf("foo" to "bar", "x" to "y"))

        assertEquals(1, papers.size)
        assert(papers.any { it.title == "bar" && it.abstract == "y" })
    }

    @Test
    fun `When a python fetcher's fetchForwardReferences returns papers, then java should read them correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchForwardReferences(paper, options):
                return {
                    Paper("foo", "bar"),
                    Paper("x", "y"),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchForwardReferences(examplePaper, mapOf())

        assertEquals(2, papers.size)
        assert(papers.any { it.title == "foo" && it.abstract == "bar" })
        assert(papers.any { it.title == "x" && it.abstract == "y" })
    }

    @Test
    fun `When java passes a paper to a python fetcher's fetchForwardReferences, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchForwardReferences(paper, options):
                return { paper }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchForwardReferences(examplePaper, mapOf())

        assertEquals(1, papers.size)
        assert(papers.any {
            examplePaper.title == it.title &&
            examplePaper.abstract == it.abstract &&
            examplePaper.externalId == it.externalId &&
            examplePaper.publishedAt?.epochSeconds == it.publishedAt?.epochSeconds &&
            examplePaper.publisher == it.publisher &&
            examplePaper.publicationType == it.publicationType &&
            examplePaper.publicationName == it.publicationName
        })
    }

    @Test
    fun `When java passes an option to a python fetcher's fetchForwardReferences, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchForwardReferences(paper, options):
                return {
                    Paper(options["foo"], options["x"]),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchForwardReferences(examplePaper, mapOf("foo" to "bar", "x" to "y"))

        assertEquals(1, papers.size)
        assert(papers.any { it.title == "bar" && it.abstract == "y" })
    }

    @Test
    fun `When a python fetcher's fetchBackwardReferences returns papers, then java should read them correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchBackwardReferences(paper, options):
                return {
                    Paper("foo", "bar"),
                    Paper("x", "y"),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchBackwardReferences(examplePaper, mapOf())

        assertEquals(2, papers.size)
        assert(papers.any { it.title == "foo" && it.abstract == "bar" })
        assert(papers.any { it.title == "x" && it.abstract == "y" })
    }

    @Test
    fun `When java passes a paper to a python fetcher's fetchBackwardReferences, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchBackwardReferences(paper, options):
                return { paper }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchBackwardReferences(examplePaper, mapOf())

        assertEquals(1, papers.size)
        assert(papers.any {
            examplePaper.title == it.title &&
            examplePaper.abstract == it.abstract &&
            examplePaper.externalId == it.externalId &&
            examplePaper.publishedAt?.epochSeconds == it.publishedAt?.epochSeconds &&
            examplePaper.publisher == it.publisher &&
            examplePaper.publicationType == it.publicationType &&
            examplePaper.publicationName == it.publicationName
        })
    }

    @Test
    fun `When java passes an option to a python fetcher's fetchBackwardReferences, then python should read it correctly`() = runTest {
        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            def fetchBackwardReferences(paper, options):
                return {
                    Paper(options["foo"], options["x"]),
                }
        """.trimIndent(), Path.of("."), mockk())

        val papers = fetcher.fetchBackwardReferences(examplePaper, mapOf("foo" to "bar", "x" to "y"))

        assertEquals(1, papers.size)
        assert(papers.any { it.title == "bar" && it.abstract == "y" })
    }

    // This test could fail even though the implementation is correct. Because
    // a real http connection to a real ad-hoc http server is established, it
    // is possible that the used port (62843) is already in use.
    @Test
    fun `When a python fetcher uses the requests module, then it is able to fetch web resources`() = runTest {
        val server = HttpServer.create(InetSocketAddress(62843), 0)
        server.createContext("/", object : HttpHandler {
            override fun handle(t: HttpExchange) {
                val response = "foobar"
                t.sendResponseHeaders(200, response.length.toLong())
                with(t.responseBody) {
                    write(response.toByteArray())
                    close()
                }
            }
        })
        server.start()

        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            import requests

            availableOptions = {
                requests.get("http://127.0.0.1:62843/").text
            }
        """.trimIndent(), Path.of("."), mockk())

        val opts = fetcher.getAvailableOptions()
        assertEquals(setOf("foobar"), opts)

        server.stop(0)
    }

    @Test
    fun `When a python fetcher uses the exposed java fetcherManager, then it is able to make requests to other fetchers`() = runTest {
        val fetcherManager = mockk<FetcherManager>()

        coEvery { fetcherManager.getAvailableFetchers() } returns setOf("foo")
        coEvery { fetcherManager.getAvailableOptions("foo") } returns setOf("bar")
        coEvery { fetcherManager.searchPapers("foo", any(), any()) } returns setOf(examplePaper)

        val fetcher = PythonPluginFetcher.fromSource("test" ,"""
            availableOptions = fetchers.getAvailableOptions("foo")

            def searchPapers(searchQuery, options):
                return fetchers.searchPapers("foo", "x", { "y": "z" })
        """.trimIndent(), Path.of("."), fetcherManager)

        assertEquals(setOf("bar"), fetcher.getAvailableOptions())
        assert(fetcher.searchPapers("", mapOf()).any {
            examplePaper.title == it.title &&
            examplePaper.abstract == it.abstract &&
            examplePaper.externalId == it.externalId &&
            examplePaper.publishedAt?.epochSeconds == it.publishedAt?.epochSeconds &&
            examplePaper.publisher == it.publisher &&
            examplePaper.publicationType == it.publicationType &&
            examplePaper.publicationName == it.publicationName
        })

        coVerify {
            fetcherManager.getAvailableOptions("foo")
            fetcherManager.searchPapers("foo", "x", mapOf("y" to "z"))
        }
        confirmVerified(fetcherManager)
    }
}
