package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import jep.Jep
import jep.MainInterpreter
import jep.SharedInterpreter
import jep.python.PyBuiltins
import jep.python.PyCallable
import jep.python.PyObject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import se.uulm.snowballr.backend.model.dto.Paper
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.Executors
import kotlin.io.path.readText

private val logger = KotlinLogging.logger {}

@Suppress("StringTemplateIndent", "Indentation")
val jepLocatorScript = """
    import site
    import os
    import glob
    for f in glob.glob(os.path.join(site.getsitepackages()[0], "jep/libjep.*")):
        print(f)
    """.trimIndent()

class PythonPluginFetcher : IFetcher {
    companion object {
        private var nativeLibraryLoaded = false

        fun locateNativeLibrary() {
            if (!nativeLibraryLoaded) {
                nativeLibraryLoaded = true
                val process = ProcessBuilder()
                    .command("python3", "-")
                    .redirectInput(ProcessBuilder.Redirect.PIPE)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .start()

                with(process.outputStream) {
                    write(jepLocatorScript.toByteArray(Charsets.UTF_8))
                    close()
                }

                val ret = process.inputStream.bufferedReader().readLine()
                logger.info { "Located Jep C Library: $ret" }
                MainInterpreter.setJepLibraryPath(ret)
            }
        }

        fun fromFile(name: String, path: Path, cwd: Path, fetcherManager: FetcherManager): PythonPluginFetcher =
            fromSource(
                name,
                path.readText(),
                cwd,
                fetcherManager,
            )

        fun fromSource(name: String, source: String, cwd: Path, fetcherManager: FetcherManager): PythonPluginFetcher {
            val thread = Executors.newSingleThreadExecutor {
                Thread(it, "PythonPluginFetcher:$name")
            }.asCoroutineDispatcher()

            val interp = runBlocking(thread) {
                val interp = SharedInterpreter()
                val builtins = PyBuiltins.get(interp)
                interp.getValue("__import__('os')", PyObject::class.java)
                    .getAttr("chdir", PyCallable::class.java)
                    .call(cwd.toAbsolutePath().toString())
                interp.getValue("__import__('sys')", PyObject::class.java)
                    .getAttr("path", PyObject::class.java)
                    .getAttr("append", PyCallable::class.java)
                    .call(cwd.toAbsolutePath().toString())

                // Workaround used to make ruff and lsp not complain about
                // missing definitions
                interp.getValue("__import__('builtins')", PyObject::class.java)
                    .setAttr(
                        "snowballr",
                        builtins.dict(
                            mapOf(
                                "log" to PythonLogger(name),
                                "fetchers" to PythonFetcherManager(fetcherManager, interp, thread),
                            ),
                        ),
                    )

                interp.exec(source)
                interp
            }

            return PythonPluginFetcher(thread, interp)
        }
    }

    // Each Jep interpreter requires its own thread.
    private val thread: CoroutineDispatcher
    private val interp: Jep

    private constructor(thread: CoroutineDispatcher, interp: Jep) {
        this.thread = thread
        this.interp = interp
    }

    override suspend fun getAvailableOptions(): Set<String> = withContext(thread) {
        interp.getValue("availableOptions", PyObject::class.java).toSet<String>(interp)
    }

    override suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper> = withContext(
        thread,
    ) {
        interp.getValue(
            "searchPapers",
            PyCallable::class.java,
        ).callAs(PyObject::class.java, searchQuery, options.toPyObject(interp))
            .toSet<PyObject>(interp)
            .map { it.toPaper() }
            .toSet()
    }

    override suspend fun fetchForwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = withContext(
        thread,
    ) {
        interp.getValue(
            "fetchForwardReferences",
            PyCallable::class.java,
        ).callAs(PyObject::class.java, paper.toPyObject(interp), options.toPyObject(interp))
            .toSet<PyObject>(interp)
            .map { it.toPaper() }
            .toSet()
    }

    override suspend fun fetchBackwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = withContext(
        thread,
    ) {
        interp.getValue(
            "fetchBackwardReferences",
            PyCallable::class.java,
        ).callAs(PyObject::class.java, paper.toPyObject(interp), options.toPyObject(interp))
            .toSet<PyObject>(interp)
            .map { it.toPaper() }
            .toSet()
    }
}

private class PythonLogger {
    private val logger: KLogger

    constructor(name: String) {
        this.logger = KotlinLogging.logger(name)
    }

    fun trace(message: String) = logger.trace { message }
    fun debug(message: String) = logger.debug { message }
    fun info(message: String) = logger.info { message }
    fun warn(message: String) = logger.warn { message }
    fun error(message: String) = logger.error { message }
}

private class PythonFetcherManager(
    private val fetcherManager: FetcherManager,
    private val interp: Jep,
    private val thread: CoroutineDispatcher,
) {
    fun getAvailableFetchers(): PyObject = runBlocking {
        fetcherManager.getAvailableFetchers()
    }.toPyObject(interp)

    fun getAvailableOptions(fetcher: String): PyObject = runBlocking {
        fetcherManager.getAvailableOptions(fetcher)
    }.toPyObject(interp)

    fun searchPapers(fetcher: String, searchQuery: String, options: PyObject): PyObject = runBlocking {
        fetcherManager.searchPapers(fetcher, searchQuery, options.toOptionsMap())
    }.map { it.toPyObject(interp) }.toSet().toPyObject(interp)

    fun fetchForwardReferences(fetcher: String, paper: PyObject, options: PyObject): PyObject = runBlocking {
        fetcherManager.fetchForwardReferences(fetcher, paper.toPaper(), options.toOptionsMap())
    }.map { it.toPyObject(interp) }.toSet().toPyObject(interp)

    fun fetchBackwardReferences(fetcher: String, paper: PyObject, options: PyObject): PyObject = runBlocking {
        fetcherManager.fetchBackwardReferences(fetcher, paper.toPaper(), options.toOptionsMap())
    }.map { it.toPyObject(interp) }.toSet().toPyObject(interp)
}

fun Map<String, String>.toPyObject(interp: Jep): PyObject = PyBuiltins.get(interp).dict(this)

fun PyObject.toOptionsMap(): Map<String, String> = this.`as`(HashMap<String, String>()::class.java)

fun PyObject.toPaper(): Paper = Paper(
    UUID.randomUUID(),
    this.getAttr("title", String::class.java),
    this.getAttr("externalId", String::class.java),
    this.getAttr("abstract", String::class.java),
    this.getAttr("publishedAt", Integer::class.java)?.toLong()?.let { Instant.fromEpochSeconds(it) },
    this.getAttr("publisher", String::class.java),
    this.getAttr("publicationType", String::class.java),
    this.getAttr("publicationName", String::class.java),
    null,
    OffsetDateTime.now(),
    null,
    null,
)

fun <T> Set<T>.toPyObject(interp: Jep): PyObject = PyBuiltins.get(interp).set(this)

inline fun <reified T> PyObject.toSet(interp: Jep): Set<T> {
    val builtins = interp.getValue("__import__('builtins')", PyObject::class.java)
    val lenBuiltin = builtins.getAttr("len", PyCallable::class.java)
    val iterBuiltin = builtins.getAttr("iter", PyCallable::class.java)
    val nextBuiltin = builtins.getAttr("next", PyCallable::class.java)

    val len = lenBuiltin.call(this) as Long

    val map = HashSet<T>()
    val iterator = iterBuiltin.call(this) as PyObject

    repeat(len.toInt()) {
        map.add(nextBuiltin.callAs(T::class.java, iterator))
    }

    return map
}

fun Paper.toPyObject(interp: Jep): PyObject {
    val paper = interp.getValue("""Paper("", "")""", PyObject::class.java)

    paper.setAttr("title", this.title)
    paper.setAttr("abstract", this.abstract)
    paper.setAttr("externalId", this.externalId)
    paper.setAttr("publishedAt", this.publishedAt?.epochSeconds)
    paper.setAttr("publisher", this.publisher)
    paper.setAttr("publicationType", this.publicationType)
    paper.setAttr("publicationName", this.publicationName)

    return paper
}
