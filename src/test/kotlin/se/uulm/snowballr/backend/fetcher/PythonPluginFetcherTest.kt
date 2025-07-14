package se.uulm.snowballr.backend.fetcher

import jep.python.PyObject
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.model.dto.Paper
import java.time.OffsetDateTime
import java.util.UUID

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

internal class PythonPluginFetcherTest {
    companion object {
        @BeforeAll
        fun locateJepLibrary() {
            PythonPluginFetcher.locateNativeLibrary()
        }
    }

    @Test
    fun `When an options-map is convert to a PyObject, python should be able to correctly access it`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
            val opts = mapOf("foo" to "bar")
            interp.set("opts", opts.toPyObject(interp))
            assertEquals("bar", interp.getValue("""opts["foo"]""", String::class.java))
        }
    }

    @Test
    fun `When a python dictionary is convert to an options-map, java should be able to correctly access it`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
            val opts = interp.getValue("""{"foo": "bar"}""", PyObject::class.java).toOptionsMap()
            assertEquals("bar", opts.get("foo"))
        }
    }

    @Test
    fun `When a options-map is convert to a PyObject and back, it should stay the same`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
            val options = mapOf("foo" to "bar")
            assertEquals(options, options.toPyObject(interp).toOptionsMap())
        }
    }

    @Test
    fun `When a paper is convert to a python paper, python should be able to correctly access it`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
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
    fun `When a python paper is convert to a paper, java should be able to correctly access it`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
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
    fun `When a paper is convert to a python paper and back, it should stay the same`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
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
    fun `When python returns a string set, java can access it as a string set`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
            val set = interp.getValue("""{"foo", "bar"}""", PyObject::class.java).toSet<String>(interp)
            assertEquals(setOf("foo", "bar"), set)
        }
    }

    @Test
    fun `When python returns a paper set, java can access it as a paper set`() {
        PythonPluginFetcher.withNewInterpreter { interp ->
            val set = interp.getValue("""{Paper("foo", "bar"), Paper("x", "y")}""", PyObject::class.java).toSet<PyObject>(interp).map { it.toPaper() }.toSet()

            assertEquals(2, set.size)

            assert(set.any { it.title == "foo" && it.abstract == "bar" })
            assert(set.any { it.title == "x" && it.abstract == "y" })
        }
    }
}
