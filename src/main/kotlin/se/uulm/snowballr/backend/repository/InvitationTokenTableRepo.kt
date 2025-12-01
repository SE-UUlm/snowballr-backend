package se.uulm.snowballr.backend.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.dto.InvitationToken
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.toInvitationToken
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }

interface IInvitationTokenTableRepo {
    /**
     * Saves an invitation token for the given email.
     *
     * @param email The email of the user associated with the invitation token.
     * @param projectId The ID of the project associated with the invitation token.
     * @param token The invitation token.
     */
    suspend fun saveInvitationToken(email: String, projectId: UUID, token: String)

    /**
     * Retrieves an invitation token by its value.
     *
     * @param token The invitation token to retrieve.
     * @return A [Result] containing the [InvitationToken] associated with the given token value or a
     * [InvitationTokenNotFoundException] if the token doesn't exist.
     */
    suspend fun getInvitationTokenByValue(token: String): Result<InvitationToken>

    /**
     * Retrieves an invitation token by its email and project ID.
     *
     * @param email The email address of the user associated with the invitation token.
     * @param projectId The ID of the project associated with the invitation token.
     * @return A [Result] containing the [InvitationToken] associated with the given email and project ID or a
     * [InvitationTokenNotFoundException] if the token doesn't exist.
     */
    suspend fun getInvitationTokenByEmailAndProjectId(email: String, projectId: UUID): Result<InvitationToken>

    /**
     * Retrieves all invitation tokens for a given project.
     * Only invitation tokens that are still active and not yet expired will be returned.
     *
     * @param projectId The ID of the project for which to retrieve the invitation tokens.
     * @return A list of active [InvitationToken]s associated with the given project.
     */
    suspend fun getActiveInvitationTokensForProject(projectId: UUID): List<InvitationToken>

    /**
     * Deletes an invitation token by its value.
     *
     * @param token The invitation token to delete.
     */
    suspend fun deleteInvitationToken(token: String)

    /**
     * Deletes all invitation tokens that have expired.
     */
    suspend fun deleteExpiredInvitationTokens()
}

class InvitationTokenTableRepo(
    private val db: IDatabase,
) : IInvitationTokenTableRepo {
    override suspend fun saveInvitationToken(email: String, projectId: UUID, token: String) {
        db.query {
            InvitationTokenTable.insertAndGet(ResultRow::toInvitationToken) {
                it[InvitationTokenTable.email] = email
                it[InvitationTokenTable.projectId] = projectId
                it[InvitationTokenTable.token] = token
            }
        }
    }

    override suspend fun getInvitationTokenByValue(token: String): Result<InvitationToken> = db.query {
        val result =
            InvitationTokenTable.getEntityOrNull(ResultRow::toInvitationToken) { InvitationTokenTable.token eq token }

        wrapAsResult(result, InvitationTokenNotFoundException())
    }

    override suspend fun getInvitationTokenByEmailAndProjectId(
        email: String,
        projectId: UUID,
    ): Result<InvitationToken> = db.query {
        val token = InvitationTokenTable.getEntityOrNull(ResultRow::toInvitationToken) {
            (InvitationTokenTable.email eq email) and (InvitationTokenTable.projectId eq projectId)
        }

        wrapAsResult(token, InvitationTokenNotFoundException())
    }

    override suspend fun getActiveInvitationTokensForProject(projectId: UUID): List<InvitationToken> = db.query {
        val allTokens = InvitationTokenTable.getEntities(ResultRow::toInvitationToken) {
            InvitationTokenTable.projectId eq projectId
        }

        allTokens.filter { token -> OffsetDateTime.now().isBefore(token.expiresAt) }
    }

    override suspend fun deleteInvitationToken(token: String) {
        db.query {
            InvitationTokenTable.deleteWhere { InvitationTokenTable.token eq token }
        }
    }

    override suspend fun deleteExpiredInvitationTokens() = db.query {
        val deletedTokens = InvitationTokenTable.deleteWhere {
            InvitationTokenTable.expiresAt lessEq OffsetDateTime.now()
        }

        logger.info { "Deleted $deletedTokens expired invitation tokens." }
    }
}
