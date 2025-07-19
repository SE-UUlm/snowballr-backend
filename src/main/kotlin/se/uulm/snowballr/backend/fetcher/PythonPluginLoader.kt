package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.StandardWatchEventKinds.ENTRY_CREATE
import java.nio.file.StandardWatchEventKinds.ENTRY_DELETE
import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.WatchEvent
import java.nio.file.WatchKey
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText

const val WATCHER_REFRESH_INTERVAL_MS: Long = 1000

private val logger = KotlinLogging.logger {}

@DelicateCoroutinesApi
class PythonPluginLoader(
    private val pluginDirectory: Path,
    private val fetcherManager: FetcherManager,
) {
    private val fetchersDirectory = pluginDirectory.resolve("fetchers")

    init {
        PythonPluginFetcher.locateNativeLibrary()
        ensureDirectoryExists(fetchersDirectory.resolve("lib"))
        writeModules()
        loadAllPlugins()
        GlobalScope.launch { watchDirectory() }
    }

    private fun writeModules() {
        fetchersDirectory.resolve("lib/snowballr.py").writeText(Resources.pythonSnowballrTypes)
    }

    private fun loadAllPlugins() {
        val pluginFiles = fetchersDirectory
            .listDirectoryEntries()
            .filter { it.extension == "py" }
            .toSet()

        logger.info { "Trying to load ${pluginFiles.size} python fetcher plugins" }

        var successful = 0

        for (path in pluginFiles) {
            val name = path.nameWithoutExtension
            try {
                fetcherManager.registerFetcher(name, path.toAbsolutePath().toPythonFetcher())
                successful++
            } catch (e: FetcherManager.AlreadyRegisteredException) {
                logger.atError {
                    message = "A python fetcher plugin could not be loaded: ${path.name}"
                    cause = e
                }
            }
        }

        logger.info { "Successfully loaded $successful python fetcher plugins" }
    }

    private suspend fun watchDirectory() {
        val watcher = FileSystems.getDefault().newWatchService()
        val pathHandle = fetchersDirectory.toAbsolutePath().register(
            watcher,
            ENTRY_CREATE,
            ENTRY_DELETE,
            ENTRY_MODIFY,
        )

        while (true) {
            var watchHandle: WatchKey?

            do {
                watchHandle = watcher.poll()
                delay(WATCHER_REFRESH_INTERVAL_MS)
            } while (watchHandle == null)

            watchHandle.pollEvents().forEach { handleEvent(it) }

            if (!watchHandle.reset()) {
                watchHandle.cancel()
                watcher.close()
                break
            }
        }

        pathHandle.cancel()
    }

    private suspend fun handleEvent(event: WatchEvent<*>) {
        val filename = Path.of(event.context().toString())
        val basename = filename.nameWithoutExtension

        if (filename.extension != "py") return

        when (event.kind()) {
            ENTRY_CREATE -> {
                logger.info { "Witnessed file creation of '$filename'." }
                try {
                    fetcherManager.registerFetcher(basename, filename.toPythonFetcher())
                } catch (e: FetcherManager.AlreadyRegisteredException) {
                    logger.error(e) {}
                }
            }
            ENTRY_DELETE -> {
                logger.info { "Witnessed file deletion of '$filename'." }
                fetcherManager.removeFetcher(basename)
            }
            ENTRY_MODIFY -> {
                logger.info { "Witnessed file modification of '$filename'." }
                fetcherManager.removeFetcher(basename)
                try {
                    fetcherManager.registerFetcher(basename, filename.toPythonFetcher())
                } catch (e: FetcherManager.AlreadyRegisteredException) {
                    logger.error(e) {}
                }
            }
        }
    }

    private fun ensureDirectoryExists(path: Path): Boolean {
        if (path.isDirectory()) return true

        logger.warn { "Directory '$path' could not be found" }

        try {
            path.createDirectories()
            logger.info { "Created directory '$path'" }
            return true
        } catch (e: IOException) {
            logger.error(e) { "Could not create directory '$path'" }
            return false
        }
    }

    private fun Path.toPythonFetcher(): PythonPluginFetcher = PythonPluginFetcher.fromFile(
        this.nameWithoutExtension,
        this,
        fetchersDirectory,
        fetcherManager,
    )

    sealed class Resources {
        companion object {
            private fun fetcherResource(fileName: String) = this::class.java
                .getResourceAsStream("/fetchers/$fileName")
                .bufferedReader()
                .readText()

            val pythonSnowballrTypes = fetcherResource("lib/PythonSnowballrTypes.py")
        }
    }
}
