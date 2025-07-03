package se.uulm.snowballr.backend.service

import kotlinx.coroutines.runBlocking
import se.uulm.snowballr.backend.model.dto.Session
import se.uulm.snowballr.backend.repository.ISessionTableRepo
import java.util.UUID

interface ISessionService {
    /**
     * Service method to create a new session for a user.
     *
     * @param userId The ID of the user for whom the session is created.
     * @return The newly created session associated with the user.
     */
    suspend fun createSession(userId: UUID): Session

    /**
     * Service method to retrieve a session by its ID.
     *
     * @param id The ID of the session to retrieve.
     * @return The session associated with the given ID.
     */
    suspend fun getSessionById(id: UUID): Session

    /**
     * Service method to revoke a session by its ID.
     *
     * @param id The ID of the session to revoke.
     */
    suspend fun revokeSession(id: UUID)

    /**
     * Service method to check if a session is revoked.
     *
     * @param id The ID of the session to check.
     * @return True if the session is revoked, false otherwise.
     */
    fun isSessionRevoked(id: UUID): Boolean
}

/**
 * The [SessionService] class handles operations related to user sessions by implementing the [ISessionService]
 * interface.
 *
 * This class serves as a layer that abstracts the responsibility of session management,
 * delegating the actual persistence operations to the [ISessionTableRepo] repository.
 *
 * @constructor Initializes the [SessionService] with a session repository.
 * @param repo The repository responsible for managing persistence operations for sessions.
 */
class SessionService(
    private val repo: ISessionTableRepo,
) : ISessionService {
    override suspend fun createSession(userId: UUID): Session = repo.createSession(userId)

    override suspend fun getSessionById(id: UUID): Session = repo.getSessionById(id)

    override suspend fun revokeSession(id: UUID) {
        repo.revokeSessionById(id)
    }

    override fun isSessionRevoked(id: UUID): Boolean = runBlocking { getSessionById(id).revoked }
}
