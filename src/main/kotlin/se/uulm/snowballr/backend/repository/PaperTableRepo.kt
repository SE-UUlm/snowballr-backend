package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.toPaper
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
     * @return whether the paper with the passed [id] exists.
     */
    suspend fun doesPaperExistById(id: UUID): Boolean
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

    override suspend fun getPaperById(id: UUID): Result<Paper> = db.query {
        getEntityByKeyAsResult(::getPaperByIdOrNull, EntityType.PAPER, id)
    }

    override suspend fun doesPaperExistById(id: UUID): Boolean = db.query {
        PaperTable.doesEntityExistById(id)
    }
}
