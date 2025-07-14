package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import jep.Jep
import jep.MainInterpreter
import jep.SharedInterpreter
import jep.python.PyBuiltins
import jep.python.PyObject
import kotlinx.datetime.Instant
import se.uulm.snowballr.backend.model.dto.Paper
import java.time.OffsetDateTime
import java.util.HashMap
import java.util.UUID
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

    @dataclass
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

        fun fromFile(path: Path): PythonPluginFetcher = fromSource(path.readText())

        fun fromSource(source: String): PythonPluginFetcher {
            val interp = newInterpreter()
            interp.exec(source)
            return PythonPluginFetcher(interp)
        }
    }

    private val interp: Jep

    private constructor(interp: Jep) {
        this.interp = interp
    }

    override suspend fun getAvailableOptions(): Set<String> = setOf()

    override suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper> = setOf()

    override suspend fun fetchForwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = setOf()

    override suspend fun fetchBackwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> = setOf()
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
