package se.uulm.snowballr.backend.fetcher

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.exception.FetcherException
import se.uulm.snowballr.backend.model.exception.UnauthorizedFetcherPathException
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.jvm.optionals.getOrElse
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class PythonPluginFetcherManagerTest {
    private lateinit var pluginDirectory: Path
    private lateinit var fetcherDirectory: Path
    private lateinit var fetcherManager: PythonPluginFetcherManager

    private fun createSymlink(link: Path, target: Path) {
        Files.deleteIfExists(link)
        Files.createSymbolicLink(link, target)
    }

    private suspend fun PythonPluginFetcherManager.makeTestCall(fetcherName: String) =
        this.searchPapers(fetcherName, "", emptyMap())

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
        assertThat(fetcherDirectory.resolve("SemanticScholar.py").exists()).isTrue()
        assertThat(fetcherDirectory.resolve("OpenAlex.py").exists()).isTrue()
        assertThat(fetcherDirectory.resolve("lib").resolve("snowballr.py").exists()).isTrue()
        assertThat(fetcherDirectory.resolve("lib").resolve("xploreapi.py").exists()).isTrue()
    }

    @Test
    fun `When available fetchers are queried, then only top-level python files are returned without extension`() =
        runTest {
            writeFetcher(
                "custom_fetcher",
                """
                import json
                import sys
                if sys.argv[1] == "info":
                    print(json.dumps({"name": "custom_fetcher", "description": "test", "links": [], "options_schema": {}}))
                else:
                    print(json.dumps([]))
                """.trimIndent(),
            )
            fetcherDirectory.resolve("README.md").writeText("ignore")
            val nestedDir = fetcherDirectory.resolve("nested").createDirectories()
            nestedDir.resolve("nested_fetcher.py").writeText("print('[]')")

            val availableFetchers = fetcherManager.getAvailableFetchers().map { it.id }

            assertThat(availableFetchers).contains("IEEEXplore", "SemanticScholar", "OpenAlex", "custom_fetcher")
            assertThat(availableFetchers).doesNotContain("README", "nested_fetcher")
        }

    @Test
    fun `When a fetcher is queried with invalid FetcherInformation, then it is filtered out`() = runTest {
        writeFetcher(
            "custom_fetcher",
            """
            import json
            import sys
            if sys.argv[1] == "info":
                # missing links and options_schema
                print(json.dumps({"name": "custom_fetcher", "description": "test"}))
            else:
                print(json.dumps([]))
            """.trimIndent(),
        )

        val availableFetchers = fetcherManager.getAvailableFetchers().map { it.id }

        assertThat(availableFetchers).doesNotContain("custom_fetcher")
    }

    @Test
    fun `When a fetcher exits successfully with information output, then information is returned`() = runTest {
        writeFetcher(
            "information_fetcher",
            """
            import json
            import sys

            if sys.argv[1] == "info":
                print(json.dumps({"name": "test", "description": "desc", "links": [], "options_schema": {}}))
            else:
                print(json.dumps([]))
            """.trimIndent(),
        )

        val result = fetcherManager.getAvailableFetchers().first { it.name == "test" }

        assertEquals("information_fetcher", result.id)
        assertEquals("test", result.name)
        assertEquals("desc", result.description)
        assertEquals(emptyList(), result.linksList)
        assertEquals(emptyMap(), result.optionsSchemaMap)
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
                "fetcher_metadata": {"id": "meta-1"}
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
            fetcherMetadata = mapOf("id" to "meta-1"),
        )
        val requestedPaper = expectedPaper.copy(fetcherMetadata = mapOf("id" to "meta-request"))

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
    fun `When querying papers, then payload is sent via stdin and not as CLI args`() = runTest {
        writeFetcher(
            "stdin_payload_fetcher",
            """
            import json
            import sys

            paper = [{
                "title": "stdin title",
                "external_id": "stdin-1",
                "abstract": "stdin abstract",
                "year": 2024,
                "publisher": "stdin publisher",
                "publication_type": "journal",
                "publication_name": "stdin publication",
                "authors": [{"first_name": "Grace", "last_name": "Hopper"}],
                "fetcher_metadata": {"id": "stdin-meta"}
            }]

            if sys.argv[1] == "query":
                if len(sys.argv) != 2:
                    print("query payload leaked to CLI", file=sys.stderr)
                    sys.exit(1)

                payload = json.loads(sys.stdin.read())
                if payload.get("search_query") != "find-me":
                    print("missing query payload", file=sys.stderr)
                    sys.exit(1)
                if payload.get("options", {}).get("api_key") != "super-secret":
                    print("missing options payload", file=sys.stderr)
                    sys.exit(1)

                print(json.dumps(paper))
            elif sys.argv[1] == "options":
                print(json.dumps({"api_key": "required"}))
            else:
                print("unsupported", file=sys.stderr)
                sys.exit(1)
            """.trimIndent(),
        )

        val expectedPaper = FetcherPaper(
            title = "stdin title",
            externalId = "stdin-1",
            abstract = "stdin abstract",
            year = 2024,
            publisher = "stdin publisher",
            publicationType = "journal",
            publicationName = "stdin publication",
            authors = listOf(Author("Grace", "Hopper")),
            fetcherMetadata = mapOf("id" to "stdin-meta"),
        )

        val queryResult = fetcherManager.searchPapers(
            "stdin_payload_fetcher",
            "find-me",
            mapOf("api_key" to "super-secret"),
        )

        assertEquals(setOf(expectedPaper), queryResult)
    }

    @Test
    fun `When a fetcher script is missing, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.makeTestCall("does_not_exist")
        }
        assertThat(exception.message).contains("Fetcher \"does_not_exist\" not found.")
    }

    @Test
    fun `When a fetcher path resolves to a directory, then a FetcherNotFoundException is thrown`() = runTest {
        fetcherDirectory.resolve("not_a_file.py").createDirectories()

        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.makeTestCall("not_a_file")
        }

        assertThat(exception.message).contains("Fetcher \"not_a_file\" not found.")
    }

    @Test
    fun `When a fetcher path traverses outside root, then an UnauthorizedFetcherPathException is thrown`() = runTest {
        val exception = assertThrows<UnauthorizedFetcherPathException> {
            fetcherManager.makeTestCall("../outside")
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
            fetcherManager.makeTestCall("symlink_outside")
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
            if sys.argv[1] == "query":
                print(json.dumps([]))
            """.trimIndent(),
        )

        // Link inside fetcherDirectory -> target inside
        createSymlink(
            link = fetcherDirectory.resolve("symlink_inside.py"),
            target = fetcherDirectory.resolve("target_fetcher.py"),
        )

        val options = fetcherManager.searchPapers("symlink_inside", "", emptyMap())

        assertEquals(emptySet(), options)
    }

    @Test
    fun `When a fetcher name contains invalid path characters, then a FetcherNotFoundException is thrown`() = runTest {
        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.makeTestCall("invalid\u0000name")
        }

        assertThat(exception.message).contains("Fetcher \"invalid")
        assertThat(exception.message).contains("not found.")
    }

    @Test
    fun `When the configured fetchers directory is missing, then a FetcherNotFoundException is thrown`() = runTest {
        fetcherDirectory.toFile().deleteRecursively()

        val exception = assertThrows<FetcherNotFoundException> {
            fetcherManager.makeTestCall("IEEEXplore")
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
            fetcherManager.makeTestCall("failing_fetcher")
        }

        assertThat(exception.message).contains("something went wrong")
    }

    @Test
    fun `When a fetcher returns invalid JSON, then a FetcherException is thrown`() = runTest {
        writeFetcher(
            "invalid_json_fetcher",
            """
            if True:
                print("{invalid-json")
            """.trimIndent(),
        )

        val exception = assertThrows<FetcherException> {
            fetcherManager.makeTestCall("invalid_json_fetcher")
        }

        assertThat(exception.message).contains("Fetcher 'invalid_json_fetcher' returned invalid JSON.")
        assertThat(exception.cause).isNotNull()
    }

    @Test
    fun `When a fetcher returns no stdout payload, then a FetcherException is thrown`() = runTest {
        writeFetcher(
            "empty_output_fetcher",
            """
            if True:
                print("")
            """.trimIndent(),
        )

        val exception = assertThrows<FetcherException> {
            fetcherManager.makeTestCall("empty_output_fetcher")
        }

        assertThat(exception.message).contains("Fetcher 'empty_output_fetcher' returned no JSON output.")
    }

    @Test
    fun `When a fetcher exceeds the configured timeout, then a timeout FetcherException is thrown`() = runTest {
        val timeoutFetcherManager = PythonPluginFetcherManager(
            createEnvReader(pluginDirectory),
            executionTimeoutMillis = 100L,
            forceKillGraceMillis = 50L,
        )

        writeFetcher(
            "slow_fetcher",
            """
            import sys
            import time

            if sys.argv[1] == "query":
                time.sleep(10)
            """.trimIndent(),
        )

        val exception = assertThrows<FetcherException> {
            timeoutFetcherManager.searchPapers("slow_fetcher", "", emptyMap())
        }

        assertThat(exception.message).contains("Fetcher 'slow_fetcher' timed out after 100ms.")
    }

    @Test
    fun `When a timed out fetcher ignores SIGTERM, then it is force killed after grace period`() = runTest {
        val timeoutFetcherManager = PythonPluginFetcherManager(
            createEnvReader(pluginDirectory),
            executionTimeoutMillis = 2_000L,
            forceKillGraceMillis = 100L,
        )
        val pidFile = fetcherDirectory.resolve("stuck_fetcher.pid")
        val escapedPidFilePath = pidFile.toString().replace("\\", "\\\\")

        writeFetcher(
            "stuck_fetcher",
            """
            import os
            import signal
            import sys
            import time

            pid_file = "$escapedPidFilePath"
            with open(pid_file, "w", encoding="utf-8") as file:
                file.write(str(os.getpid()))

            def ignore_sigterm(_signal_number, _frame):
                return

            signal.signal(signal.SIGTERM, ignore_sigterm)

            if sys.argv[1] == "query":
                while True:
                    time.sleep(0.05)
            """.trimIndent(),
        )

        val exception = assertThrows<FetcherException> {
            timeoutFetcherManager.searchPapers("stuck_fetcher", "", emptyMap())
        }
        assertThat(exception.message).contains("Fetcher 'stuck_fetcher' timed out after 2000ms.")

        assertTrue(
            waitUntil(
                timeoutMillis = 1_500L,
                condition = { pidFile.exists() },
            ),
        )
        val pid = pidFile.readText().trim().toLong()
        val processHandle = ProcessHandle.of(pid)

        val isProcessTerminated = waitUntil(
            timeoutMillis = 3_000L,
            condition = { processHandle.map { !it.isAlive }.getOrElse { true } },
        )
        assertTrue(isProcessTerminated)
    }

    @Test
    fun `When a fetcher writes to stderr but exits successfully, then the result is still returned`() = runTest {
        writeFetcher(
            "warning_fetcher",
            """
            import json
            import sys

            print("warning", file=sys.stderr)
            print(json.dumps([]))
            """.trimIndent(),
        )

        val options = fetcherManager.searchPapers("warning_fetcher", "", emptyMap())

        assertEquals(emptySet(), options)
    }

    @Test
    fun `When a fetcher writes multiple stdout lines, then only the first line is interpreted`() = runTest {
        writeFetcher(
            "multiline_fetcher",
            """
            import json

            print(json.dumps([]))
            print("ignored-second-line")
            """.trimIndent(),
        )

        val options = fetcherManager.searchPapers("multiline_fetcher", "", emptyMap())

        assertEquals(emptySet(), options)
    }

    @Test
    fun `When a fetcher option with a default value is not provided, then the default is injected`() = runTest {
        writeFetcher(
            "defaults_fetcher",
            """
            from snowballr import FetcherInformation, FetcherOptionsSchema, Paper, fetcher_plugin

            info = FetcherInformation(
                name="defaults_fetcher",
                description="test",
                links=[],
                options_schema={
                    "MY_OPTION": FetcherOptionsSchema(
                        name="test",
                        description="test description",
                        default_value="injected_default",
                    ),
                },
            )

            def search(query, options):
                return [Paper(title=options.get("MY_OPTION", "NOT_INJECTED"))]

            def refs(paper, options):
                return []

            fetcher_plugin(information=info, query=search, forwards=refs, backwards=refs)
            """.trimIndent(),
        )

        val result = fetcherManager.searchPapers("defaults_fetcher", "", emptyMap())

        assertEquals(1, result.size)
        assertEquals("injected_default", result.first().title)
    }

    @Test
    fun `When a fetcher option with a default value is explicitly provided, then the provided value is used`() =
        runTest {
            writeFetcher(
                "override_fetcher",
                """
                from snowballr import FetcherInformation, FetcherOptionsSchema, Paper, fetcher_plugin

                info = FetcherInformation(
                    name="override_fetcher",
                    description="test",
                    links=[],
                    options_schema={
                        "MY_OPTION": FetcherOptionsSchema(
                            name="test",
                            description="test description",
                            default_value="injected_default",
                        ),
                    },
                )

                def search(query, options):
                    return [Paper(title=options.get("MY_OPTION", "NOT_INJECTED"))]

                def refs(paper, options):
                    return []

                fetcher_plugin(information=info, query=search, forwards=refs, backwards=refs)
                """.trimIndent(),
            )

            val result = fetcherManager.searchPapers("override_fetcher", "", mapOf("MY_OPTION" to "caller_value"))

            assertEquals(1, result.size)
            assertEquals("caller_value", result.first().title)
        }

    private fun writeFetcher(name: String, source: String) {
        fetcherDirectory.resolve("$name.py").writeText(source.trimIndent())
    }

    private suspend fun waitUntil(
        timeoutMillis: Long,
        pollIntervalMillis: Long = 25L,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            delay(pollIntervalMillis.milliseconds)
        }
        return condition()
    }

    private fun createEnvReader(pluginDirectory: Path): EnvReader {
        val venvPython = Path.of(".venv/bin/python3").toAbsolutePath()
        val pythonExecutable = if (venvPython.exists()) venvPython.toString() else "python3"

        val pluginEnv = mockk<Env.Plugins>()
        every { pluginEnv.pluginDirectory } returns pluginDirectory
        every { pluginEnv.pythonExecutable } returns pythonExecutable

        val env = mockk<Env>()
        every { env.plugins } returns pluginEnv

        val envReader = mockk<EnvReader>()
        every { envReader.env } returns env
        return envReader
    }
}
