package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.VerificationToken
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.table.toVerificationToken
import java.util.UUID

interface IVerificationTokenTableRepo {
    /**
     * Saves a verification token for the given email.
     *
     * @param userId The ID of the user associated with the verification token.
     * @param token The verification token.
     */
    suspend fun saveVerificationToken(userId: UUID, token: String)

    /**
     * Retrieves a verification token by its value.
     *
     * @param token The verification token to retrieve.
     * @return The [VerificationToken] associated with the given token value.
     */
    suspend fun getVerificationTokenByValue(token: String): VerificationToken?

    /**
     * Deletes a verification token by its value.
     *
     * @param token The verification token to delete.
     */
    suspend fun deleteVerificationToken(token: String)
}

class VerificationTokenTableRepo(
    private val db: IDatabase,
) : IVerificationTokenTableRepo {
    override suspend fun saveVerificationToken(userId: UUID, token: String) {
        db.query {
            VerificationTokenTable.insertAndGet(ResultRow::toVerificationToken, EntityType.VERIFICATION_TOKEN) {
                it[VerificationTokenTable.userId] = userId
                it[VerificationTokenTable.token] = token
            }
        }
    }

    override suspend fun getVerificationTokenByValue(token: String): VerificationToken? = db.query {
        VerificationTokenTable
            .selectAll()
            .where { VerificationTokenTable.token eq token }
            .map { it.toVerificationToken() }
            .singleOrNull()
    }

    override suspend fun deleteVerificationToken(token: String) {
        db.query {
            VerificationTokenTable.deleteWhere { VerificationTokenTable.token eq token }
        }
    }
}
