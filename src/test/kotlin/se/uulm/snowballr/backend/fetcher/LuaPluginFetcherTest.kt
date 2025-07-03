package se.uulm.snowballr.backend.fetcher

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.datetime.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.service.MainServiceTest
import se.uulm.snowballr.backend.testCoroutine
import java.time.OffsetDateTime
import java.util.UUID

@ExperimentalCoroutinesApi
@DelicateCoroutinesApi
@Suppress("StringTemplateIndent", "Indentation")
internal class LuaPluginFetcherTest : MainServiceTest() {
    @Test
    fun `When a plugin function is missing, then an exception is thrown`() = testCoroutine {
        assertThrows<Exception> { PluginBuilder().withNoOptions().build() }
        assertThrows<Exception> { PluginBuilder().withNoSearchPapers().build() }
        assertThrows<Exception> { PluginBuilder().withNoFetchCitations().build() }
        assertThrows<Exception> { PluginBuilder().withNoFetchReferences().build() }
    }

    @Test
    fun `When all plugin functions are defined, then no exception is thrown`() = testCoroutine {
        assertDoesNotThrow { PluginBuilder().build() }
    }

    @Test
    fun `When there are an incorrect number of function parameters, then an exception is thrown`() = testCoroutine {
        assertThrows<Exception> {
            PluginBuilder().withRawSearchPapers(
                """
                function searchPapers ()
                end
                """.trimIndent(),
            ).build().searchPapers("", mapOf())
        }
        assertThrows<Exception> {
            PluginBuilder().withRawSearchPapers(
                """
                function searchPapers (a, b, c)
                end
                """.trimIndent(),
            ).build().searchPapers("", mapOf())
        }
    }

    @Test
    fun `When the option table has the wrong type in lua, then an exception is thrown`() = testCoroutine {
        assertThrows<Exception> {
            PluginBuilder()
                .withRawOptions("availableOptions = \"test\"")
                .build()
                .getAvailableOptions()
        }
    }

    @Test
    fun `When an option is specified in lua, then it is returned`() = testCoroutine {
        val option = "test"
        val options = PluginBuilder().withOptions(option).build().getAvailableOptions()
        assertEquals(1, options.size)
        assertEquals(option, options.iterator().next())
    }

    @Test
    fun `When no lua table is returned, then an exception is thrown`() = testCoroutine {
        assertThrows<Exception> {
            PluginBuilder()
                .withSearchPapers("return \"test\"")
                .build()
                .searchPapers("", mapOf())
        }
    }

    @Test
    fun `When a returned paper has unknown keys, then an exception is thrown`() = testCoroutine {
        assertThrows<Exception> {
            PluginBuilder()
                .withSearchPapers("return { unknown = \"Unknown\" }")
                .build()
                .searchPapers("", mapOf())
        }
    }

    @Test
    fun `When searchPapers returns a singular full paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withSearchPapers(singleFullPaperImpl)
            .build()
            .searchPapers("", mapOf())
            .verifySingleFullPaper()
    }

    @Test
    fun `When searchPapers returns a partial paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withSearchPapers(singlePartialPaperImpl)
            .build()
            .searchPapers("", mapOf())
            .verifySinglePartialPaper()
    }

    @Test
    fun `When fetchReferences returns a singular full paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchReferences(singleFullPaperImpl)
            .build()
            .fetchReferences(emptyPaper, mapOf())
            .verifySingleFullPaper()
    }

    @Test
    fun `When fetchReferences returns a partial paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchReferences(singlePartialPaperImpl)
            .build()
            .fetchReferences(emptyPaper, mapOf())
            .verifySinglePartialPaper()
    }

    @Test
    fun `When fetchCitations returns a singular full paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchCitations(singleFullPaperImpl)
            .build()
            .fetchCitations(emptyPaper, mapOf())
            .verifySingleFullPaper()
    }

    @Test
    fun `When fetchCitations returns a partial paper, then it is correctly transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchCitations(singlePartialPaperImpl)
            .build()
            .fetchCitations(emptyPaper, mapOf())
            .verifySinglePartialPaper()
    }

    @Test
    fun `When searchPapers returns multiple papers, then multiple papers are transferred`() = testCoroutine {
        PluginBuilder()
            .withSearchPapers(multiplePapersImpl)
            .build()
            .searchPapers("", mapOf())
            .verifyMultiplePapers()
    }

    @Test
    fun `When fetchReferences returns multiple papers, then multiple papers are transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchReferences(multiplePapersImpl)
            .build()
            .fetchReferences(emptyPaper, mapOf())
            .verifyMultiplePapers()
    }

    @Test
    fun `When fetchCitations returns multiple papers, then multiple papers are transferred`() = testCoroutine {
        PluginBuilder()
            .withFetchCitations(multiplePapersImpl)
            .build()
            .fetchCitations(emptyPaper, mapOf())
            .verifyMultiplePapers()
    }

    @Test
    fun `When searchPapers is provdided with options, then the lua function can access them`() = testCoroutine {
        val testPair = "test" to "test"
        PluginBuilder()
            .withSearchPapers(optionBasedPaperImpl(testPair.first))
            .build()
            .searchPapers("", mapOf(testPair))
            .verifyOptionBasedPaper(testPair.second)
    }

    @Test
    fun `When fetchReferences is provdided with options, then the lua function can access them`() = testCoroutine {
        val testPair = "test" to "test"
        PluginBuilder()
            .withFetchReferences(optionBasedPaperImpl(testPair.first))
            .build()
            .fetchReferences(emptyPaper, mapOf(testPair))
            .verifyOptionBasedPaper(testPair.second)
    }

    @Test
    fun `When fetchCitations is provdided with options, then the lua function can access them`() = testCoroutine {
        val testPair = "test" to "test"
        PluginBuilder()
            .withFetchCitations(optionBasedPaperImpl(testPair.first))
            .build()
            .fetchCitations(emptyPaper, mapOf(testPair))
            .verifyOptionBasedPaper(testPair.second)
    }

    @Test
    fun `When a search query is provided, then the searchPapers lua function can access it`() = testCoroutine {
        val searchQuery = "test"
        val papers = PluginBuilder().withSearchPapers(
            """
            return {
                { title = searchQuery },
            }
            """.trimIndent(),
        ).build().searchPapers(searchQuery, mapOf())

        assertEquals(1, papers.size)
        assertEquals(searchQuery, papers.iterator().next().title)
    }

    @Test
    fun `When a paper is provided, then the fetchReferences lua function can access it`() = testCoroutine {
        val papers = PluginBuilder().withFetchReferences(
            """
            return {
                { title = paper.title },
            }
            """.trimIndent(),
        ).build().fetchReferences(emptyPaper, mapOf())

        assertEquals(1, papers.size)
        assertEquals(emptyPaper.title, papers.iterator().next().title)
    }

    @Test
    fun `When a paper is provided, then the fetchCitations lua function can access it`() = testCoroutine {
        val papers = PluginBuilder().withFetchCitations(
            """
            return {
                { title = paper.title },
            }
            """.trimIndent(),
        ).build().fetchCitations(emptyPaper, mapOf())

        assertEquals(1, papers.size)
        assertEquals(emptyPaper.title, papers.iterator().next().title)
    }

    class PluginBuilder {
        private var optionsImpl: String? = null
        private var searchPapersImpl: String? = null
        private var fetchReferencesImpl: String? = null
        private var fetchCitationsImpl: String? = null

        init {
            this
                .withDefaultOptions()
                .withDefaultSearchPapers()
                .withDefaultFetchCitations()
                .withDefaultFetchReferences()
        }

        fun build(): LuaPluginFetcher = LuaPluginFetcher.fromSource(
            arrayOf(
                optionsImpl,
                searchPapersImpl,
                fetchReferencesImpl,
                fetchCitationsImpl,
            ).filterNotNull()
                .joinToString("\n"),
        )

        fun clone(): PluginBuilder {
            val builder = PluginBuilder()
            builder.optionsImpl = this.optionsImpl
            builder.searchPapersImpl = this.searchPapersImpl
            builder.fetchReferencesImpl = this.fetchReferencesImpl
            builder.fetchCitationsImpl = this.fetchCitationsImpl
            return builder
        }

        fun withNoDefaults(): PluginBuilder = this
            .withNoOptions()
            .withNoSearchPapers()
            .withNoFetchCitations()
            .withNoFetchReferences()

        fun withOptions(vararg options: String): PluginBuilder = withRawOptions(
            options.map { "\"${it}\"" }.joinToString(
                ", ",
                "availableOptions = { ",
                " }",
            ),
        )

        fun withSearchPapers(impl: String): PluginBuilder =
            withRawSearchPapers(function("searchPapers", impl, "searchQuery", "options"))

        fun withFetchReferences(impl: String): PluginBuilder =
            withRawFetchReferences(function("fetchReferences", impl, "paper", "options"))

        fun withFetchCitations(impl: String): PluginBuilder =
            withRawFetchCitations(function("fetchCitations", impl, "paper", "options"))

        fun withRawOptions(impl: String?): PluginBuilder {
            this.optionsImpl = impl
            return this
        }

        fun withRawSearchPapers(impl: String?): PluginBuilder {
            this.searchPapersImpl = impl
            return this
        }

        fun withRawFetchReferences(impl: String?): PluginBuilder {
            this.fetchReferencesImpl = impl
            return this
        }

        fun withRawFetchCitations(impl: String?): PluginBuilder {
            this.fetchCitationsImpl = impl
            return this
        }

        fun withDefaultOptions(): PluginBuilder {
            this.optionsImpl = "availableOptions = {}"
            return this
        }

        fun withDefaultSearchPapers(): PluginBuilder =
            withRawSearchPapers(emptyFunction("searchPapers", "searchQuery", "options"))

        fun withDefaultFetchReferences(): PluginBuilder =
            withRawFetchReferences(emptyFunction("fetchReferences", "paper", "options"))

        fun withDefaultFetchCitations(): PluginBuilder =
            withRawFetchCitations(emptyFunction("fetchCitations", "paper", "options"))

        fun withNoOptions(): PluginBuilder = withRawOptions(null)

        fun withNoSearchPapers(): PluginBuilder = withRawSearchPapers(null)

        fun withNoFetchReferences(): PluginBuilder = withRawFetchReferences(null)

        fun withNoFetchCitations(): PluginBuilder = withRawFetchCitations(null)

        private fun function(name: String, body: String, vararg params: String): String = """
            function $name(${params.joinToString(", ")})
                $body
            end
        """.trimIndent()

        private fun emptyFunction(name: String, vararg params: String): String = function(name, "return {}", *params)
    }

    private val singleFullPaperImpl =
        """
        return {
            {
                title = "Title",
                externalId = "ExternalId",
                abstract = "Abstract",
                publishedAt = 0,
                publisher = "Publisher",
                publicationType = "PublicationType",
                publicationName = "PublicationName",
            },
        }
        """.trimIndent()

    private fun Set<Paper>.verifySingleFullPaper() {
        assertEquals(1, this.size)
        val paper = this.iterator().next()

        with(paper) {
            assertEquals(title, "Title")
            assertEquals(externalId, "ExternalId")
            assertEquals(abstract, "Abstract")
            assertEquals(publishedAt, Instant.fromEpochSeconds(0))
            assertEquals(publisher, "Publisher")
            assertEquals(publicationType, "PublicationType")
            assertEquals(publicationName, "PublicationName")
        }
    }

    private val singlePartialPaperImpl =
        """
        return {
            {
                title = "Title",
                externalId = "ExternalId",
                abstract = "Abstract",
            },
        }
        """.trimIndent()

    private fun Set<Paper>.verifySinglePartialPaper() {
        assertEquals(1, this.size)
        val paper = this.iterator().next()

        with(paper) {
            assertEquals(title, "Title")
            assertEquals(externalId, "ExternalId")
            assertEquals(abstract, "Abstract")
        }
    }

    private val multiplePapersImpl =
        """
        return {
            { title = "Title1" },
            { title = "Title2" },
        }
        """.trimIndent()

    private fun Set<Paper>.verifyMultiplePapers() {
        assertEquals(2, this.size)

        val iter = this.iterator()
        val paper1 = iter.next()
        val paper2 = iter.next()
        assertEquals("Title1", paper1.title)
        assertEquals("Title2", paper2.title)
    }

    private fun optionBasedPaperImpl(option: String) = """
        return {
            { title = options["$option"] },
        }
    """.trimIndent()

    private fun Set<Paper>.verifyOptionBasedPaper(optionValue: String) {
        assertEquals(1, this.size)
        assertEquals(optionValue, this.iterator().next().title)
    }

    private val emptyPaper = Paper(
        UUID.randomUUID(),
        "Title",
        null,
        "Abstract",
        null,
        null,
        null,
        null,
        null,
        OffsetDateTime.now(),
        null,
        null,
    )
}
