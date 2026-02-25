package se.uulm.snowballr.backend.fetcher

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.model.exception.UnauthorizedFetcherPathException
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.writeText
import kotlin.test.assertEquals

class PythonPluginFetcherManagerTest {
    private lateinit var pluginDirectory: Path
    private lateinit var fetcherDirectory: Path
    private lateinit var fetcherManager: PythonPluginFetcherManager

    private fun createSymlink(link: Path, target: Path) {
        Files.deleteIfExists(link)
        Files.createSymbolicLink(link, target)
    }

    @BeforeEach
    fun setUp() {
        pluginDirectory = Files.createTempDirectory("python-fetcher-test")
        fetcherDirectory = pluginDirectory.resolve("fetchers")
        fetcherManager = PythonPluginFetcherManager(createEnvReader(pluginDirectory))
    }

    @AfterEach
    fun tearDown() {
        fetcherDirectory.toFile().deleteRecursively()
    }

    @Test
    fun `When the manager is initialized, then bundled plugin resources are copied`() {
        assertThat(fetcherDirectory.resolve("IEEEXplore.py").exists()).isTrue()
        assertThat(fetcherDirectory.resolve("lib").resolve("snowballr.py").exists()).isTrue()
        assertThat(fetcherDirectory.resolve("lib").resolve("xploreapi.py").exists()).isTrue()
    }

    @Test
    fun `When available fetchers are queried, then only top-level python files are returned without extension`() {
        writeFetcher(
            "custom_fetcher",
            """
            import json
            print(json.dumps({}))
            """.trimIndent(),
        )
        fetcherDirectory.resolve("README.md").writeText("ignore")
        val nestedDir = fetcherDirectory.resolve("nested").createDirectories()
        nestedDir.resolve("nested_fetcher.py").writeText("print('[]')")

        val availableFetchers = fetcherManager.getAvailableFetchers()

        assertThat(availableFetchers).contains("IEEEXplore", "custom_fetcher")
        assertThat(availableFetchers).doesNotContain("README", "nested_fetcher")
    }

    @Test
    fun `When a fetcher exits successfully with options output, then options are returned`() = runTest {
        writeFetcher(
            "options_fetcher",
            """
            import json
            import sys

            if sys.argv[1] == "options":
                print(json.dumps({"limit": "25", "sort": "desc"}))
            else:
                print(json.dumps([]))
            """.trimIndent(),
        )

        val result = fetcherManager.getAvailableOptions("options_fetcher")

        assertEquals(
            mapOf(
                "limit" to "25",
                "sort" to "desc",
            ),
            result,
        )
    }

    @Test
    fun `When a fetcher returns papers, then query and reference methods decode them consistently`() = runTest {
        writeFetcher(
            "paper_fetcher",
            """
            import json
            import sys

            paper = [{
                "title": "Example title",
                "external_id": "external-42",
                "abstract": "Example abstract",
                "year": 2024,
                "publisher": "Example publisher",
                "publication_type": "journal",
                "publication_name": "Example publication",
                "authors": [{"first_name": "Ada", "last_name": "Lovelace"}],
                "metadata": {"id": "meta-1"}
            }]

            if sys.argv[1] == "options":
                print(json.dumps({"api_key": "required"}))
            elif sys.argv[1] in ("query", "forwards", "backwards"):
                print(json.dumps(paper))
            else:
                print("unsupported", file=sys.stderr)
                sys.exit(1)
            """.trimIndent(),
        )

        val expectedPaper = FetcherPaper(
            title = "Example title",
            externalId = "external-42",
            abstract = "Example abstract",
            year = 2024,
            publisher = "Example publisher",
            publicationType = "journal",
            publicationName = "Example publication",
            authors = listOf(Author("Ada", "Lovelace")),
            metadata = mapOf("id" to "meta-1"),
        )
        val requestedPaper = expectedPaper.copy(metadata = mapOf("id" to "meta-request"))

        val queryResult = fetcherManager.searchPapers("paper_fetcher", "query", mapOf("api_key" to "123"))
        val forwardResult = fetcherManager.fetchForwardReferences(
            "paper_fetcher",
            requestedPaper,
            mapOf("api_key" to "123"),
        )
        val backwardResult = fetcherManager.fetchBackwardReferences(
            "paper_fetcher",
            requestedPaper,
            mapOf("api_key" to "123"),
        )

        assertEquals(setOf(expectedPaper), queryResult)
        assertEquals(setOf(expectedPaper), forwardResult)
        assertEquals(setOf(expectedPaper), backwardResult)
    }

    @Test
    fun `When a fetcher script is missing, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.getAvailableOptions("does_not_exist")
        }
        assertThat(exception.message).contains("Fetcher \"does_not_exist\" not found.")
    }

    @Test
    fun `When a fetcher path traverses outside root, then an UnauthorizedFetcherPathException is thrown`() = runTest {
        val exception = assertThrows<UnauthorizedFetcherPathException> {
            fetcherManager.getAvailableOptions("../outside")
        }

        assertThat(exception.message).contains("Fetcher \"../outside\" is outside the configured fetchers directory.")
    }

    @Test
    fun `When a fetcher symlink points outside root, then an UnauthorizedFetcherPathException is thrown`() = runTest {
        val outsideDir = Files.createTempDirectory("python-fetcher-outside")
        val outsideTarget = outsideDir.resolve("outside_fetcher.py").apply {
            writeText(
                """
                import json
                import sys
                if sys.argv[1] == "options":
                    print(json.dumps({"ok": "nope"}))
                """.trimIndent(),
            )
        }

        // Link inside fetcherDirectory -> target outside
        createSymlink(
            link = fetcherDirectory.resolve("symlink_outside.py"),
            target = outsideTarget,
        )

        val exception = assertThrows<UnauthorizedFetcherPathException> {
            fetcherManager.getAvailableOptions("symlink_outside")
        }
        assertThat(exception.message)
            .contains("Fetcher \"symlink_outside\" is outside the configured fetchers directory.")

        outsideDir.toFile().deleteRecursively()
    }

    @Test
    fun `When a fetcher is a symlink to a file inside root, then it is allowed`() = runTest {
        writeFetcher(
            "target_fetcher",
            """
            import json
            import sys
            if sys.argv[1] == "options":
                print(json.dumps({"foo": "bar"}))
            """.trimIndent(),
        )

        // Link inside fetcherDirectory -> target inside
        createSymlink(
            link = fetcherDirectory.resolve("symlink_inside.py"),
            target = fetcherDirectory.resolve("target_fetcher.py"),
        )

        val options = fetcherManager.getAvailableOptions("symlink_inside")
        assertEquals(mapOf("foo" to "bar"), options)
    }

    @Test
    fun `When a fetcher name contains invalid path characters, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.getAvailableOptions("invalid\u0000name")
        }

        assertThat(exception.message).contains("Fetcher \"invalid")
        assertThat(exception.message).contains("not found.")
    }

    @Test
    fun `When the configured fetchers directory is missing, then a FetcherNotFoundException is thrown`() = runTest {
        fetcherDirectory.toFile().deleteRecursively()

        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.getAvailableOptions("IEEEXplore")
        }

        assertThat(exception.message).contains("Fetcher \"IEEEXplore\" not found.")
    }

    @Test
    fun `When a fetcher exits with an error code, then a FetcherException is thrown`() = runTest {
        writeFetcher(
            "failing_fetcher",
            """
            import sys
            print("something went wrong", file=sys.stderr)
            sys.exit(2)
            """.trimIndent(),
        )

        val exception = assertThrows<FetcherException> {
            fetcherManager.getAvailableOptions("failing_fetcher")
        }

        assertThat(exception.message).contains("something went wrong")
    }

    @Test
    fun `When a fetcher writes to stderr but exits successfully, then the result is still returned`() = runTest {
        writeFetcher(
            "warning_fetcher",
            """
            import json
            import sys

            print("warning", file=sys.stderr)
            print(json.dumps({"foo": "bar"}))
            """.trimIndent(),
        )

        val options = fetcherManager.getAvailableOptions("warning_fetcher")

        assertEquals(mapOf("foo" to "bar"), options)
    }

    @Test
    fun `When a fetcher writes multiple stdout lines, then only the first line is interpreted`() = runTest {
        writeFetcher(
            "multiline_fetcher",
            """
            import json

            print(json.dumps({"foo": "bar"}))
            print("ignored-second-line")
            """.trimIndent(),
        )

        val options = fetcherManager.getAvailableOptions("multiline_fetcher")

        assertEquals(mapOf("foo" to "bar"), options)
    }

    private fun writeFetcher(name: String, source: String) {
        fetcherDirectory.resolve("$name.py").writeText(source.trimIndent())
    }

    private fun createEnvReader(pluginDirectory: Path): EnvReader {
        val pluginEnv = mockk<Env.Plugins>()
        every { pluginEnv.pluginDirectory } returns pluginDirectory
        every { pluginEnv.pythonExecutable } returns "python3"

        val env = mockk<Env>()
        every { env.plugins } returns pluginEnv

        val envReader = mockk<EnvReader>()
        every { envReader.env } returns env
        return envReader
    }
}
