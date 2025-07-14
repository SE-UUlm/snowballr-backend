package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.KLogger
import jep.Jep
import jep.MainInterpreter
import jep.SharedInterpreter
import jep.python.PyBuiltins
import jep.python.PyObject
import jep.python.PyCallable
import kotlinx.datetime.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import se.uulm.snowballr.backend.model.dto.Paper
import java.time.OffsetDateTime
import java.util.HashMap
import java.util.UUID
import java.util.concurrent.Executors
import java.nio.file.Path
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

@Suppress("StringTemplateIndent", "Indentation")
private val pythonDataTypes = """
    from dataclasses import dataclass
    from datetime import datetime
    from typing import Optional

    # Unsafe hash needed to make it hashable whilst maintaining mutability.
    # Do not add an object of this class to a dict and then modify it!
    @dataclass(unsafe_hash=True)
    class Paper:
        title: str
        abstract: str
        externalId: Optional[str] = None
        publishedAt: Optional[int] = None
        publisher: Optional[str] = None
        publicationType: Optional[str] = None
        publicationName: Optional[str] = None
    """.trimIndent()

class PythonPluginFetcher : IFetcher {
    companion object {
        fun locateNativeLibrary() {
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

        fun newInterpreter(): Jep {
            val interp = SharedInterpreter()
            interp.exec(pythonDataTypes)
            return interp
        }

        fun withNewInterpreter(block: (Jep) -> Unit) = newInterpreter().use(block)

        fun fromFile(name: String, path: Path): PythonPluginFetcher = fromSource(name, path.readText())

        fun fromSource(name: String, source: String): PythonPluginFetcher {
            val thread = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
            val interp = runBlocking(thread) {
                val interp = newInterpreter()
                interp.set("log", PythonLogger("PythonFetcherPlugin '$name'"))
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

    override suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper> = withContext(thread) {
        (interp.invoke("searchPapers", searchQuery, options.toPyObject(interp)) as PyObject)
            .toSet<PyObject>(interp)
            .map { it.toPaper() }
            .toSet()
    }

    override suspend fun fetchForwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = withContext(thread) {
        (interp.invoke("fetchForwardReferences", paper.toPyObject(interp), options.toPyObject(interp)) as PyObject)
            .toSet<PyObject>(interp)
            .map { it.toPaper() }
            .toSet()
    }

    override suspend fun fetchBackwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = withContext(thread) {
       (interp.invoke("fetchBackwardReferences", paper.toPyObject(interp), options.toPyObject(interp)) as PyObject)
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

inline fun<reified T> PyObject.toSet(interp: Jep): Set<T> {
    val builtins = interp.getValue("__import__('builtins')", PyObject::class.java)
    val lenBuiltin = builtins.getAttr("len", PyCallable::class.java)
    val iterBuiltin = builtins.getAttr("iter", PyCallable::class.java)
    val nextBuiltin = builtins.getAttr("next", PyCallable::class.java)

    val len = lenBuiltin.call(this) as Long

    val map = HashSet<T>()
    val iterator = iterBuiltin.call(this) as PyObject

    for (i in 1..len) {
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
