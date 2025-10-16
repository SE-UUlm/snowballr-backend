package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.association.AuthorOfPaperTable
import se.uulm.snowballr.backend.table.toAuthor
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [AuthorOfPaperTable].
 *
 * This interface provides abstraction for handling persistence and retrieval operations for authors of papers. By using
 * this interface, the functionality for managing author-paper associations can remain decoupled from the specifics of
 * the database layer.
 */
interface IAuthorOfPaperTableRepo {
    /**
     * Returns all authors of a [Paper].
     *
     * @param paperId The ID of the paper for which the authors should be retrieved.
     */
    suspend fun getAuthorsOfPaperById(paperId: UUID): List<Author>

    /**
     * Adds an author to a paper.
     *
     * @param authorId The ID of the author to be added.
     * @param paperId The ID of the paper to which the author should be added.
     */
    suspend fun addAuthorToPaper(authorId: UUID, paperId: UUID)

    /**
     * Removes an author from a paper.
     *
     * @param authorId The ID of the author to be removed.
     * @param paperId The ID of the paper from which the author should be removed.
     */
    suspend fun removeAuthorFromPaper(authorId: UUID, paperId: UUID)
}

/**
 * Repository implementation for managing the [AuthorOfPaperTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for authors of papers by leveraging
 * the database abstraction defined in [IDatabase]. It facilitates CRUD operations on author-paper associations and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class AuthorOfPaperTableRepo(
    private val db: IDatabase,
) : IAuthorOfPaperTableRepo {
    override suspend fun getAuthorsOfPaperById(paperId: UUID): List<Author> = db.query {
        (AuthorTable innerJoin AuthorOfPaperTable)
            .selectAll()
            .where { AuthorOfPaperTable.paperId eq paperId }
            .map { it.toAuthor() }
            .toList()
    }

    override suspend fun addAuthorToPaper(authorId: UUID, paperId: UUID) = db.query {
        AuthorOfPaperTable.insert {
            it[AuthorOfPaperTable.authorId] = authorId
            it[AuthorOfPaperTable.paperId] = paperId
        }
        Unit
    }

    override suspend fun removeAuthorFromPaper(authorId: UUID, paperId: UUID) = db.query {
        AuthorOfPaperTable.deleteWhere {
            (AuthorOfPaperTable.paperId eq paperId) and (AuthorOfPaperTable.authorId eq authorId)
        }
        Unit
    }
}
