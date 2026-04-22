package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.exception.UnauthorizedFetcherPathException
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import java.io.IOException
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

private val logger = KotlinLogging.logger {}

// Bundled python scripts and libraries to bootstrap python fetcher infrastructure.
private val resources = buildResources("/plugins/fetchers") {
    file("IEEEXplore.py")
    dir("lib") {
        file("xploreapi.py")
        file("snowballr.py")
    }
}

class PythonPluginFetcherManager(
    envReader: EnvReader,
    private val executionTimeoutMillis: Long = 30_000L,
    private val forceKillGraceMillis: Long = 1_000L,
) : IFetcherManager {
    private val root = envReader.env.plugins.pluginDirectory.resolve("fetchers")
    private val pythonExecutable = envReader.env.plugins.pythonExecutable

    init {
        logger.info {
            "Python fetcher setup: pythonExecutable='$pythonExecutable', pluginDirectory='${root.toAbsolutePath()}'"
        }

        for (resource in resources) {
            val target = root.resolve(resource.removePrefix("/plugins/fetchers/"))
            val stream = this::class.java.getResourceAsStream(resource)

            target.parent.createDirectories()
            target.writeText(stream.bufferedReader().readText())
        }
    }

    override fun getAvailableFetchers(): Set<String> = root.listDirectoryEntries()
        .filter { it.extension == "py" }
        .map { it.nameWithoutExtension }
        .toSet()

    override suspend fun getAvailableOptions(fetcher: String): Map<String, String> =
        Json.decodeFromString<Map<String, String>>(
            execFetcher(fetcher, "options"),
        )

    override suspend fun searchPapers(
        fetcher: String,
        searchQuery: String,
        options: Map<String, String>,
    ): Set<FetcherPaper> = Json.decodeFromString<Set<FetcherPaper>>(
        execFetcher(
            fetcher,
            "query",
            searchQuery,
            Json.encodeToString(options),
        ),
    )

    override suspend fun fetchForwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> = Json.decodeFromString<Set<FetcherPaper>>(
        execFetcher(
            fetcher,
            "forwards",
            Json.encodeToString(paper),
            Json.encodeToString(options),
        ),
    )

    override suspend fun fetchBackwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> = Json.decodeFromString<Set<FetcherPaper>>(
        execFetcher(
            fetcher,
            "backwards",
            Json.encodeToString(paper),
            Json.encodeToString(options),
        ),
    )

    /**
     * Executes the python script of a given fetcher with the given command
     * line arguments. The output of the fetcher (which should constrain itself
     * to a single line) is then returned.
     *
     * The current contract expects the fetcher to return a single line of JSON.
     * Furthermore, the first command line argument represents the action to be
     * performed (query, forwards, backwards) and the rest of the cli arguments
     * are the JSON serialized representations of the action arguments:
     *
     * - python fetcher.py options
     * - python fetcher.py query <SEARCH_QUERY> <OPTIONS>
     * - python fetcher.py backwards <PAPER> <OPTIONS>
     * - python fetcher.py forwards <PAPER> <OPTIONS>
     */
    private suspend fun execFetcher(
        fetcher: String,
        vararg args: String,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): String {
        val fetcherPath = resolveFetcherPath(fetcher)
        val processBuilder = ProcessBuilder(pythonExecutable, fetcherPath.toString(), *args)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .also { it.environment().put("PYTHONPATH", root.resolve("lib").toAbsolutePath().toString()) }
        val process = withContext(dispatcher) { processBuilder.start() }

        val (stdout, stderr, returnCode) = coroutineScope {
            val stdoutDeferred = async(dispatcher) { process.inputReader().use { it.readText() } }
            val stderrDeferred = async(dispatcher) { process.errorReader().use { it.readText() } }

            val finishedInTime = withContext(dispatcher) {
                process.waitFor(executionTimeoutMillis, TimeUnit.MILLISECONDS)
            }

            if (!finishedInTime) {
                process.destroy()
                withContext(dispatcher) {
                    if (!process.waitFor(forceKillGraceMillis, TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly()
                    }
                }
                throw FetcherException("Fetcher '$fetcher' timed out after ${executionTimeoutMillis}ms.")
            }

            Triple(stdoutDeferred.await(), stderrDeferred.await(), process.exitValue())
        }

        if (returnCode == 0) {
            if (!stderr.isBlank()) logger.info { "Fetcher '$fetcher' encountered a problem: $stderr" }
            return stdout.lineSequence().first()
        } else {
            logger.error { "Could not correctly execute fetcher '$fetcher':\n$stderr" }
            throw FetcherException(stderr)
        }
    }

    /**
     * Resolves the on-disk path and validates that it is safe to execute.
     *
     * The script must be a direct child of the configured fetchers directory. Symlinks are allowed,
     * but the fully resolved target must still reside in that directory.
     *
     * @param fetcher Base name of the fetcher script (without `.py` extension)
     * @return Path to the validated fetcher script
     * @throws UnauthorizedFetcherPathException if the fetcher path traverses outside the configured directory.
     * @throws FetcherNotFoundException for missing fetchers and all other path resolution errors.
     */
    @Suppress("ThrowsCount")
    private fun resolveFetcherPath(fetcher: String): Path {
        return runCatching {
            val normalizedRoot = root.normalize()
            val fetcherPath = normalizedRoot.resolve("$fetcher.py").normalize()

            if (fetcherPath.parent != normalizedRoot) {
                throw UnauthorizedFetcherPathException(fetcher)
            }

            val rootReal = normalizedRoot.toRealPath()
            val fetcherReal = fetcherPath.toRealPath()

            if (fetcherReal.parent != rootReal) {
                throw UnauthorizedFetcherPathException(fetcher)
            }

            fetcherPath
        }.getOrElse { exception ->
            when (exception) {
                is UnauthorizedFetcherPathException -> throw exception
                is InvalidPathException -> throw FetcherNotFoundException(fetcher)
                is IOException -> throw FetcherNotFoundException(fetcher)
                else -> throw exception
            }
        }
    }
}
