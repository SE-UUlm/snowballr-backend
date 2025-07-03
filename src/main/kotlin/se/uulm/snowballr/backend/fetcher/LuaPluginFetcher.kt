package se.uulm.snowballr.backend.fetcher

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.datetime.Instant
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.jse.JsePlatform
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.Paper
import java.nio.file.Path
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger {}

@Suppress("StringLiteralDuplication")
class LuaPluginFetcher : IFetcher {
    private val globals: Globals

    private constructor(globals: Globals) {
        this.globals = globals
        ensureApiContract()
    }

    companion object {
        private val VALID_KEYS: Set<String> = setOf(
            "name",
            "title",
            "externalId",
            "abstract",
            "publishedAt",
            "publisher",
            "publicationType",
            "publicationName",
        )

        private fun newGlobals(): Globals = JsePlatform.standardGlobals()

        fun fromFile(path: Path): LuaPluginFetcher {
            val globals = newGlobals()
            val chunk = globals.loadfile(path.toString())
            chunk.call()
            return LuaPluginFetcher(globals)
        }

        fun fromSource(source: String): LuaPluginFetcher {
            val globals = newGlobals()
            val chunk = globals.load(source)
            chunk.call()
            return LuaPluginFetcher(globals)
        }
    }

    private fun ensureApiContract() {
        globals.get("availableOptions").checktable()
        globals.get("searchPapers").checkfunction()
        globals.get("fetchReferences").checkfunction()
        globals.get("fetchCitations").checkfunction()
    }

    override suspend fun getAvailableOptions(): Set<String> {
        val value = globals.get("availableOptions")

        if (!value.istable()) {
            throw SnowballRException.FetcherException.LuaInternal("Lua fetcher specified invalid 'availableOptions'")
        }

        val table = value as LuaTable
        return table
            .keys()
            .map { table.get(it) }
            .map { it.tojstring() }
            .toSet()
    }

    override suspend fun searchPapers(searchQuery: String, options: Map<String, String>): Set<Paper> {
        val value = globals
            .get("searchPapers")
            .call(LuaValue.valueOf(searchQuery), options.toLuaTable())
        return value.toPaperSet()
    }

    override suspend fun fetchReferences(paper: Paper, options: Map<String, String>): Set<Paper> {
        val value = globals
            .get("fetchReferences")
            .call(paper.toLuaTable(), options.toLuaTable())
        return value.toPaperSet()
    }

    override suspend fun fetchCitations(paper: Paper, options: Map<String, String>): Set<Paper> {
        val value = globals
            .get("fetchCitations")
            .call(paper.toLuaTable(), options.toLuaTable())
        return value.toPaperSet()
    }

    fun LuaValue.toPaper(): Paper {
        if (!this.istable()) {
            throw SnowballRException.FetcherException.LuaInternal("Lua fetcher return value was not a table")
        }

        val table = this as LuaTable

        if (table.keys().any {
                logger.info { "Key: $it" }
                !LuaPluginFetcher.VALID_KEYS.contains(it.toString())
            }
        ) {
            throw SnowballRException.FetcherException.LuaInternal("Lua fetcher return table contains unexpected key")
        }

        // Sets some keys to `null` as these ideally shouldn't be settable by a fetcher
        return Paper(
            id = UUID.randomUUID(),
            title = table.get("title").tojstring(),
            externalId = table.get("externalId").tojstring(),
            abstract = table.get("abstract").tojstring(),
            publishedAt = Instant.fromEpochSeconds(table.get("publishedAt").tolong()),
            publisher = table.get("publisher").tojstring(),
            publicationType = table.get("publicationType").tojstring(),
            publicationName = table.get("publicationName").tojstring(),
            pdfId = null,
            createdAt = OffsetDateTime.now(),
            modifiedAt = null,
            modifiedBy = null,
        )
    }

    fun LuaValue.toPaperSet(): Set<Paper> {
        if (!this.istable()) {
            throw SnowballRException.FetcherException.LuaInternal("Lua fetcher return value was not an array")
        }
        val table = this as LuaTable
        val papers = table
            .keys()
            .map { table.get(it) }
            .map { it.toPaper() }
            .toSet()
        return papers
    }

    fun Map<String, String>.toLuaTable(): LuaValue = LuaValue.tableOf(
        this.entries
            .flatMap { listOf(it.key, it.value) }
            .map { LuaValue.valueOf(it) }.toTypedArray(),
    )

    fun Paper.toLuaTable(): LuaValue {
        val entries = ArrayList<String>()

        fun <T> addEntry(key: String, value: T?) {
            if (value != null) {
                entries.add(key)
                entries.add(value.toString())
            }
        }

        addEntry("id", this.id)
        addEntry("title", this.title)
        addEntry("externalId", this.externalId)
        addEntry("abstract", this.abstract)
        addEntry("publishedAt", this.publishedAt?.epochSeconds)
        addEntry("publisher", this.publisher)
        addEntry("publicationType", this.publicationType)
        addEntry("publicationName", this.publicationName)
        addEntry("pdfId", this.pdfId)
        addEntry("createdAt", this.createdAt.toEpochSecond())
        addEntry("modifiedAt", this.modifiedAt?.toEpochSecond())
        addEntry("modifiedBy", this.modifiedBy)

        return LuaValue.tableOf(entries.map { LuaValue.valueOf(it) }.toTypedArray())
    }
}
