package se.uulm.snowballr.backend.fetcher

import jep.SharedInterpreter
import jep.MainInterpreter
import se.uulm.snowballr.backend.model.dto.Paper
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

class PythonPluginFetcher : IFetcher {
    companion object {
        fun locateNativeLibrary() {
            val locatorScript = """
                import site
                import os
                import glob
                for f in glob.glob(os.path.join(site.getsitepackages()[0], "jep/libjep.*")):
                    print(f)
            """.trimIndent()
            val process = ProcessBuilder()
                .command("python3", "-")
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .start()

            with (process.outputStream) {
                write(locatorScript.toByteArray(Charsets.UTF_8))
                close()
            }

            val ret = process.inputStream.bufferedReader().readLine()
            logger.info { "Located Jep C Library: ${ret}" }
            MainInterpreter.setJepLibraryPath(ret)
        }
    }

    override suspend fun getAvailableOptions(): Set<String> {
        return setOf()
    }

    override suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper> {
        return setOf()
    }

    override suspend fun fetchForwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> {
        return setOf()
    }

    override suspend fun fetchBackwardReferences(paper: Paper, options: Map<String, String>): Set<Paper> {
        return setOf()
    }
}
