package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotPersistedException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Session
import se.uulm.snowballr.backend.table.SessionTable
import se.uulm.snowballr.backend.table.getUserEntityId
import se.uulm.snowballr.backend.table.toSession
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [SessionTable].
 *
 * This interface is used to handle persistence and retrieval operations for user sessions
 * by providing abstraction over the underlying database implementation. By using this interface,
 * the logic for creating and managing sessions can remain decoupled from the specifics of the database layer.
 */
interface ISessionTableRepo {
    /**
     * Creates a new session for the user identified by [userId].
     *
     * @param userId The ID of the user for whom the session is being created.
     * @return The newly created [Session] object.
     * @throws EntityNotPersistedException with [EntityType.SESSION] if the session could not be persisted.
     */
    suspend fun createSession(userId: UUID): Session

    /**
     * Retrieves a session by its unique identifier.
     *
     * @param id The unique identifier of the session to retrieve.
     * @return The [Session] object corresponding to the provided ID.
     * @throws NotFoundException with [EntityType.SESSION] if no session with the given ID exists.
     */
    suspend fun getSessionById(id: UUID): Session

    /**
     * Revokes a session identified by its unique identifier.
     *
     * @param id The unique identifier of the session to revoke.
     * @throws NotFoundException with [EntityType.SESSION] if no session with the given ID exists.
     */
    suspend fun revokeSessionById(id: UUID)
}

/**
 * Repository implementation for managing the [SessionTable] in the database.
 *
 * This class handles the persistence and retrieval of session data by integrating
 * with the underlying database through the [IDatabase] interface. It provides
 * concrete methods for CRUD operations on session records within the database.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class SessionTableRepo(
    private val db: IDatabase,
) : ISessionTableRepo {
    override suspend fun createSession(userId: UUID): Session = db.dbQuery {
        val userId = getUserEntityId(userId)

        SessionTable.insertAndGet(ResultRow::toSession, EntityType.SESSION) {
            it[SessionTable.userId] = userId
        }
    }

    override suspend fun getSessionById(id: UUID): Session = db.dbQuery {
        SessionTable
            .selectAll()
            .where { SessionTable.id eq id }
            .map { it.toSession() }
            .singleOrNull()
            ?: throw NotFoundException(EntityType.SESSION, id.toString())
    }

    override suspend fun revokeSessionById(id: UUID) = db.dbQuery {
        val updatedRows =
            SessionTable
                .update({ SessionTable.id eq id }) {
                    it[SessionTable.revoked] = true
                }

        if (updatedRows == 0) {
            throw NotFoundException(EntityType.SESSION, id.toString())
        }
    }
}
