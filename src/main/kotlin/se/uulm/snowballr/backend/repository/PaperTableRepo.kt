package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.toAuthor
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.PaperNotFoundException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.toPaper
import java.time.OffsetDateTime
import java.util.UUID
import snowballr.PaperOuterClass.Author as GrpcAuthor
import snowballr.PaperOuterClass.Paper as GrpcPaper

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
     * Returns a [Result] containing the paper by its external ID or a [NotFoundException] if the paper with the
     * passed [externalId] doesn't exist.
     */
    suspend fun getPaperByExternalId(externalId: String): Result<Paper>

    /**
     * Ensures that the paper exists with the passed [id].
     *
     * Throws a [PaperNotFoundException] if the paper is missing; otherwise does nothing.
     */
    suspend fun ensurePaperExists(id: UUID)

    /**
     * Checks whether a paper with the passed [externalId] exists.
     */
    suspend fun doesPaperExistByExternalId(externalId: String): Boolean

    /**
     * Creates a new paper in the database with the provided values.
     */
    suspend fun createPaper(request: GrpcPaper): Paper

    /**
     * Updates an existing paper in the database with the provided new values.
     * The following fields can be updated:
     * - [GrpcPaper.externalId_]
     * - [GrpcPaper.title_]
     * - [GrpcPaper.abstrakt_]
     * - [GrpcPaper.year_]
     * - [GrpcPaper.publisher_]
     * - [GrpcPaper.publicationName_]
     * - [GrpcPaper.publicationType_]
     * - [GrpcPaper.authors_]
     */
    suspend fun updatePaper(request: GrpcPaper.Update): Paper
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
    private fun getPaperByIdOrNull(id: UUID): Paper? = PaperTable.getEntityByIdOrNull(id, ResultRow::toPaper)

    private fun getPaperByExternalIdOrNull(externalId: String): Paper? =
        PaperTable.getEntityOrNull(ResultRow::toPaper) { PaperTable.externalId eq externalId }

    override suspend fun getPaperById(id: UUID): Result<Paper> = db.query {
        getEntityByKeyAsResult(::getPaperByIdOrNull, EntityType.PAPER, id)
    }

    override suspend fun getPaperByExternalId(externalId: String): Result<Paper> = db.query {
        getEntityByKeyAsResult(::getPaperByExternalIdOrNull, EntityType.PAPER, externalId)
    }

    override suspend fun ensurePaperExists(id: UUID) = db.query {
        if (!PaperTable.doesEntityExistById(id)) {
            throw PaperNotFoundException(id)
        }
    }

    override suspend fun doesPaperExistByExternalId(externalId: String): Boolean = db.query {
        PaperTable.doesEntityExist { PaperTable.externalId eq externalId }
    }

    override suspend fun createPaper(request: GrpcPaper): Paper = db.query {
        PaperTable.insertAndGet(ResultRow::toPaper) {
            it[title] = request.title
            it[externalId] = request.externalId.ifBlank { null }
            it[abstract] = request.abstrakt
            it[year] = request.year
            it[publisher] = request.publisher
            it[publicationName] = request.publicationName
            it[publicationType] = request.publicationType
            it[authors] = request.authorsList.map(GrpcAuthor::toAuthor)
            it[fetcherMetadata] = request.fetcherMetadataMap
            it[createdAt] = OffsetDateTime.now()
        }
    }

    override suspend fun updatePaper(request: GrpcPaper.Update): Paper = db.query {
        val paperId = parseUUID(request.paper.id, EntityType.PAPER)
        val fieldMask = FieldMaskUtil.normalize(request.mask)

        if (fieldMask.pathsList.isEmpty()) {
            return@query getPaperById(paperId).getOrThrow()
        }

        PaperTable.updateByIdAndGet(paperId, ResultRow::toPaper) {
            for (field in fieldMask.pathsList) {
                when (field) {
                    "paper.external_id" -> it[externalId] = request.paper.externalId
                    "paper.title" -> it[title] = request.paper.title
                    "paper.abstrakt" -> it[abstract] = request.paper.abstrakt
                    "paper.year" -> it[year] = request.paper.year
                    "paper.publisher" -> it[publisher] = request.paper.publisher
                    "paper.publication_name" -> it[publicationName] = request.paper.publicationName
                    "paper.publication_type" -> it[publicationType] = request.paper.publicationType
                    "paper.authors" -> it[authors] = request.paper.authorsList.map(GrpcAuthor::toAuthor)
                }
            }

            it[modifiedAt] = OffsetDateTime.now()
        }
    }
}
