package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.toAuthor
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [AuthorTable].
 *
 * This interface facilitates persistence and retrieval operations for authors, providing
 * an abstraction over the underlying database implementation. By utilizing this interface,
 * the logic for creating and managing authors remains decoupled from the specifics of the database layer.
 */
interface IAuthorTableRepo {
    /**
     * Returns a [Result] containing the author by its ID or a [NotFoundException] if the author with the passed [id]
     * doesn't exist.
     */
    suspend fun getAuthorById(id: UUID): Result<Author>
}

/**
 * Repository implementation for managing the [AuthorTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for authors by leveraging the
 * database abstraction defined in [IDatabase]. It facilitates CRUD operations on authors and ensures database
 * transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class AuthorTableRepo(
    private val db: IDatabase,
) : IAuthorTableRepo {
    /**
     * Requesting an author from the database.
     *
     * @param id The ID of the requested author.
     * @return The [Author] object or null, if no author with the given [id] was found.
     */
    private fun getAuthorByIdOrNull(id: UUID): Author? = AuthorTable.getEntityByIdOrNull(id, ResultRow::toAuthor)

    override suspend fun getAuthorById(id: UUID): Result<Author> = db.query {
        getEntityByIdAsResult(::getAuthorByIdOrNull, EntityType.AUTHOR, id)
    }
}
