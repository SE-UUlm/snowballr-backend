package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
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
     * @return The [InvitationToken] associated with the given token value.
     */
    suspend fun getInvitationTokenByValue(token: String): InvitationToken?

    /**
     * Retrieves an invitation token by its email and project ID.
     *
     * @param email The email address of the user associated with the invitation token.
     * @param projectId The ID of the project associated with the invitation token.
     * @return The [InvitationToken] associated with the given email and project ID.
     */
    suspend fun getInvitationTokenByEmailAndProjectId(email: String, projectId: UUID): InvitationToken?

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

    override suspend fun getInvitationTokenByValue(token: String): InvitationToken? = db.query {
        InvitationTokenTable
            .selectAll()
            .where { InvitationTokenTable.token eq token }
            .map { it.toInvitationToken() }
            .singleOrNull()
    }

    override suspend fun getInvitationTokenByEmailAndProjectId(email: String, projectId: UUID): InvitationToken? =
        db.query {
            InvitationTokenTable
                .selectAll()
                .where {
                    (InvitationTokenTable.email eq email) and (InvitationTokenTable.projectId eq projectId)
                }
                .map { it.toInvitationToken() }
                .singleOrNull()
        }

    override suspend fun deleteInvitationToken(token: String) {
        db.query {
            InvitationTokenTable.deleteWhere { InvitationTokenTable.token eq token }
        }
    }
}
