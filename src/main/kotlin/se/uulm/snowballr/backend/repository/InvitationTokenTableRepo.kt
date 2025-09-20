package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.dto.InvitationToken
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.toInvitationToken
import java.util.UUID

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
     * Deletes an invitation token by its value.
     *
     * @param token The invitation token to delete.
     */
    suspend fun deleteInvitationToken(token: String)
}

class InvitationTokenTableRepo(
    private val db: IDatabase,
) : IInvitationTokenTableRepo {
    override suspend fun saveInvitationToken(email: String, projectId: UUID, token: String) {
        db.query {
            InvitationTokenTable.insertAndGet(ResultRow::toInvitationToken, EntityType.INVITATION_TOKEN) {
                it[InvitationTokenTable.email] = email
                it[InvitationTokenTable.projectId] = projectId
                it[InvitationTokenTable.token] = token
            }
        }
    }

    override suspend fun getInvitationTokenByValue(token: String): Result<InvitationToken> = db.query {
        val result =
            InvitationTokenTable.getEntityOrNull(ResultRow::toInvitationToken) { InvitationTokenTable.token eq token }

        if (result != null) {
            Result.success(result)
        } else {
            Result.failure(InvitationTokenNotFoundException())
        }
    }

    override suspend fun getInvitationTokenByEmailAndProjectId(
        email: String,
        projectId: UUID,
    ): Result<InvitationToken> = db.query {
        val token = InvitationTokenTable.getEntityOrNull(ResultRow::toInvitationToken) {
            (InvitationTokenTable.email eq email) and (InvitationTokenTable.projectId eq projectId)
        }

        if (token != null) {
            Result.success(token)
        } else {
            Result.failure(InvitationTokenNotFoundException())
        }
    }

    override suspend fun deleteInvitationToken(token: String) {
        db.query {
            InvitationTokenTable.deleteWhere { InvitationTokenTable.token eq token }
        }
    }
}
