package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.repository.doesEntityExist
import se.uulm.snowballr.backend.repository.joinPaperHasExternalId
import se.uulm.snowballr.backend.repository.toPaperWithExternalIds
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.association.ReadingListTable
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ReadingListTable].
 *
 * This interface provides an abstraction for handling persistence and retrieval operations for reading list entries.
 * By using this interface, the functionality for managing reading lists remains decoupled from the specifics of the
 * database layer, promoting flexibility and maintainability in the codebase.
 */
interface IReadingListTableRepo {
    /**
     * Add the provided paper to the reading list of the provided user.
     */
    suspend fun createReadingListEntry(userId: UUID, paperId: UUID)

    /**
     * Remove the provided paper from the reading list of the provided user.
     */
    suspend fun removeReadingListEntry(userId: UUID, paperId: UUID)

    /**
     * @return Whether the provided paper is on the reading list of the provided user.
     */
    suspend fun isPaperOnReadingList(userId: UUID, paperId: UUID): Boolean

    /**
     * @return The [List] of all [Paper]s the provided user has on their reading list.
     */
    suspend fun getAllReadingListEntries(userId: UUID): List<Paper>
}

/**
 * Repository implementation for managing the [ReadingListTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for reading list data by leveraging
 * the database abstraction defined in [IDatabase]. It facilitates CRUD operations on reading list entries and ensures
 * database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ReadingListTableRepo(
    private val db: IDatabase,
) : IReadingListTableRepo {
    override suspend fun createReadingListEntry(userId: UUID, paperId: UUID) = db.query {
        if (isPaperOnReadingList(userId, paperId)) {
            // Entry already exists, so we do nothing to avoid duplicates.
            return@query
        }

        ReadingListTable.insert {
            it[this.userId] = userId
            it[this.paperId] = paperId
        }
        Unit
    }

    override suspend fun removeReadingListEntry(userId: UUID, paperId: UUID) = db.query {
        ReadingListTable.deleteWhere {
            (this.userId eq userId) and (this.paperId eq paperId)
        }
        Unit
    }

    override suspend fun isPaperOnReadingList(userId: UUID, paperId: UUID): Boolean = db.query {
        ReadingListTable.doesEntityExist {
            (ReadingListTable.userId eq userId) and (ReadingListTable.paperId eq paperId)
        }
    }

    override suspend fun getAllReadingListEntries(userId: UUID): List<Paper> = db.query {
        PaperTable
            .join(
                ReadingListTable,
                JoinType.INNER,
                onColumn = PaperTable.id,
                otherColumn = ReadingListTable.paperId,
            )
            .joinPaperHasExternalId()
            .selectAll()
            .where { ReadingListTable.userId eq userId }
            .groupBy { it[PaperTable.id].value }
            .values
            .map { it.toPaperWithExternalIds() }
    }
}
