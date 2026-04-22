package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.exception.UnauthorizedFetcherPathException
import se.uulm.snowballr.backend.model.exception.notfound.FetcherNotFoundException
import java.io.IOException
import java.io.InputStream
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

private data class ProcessResult(
    val stdout: String,
    val stderr: String,
    val returnCode: Int,
)

@Serializable
private data class QueryPayload(
    @SerialName("search_query")
    val searchQuery: String,
    val options: Map<String, String>,
)

@Serializable
private data class ReferencePayload(
    val paper: FetcherPaper,
    val options: Map<String, String>,
)

class PythonPluginFetcherManager(
    envReader: EnvReader,
    resourceLoader: (String) -> InputStream? = { resource ->
        PythonPluginFetcherManager::class.java.getResourceAsStream(resource)
    },
    private val executionTimeoutMillis: Long = 30_000L,
    private val forceKillGraceMillis: Long = 1_000L,
) : IFetcherManager {
    private val root = envReader.env.plugins.pluginDirectory.resolve("fetchers")
    private val pythonExecutable = envReader.env.plugins.pythonExecutable

    init {
        logger.info {
            "Python fetcher setup: pythonExecutable='$pythonExecutable', pluginDirectory='${root.toAbsolutePath()}'"
        }

        root.createDirectories()

        for (resource in resources) {
            val stream = resourceLoader(resource)
            if (stream == null) {
                logger.error { "Bundled fetcher resource '$resource' is missing from classpath." }
                error("Bundled fetcher resource '$resource' is missing from classpath.")
            }

            val target = root.resolve(resource.removePrefix("/plugins/fetchers/"))
            target.parent.createDirectories()
            stream.bufferedReader().use { target.writeText(it.readText()) }
        }
    }

    override fun getAvailableFetchers(): Set<String> = root.listDirectoryEntries()
        .filter { it.extension == "py" }
        .map { it.nameWithoutExtension }
        .toSet()

    override suspend fun getAvailableOptions(fetcher: String): Map<String, String> =
        decodeFetcherJson(fetcher, execFetcher(fetcher, action = "options"))

    override suspend fun searchPapers(
        fetcher: String,
        searchQuery: String,
        options: Map<String, String>,
    ): Set<FetcherPaper> = decodeFetcherJson(
        fetcher,
        execFetcher(
            fetcher,
            action = "query",
            payload = Json.encodeToString(QueryPayload(searchQuery = searchQuery, options = options)),
        ),
    )

    override suspend fun fetchForwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> = decodeFetcherJson(
        fetcher,
        execFetcher(
            fetcher,
            action = "forwards",
            payload = Json.encodeToString(ReferencePayload(paper = paper, options = options)),
        ),
    )

    override suspend fun fetchBackwardReferences(
        fetcher: String,
        paper: FetcherPaper,
        options: Map<String, String>,
    ): Set<FetcherPaper> = decodeFetcherJson(
        fetcher,
        execFetcher(
            fetcher,
            action = "backwards",
            payload = Json.encodeToString(ReferencePayload(paper = paper, options = options)),
        ),
    )

    /**
     * Executes the python script of a given fetcher with the given command
     * line arguments. The output of the fetcher (which should constrain itself
     * to a single line) is then returned.
     *
     * The current contract expects the fetcher to return a single line of JSON.
     * The first command line argument represents the action to be performed.
     * Action payloads are serialized JSON and sent over stdin.
     *
     * - python fetcher.py options
     * - python fetcher.py query
     * - python fetcher.py backwards
     * - python fetcher.py forwards
     */
    @Suppress("ThrowsCount")
    private suspend fun execFetcher(
        fetcher: String,
        action: String,
        payload: String = "",
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
    ): String {
        val fetcherPath = resolveFetcherPath(fetcher)
        val process = createProcess(fetcherPath, action, dispatcher)
        writePayload(process, payload, dispatcher)
        val processResult = awaitProcessResult(fetcher, process, dispatcher)
        return parseProcessResult(fetcher, processResult)
    }

    /**
     * Creates and starts a Python fetcher process with the expected environment.
     */
    private suspend fun createProcess(fetcherPath: Path, action: String, dispatcher: CoroutineDispatcher): Process {
        val processBuilder = ProcessBuilder(pythonExecutable, fetcherPath.toString(), action)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .redirectInput(ProcessBuilder.Redirect.PIPE)
            .also { it.environment()["PYTHONPATH"] = root.resolve("lib").toAbsolutePath().toString() }
        return withContext(dispatcher) {
            processBuilder.start()
        }
    }

    /**
     * Writes a JSON payload to the process stdin and then closes the input stream.
     */
    private suspend fun writePayload(process: Process, payload: String, dispatcher: CoroutineDispatcher) {
        withContext(dispatcher) {
            process.outputWriter().use { writer ->
                if (payload.isNotEmpty()) {
                    writer.write(payload)
                }
            }
        }
    }

    /**
     * Waits for the process to finish, enforcing timeout and force-kill grace period.
     */
    private suspend fun awaitProcessResult(
        fetcher: String,
        process: Process,
        dispatcher: CoroutineDispatcher,
    ): ProcessResult = coroutineScope {
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

        ProcessResult(
            stdout = stdoutDeferred.await(),
            stderr = stderrDeferred.await(),
            returnCode = process.exitValue(),
        )
    }

    /**
     * Interprets the finished process result and returns the fetcher payload line.
     */
    private fun parseProcessResult(fetcher: String, processResult: ProcessResult): String {
        val (stdout, stderr, returnCode) = processResult
        if (returnCode == 0) {
            if (!stderr.isBlank()) logger.info { "Fetcher '$fetcher' encountered a problem: $stderr" }
            val output = stdout.lineSequence().firstOrNull()?.trim().orEmpty()
            if (output.isBlank()) {
                throw FetcherException("Fetcher '$fetcher' returned no JSON output.")
            }
            return output
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

    /**
     * Decodes fetcher JSON output and maps parse failures to a fetcher-specific exception.
     */
    private inline fun <reified T> decodeFetcherJson(fetcher: String, input: String): T = try {
        Json.decodeFromString<T>(input)
    } catch (exception: SerializationException) {
        throw FetcherException("Fetcher '$fetcher' returned invalid JSON.", exception)
    }
}
