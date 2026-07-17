package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.or
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import org.jetbrains.exposed.v1.jdbc.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.PaperHasExternalIdTable
import se.uulm.snowballr.backend.table.association.toExternalIdPair
import se.uulm.snowballr.backend.table.toPaper
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [PaperTable].
 *
 * This interface provides abstraction for handling persistence and retrieval operations for papers. By using this
 * interface, the functionality for managing papers can remain decoupled from the specifics of the database layer.
 */
interface IPaperTableRepo {
    /**
     * Returns a [Result] containing the paper by its ID or a [NotFoundException] if the paper with the passed [id]
     * doesn't exist.
     */
    suspend fun getPaperById(id: UUID): Result<Paper>

    /**
     * Ensures that the paper exists with the passed [id].
     *
     * Throws a [PaperNotFoundException] if the paper is missing; otherwise does nothing.
     */
    suspend fun ensurePaperExists(id: UUID)

    /**
     * Checks whether a paper with the passed [externalIds] exists.
     */
    suspend fun doesPaperExistByExternalIds(externalIds: List<ExternalId>): Boolean

    /**
     * Creates a new paper in the database with the provided values.
     */
    suspend fun createPaper(request: CreatePaperRequest): Paper

    /**
     * Updates an existing paper in the database with the provided new values.
     */
    suspend fun updatePaper(request: UpdatePaperRequest, paths: List<String>): Paper

    /**
     * Retrieves a list of papers whose title partially or fully match the [query].
     *
     * The number of returned papers is limited to a maximum of 20. Furthermore, the matching papers are sorted by their
     * similarity to the search query.
     *
     * @param query The query used to match the paper titles.
     * @return A list of up to 20 matching papers.
     */
    suspend fun getPapersBySearchQuery(query: String): List<Paper>

    /**
     * Retrieves all papers that have one or more external ID(s) that matches at least one external ID in [externalIds].
     */
    suspend fun getPapersByExternalIds(externalIds: List<ExternalId>): List<Paper>
}

/**
 * Repository implementation for managing the [PaperTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for papers by leveraging the
 * database abstraction defined in [IDatabase]. It facilitates CRUD operations on freestanding papers and ensures
 * database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class PaperTableRepo(
    private val db: IDatabase,
) : IPaperTableRepo {
    companion object {
        private const val MAXIMUM_NUMBER_OF_PAPER_CANDIDATES = 20
        private const val MINIMUM_SIMILARITY_SCORE = 0.2
    }

    private fun getPapersWhere(where: () -> Op<Boolean>) =
        PaperTable
            .joinPaperHasExternalId()
            .selectAll()
            .where(where)
            .groupBy { it[PaperTable.id].value }
            .values
            .map { rows -> rows.toPaperWithExternalIds() }

    private fun getPaperByIdOrNull(id: UUID): Paper? = getPapersWhere(where = { PaperTable.id eq id }).singleOrNull()

    /**
     * Creates a where clause to find a paper that has any of the passed [externalIds].
     *
     * Translates to:
     * ```
     *    (type == item[0].type and value == item[0].value)
     * or (type == item[1].type and value == item[1].value)
     * ...
     * or (type == item[n-1].type and value == item[n-1].value)
     * ```
     */
    private fun externalIdsWhereOp(externalIds: List<ExternalId>) = externalIds.map {
        (PaperHasExternalIdTable.type eq it.type) and (PaperHasExternalIdTable.value eq it.value)
    }.fold<Op<Boolean>, Op<Boolean>>(Op.FALSE) { acc, value -> acc or value }

    override suspend fun getPaperById(id: UUID): Result<Paper> = db.query {
        getEntityByKeyAsResult(::getPaperByIdOrNull, EntityType.PAPER, id)
    }

    override suspend fun ensurePaperExists(id: UUID) = db.query {
        if (!PaperTable.doesEntityExistById(id)) {
            throw PaperNotFoundException(id)
        }
    }

    override suspend fun doesPaperExistByExternalIds(externalIds: List<ExternalId>): Boolean = db.query {
        PaperHasExternalIdTable.doesEntityExist { externalIdsWhereOp(externalIds) }
    }

    override suspend fun createPaper(request: CreatePaperRequest): Paper = db.query {
        val paperId = PaperTable.insertAndGetId {
            it[title] = request.title
            it[abstract] = request.abstract
            it[year] = request.year
            it[publisher] = request.publisher
            it[publicationName] = request.publicationName
            it[publicationType] = request.publicationType
            it[authors] = request.authors
            it[fetcherMetadata] = request.fetcherMetadata
            it[createdAt] = OffsetDateTime.now()
        }.value

        insertExternalIds(paperId, request.externalIds)

        getPaperById(paperId).getOrThrow()
    }

    override suspend fun updatePaper(request: UpdatePaperRequest, paths: List<String>): Paper = db.query {
        if (paths.isEmpty()) {
            return@query getPaperById(request.paperId).getOrThrow()
        }

        PaperTable.update({ PaperTable.id eq request.paperId }) {
            for (field in paths) {
                when (field) {
                    "paper.title" -> it[title] = request.title
                    "paper.abstrakt" -> it[abstract] = request.abstract
                    "paper.year" -> it[year] = request.year
                    "paper.publisher" -> it[publisher] = request.publisher
                    "paper.publication_name" -> it[publicationName] = request.publicationName
                    "paper.publication_type" -> it[publicationType] = request.publicationType
                    "paper.authors" -> it[authors] = request.authors
                }
            }

            it[modifiedAt] = OffsetDateTime.now()
        }

        if (paths.contains("paper.external_ids")) {
            PaperHasExternalIdTable.deleteWhere { PaperHasExternalIdTable.paperId eq request.paperId }
            insertExternalIds(request.paperId, request.externalIds)
        }

        getPaperById(request.paperId).getOrThrow()
    }

    override suspend fun getPapersBySearchQuery(query: String): List<Paper> = db.query {
        val paperTable = "\"${PaperTable.tableName}\""
        val titleCol = "$paperTable.${PaperTable.title.name}"

        val rawSqlQuery =
            """
            WITH papers_with_similarity_scores AS (
                SELECT *, similarity($titleCol, ?) AS sim_title
                FROM $paperTable
            )
            SELECT *
            FROM papers_with_similarity_scores
            WHERE sim_title > $MINIMUM_SIMILARITY_SCORE
            ORDER BY sim_title DESC
            LIMIT $MAXIMUM_NUMBER_OF_PAPER_CANDIDATES
            """.trimIndent()

        val matchingPapers = exec(
            stmt = rawSqlQuery,
            args = listOf(TextColumnType() to query),
            explicitStatementType = StatementType.SELECT,
            transform = { extractPaperRows(JdbcResult(it)) },
        ).orEmpty()

        val paperIds = matchingPapers.map { it.id }
        val paperExternalIds = PaperHasExternalIdTable.selectAll()
            .where { PaperHasExternalIdTable.paperId inList paperIds }
            .map(ResultRow::toExternalIdPair)
            .groupBy { it.first } // group by paper ID
            .mapValues { entry -> entry.value.map { it.second } }

        matchingPapers.map { it.copy(externalIds = paperExternalIds[it.id].orEmpty()) }
    }

    override suspend fun getPapersByExternalIds(externalIds: List<ExternalId>): List<Paper> = db.query {
        if (externalIds.isEmpty()) return@query emptyList()

        getPapersWhere(where = { externalIdsWhereOp(externalIds) })
    }

    /**
     * Extracts and converts rows from a [JdbcResult] to a list of [Paper] objects.
     *
     * @param result The [JdbcResult] containing paper data.
     * @return A list of [Paper] objects extracted from the result set.
     */
    private fun extractPaperRows(result: JdbcResult): List<Paper> =
        extractTableRows(result, PaperTable) { it.toPaper(emptyList()) }

    private fun insertExternalIds(paperId: UUID, externalIds: List<ExternalId>) {
        PaperHasExternalIdTable.batchInsert(externalIds) {
            this[PaperHasExternalIdTable.paperId] = paperId
            this[PaperHasExternalIdTable.type] = it.type
            this[PaperHasExternalIdTable.value] = it.value
        }
    }
}
