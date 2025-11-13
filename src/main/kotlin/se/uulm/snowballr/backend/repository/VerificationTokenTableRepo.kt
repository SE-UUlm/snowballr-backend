package se.uulm.snowballr.backend.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.deleteWhere
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.model.dto.VerificationToken
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.table.toVerificationToken
import java.time.OffsetDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }

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
     * @return A [Result] containing the [VerificationToken] associated with the given token value or a
     * [VerificationTokenNotFoundException] if the token doesn't exist.
     */
    suspend fun getVerificationTokenByValue(token: String): Result<VerificationToken>

    /**
     * Deletes a verification token by its value.
     *
     * @param token The verification token to delete.
     */
    suspend fun deleteVerificationToken(token: String)

    /**
     * Deletes all verification tokens that have expired.
     */
    suspend fun deleteExpiredVerificationTokens()
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

    override suspend fun getVerificationTokenByValue(token: String): Result<VerificationToken> = db.query {
        val result =
            VerificationTokenTable.getEntityOrNull(
                ResultRow::toVerificationToken,
            ) { VerificationTokenTable.token eq token }

        if (result != null) {
            Result.success(result)
        } else {
            Result.failure(VerificationTokenNotFoundException())
        }
    }

    override suspend fun deleteVerificationToken(token: String) {
        db.query {
            VerificationTokenTable.deleteWhere { VerificationTokenTable.token eq token }
        }
    }

    override suspend fun deleteExpiredVerificationTokens() = db.query {
        val deletedTokens = VerificationTokenTable.deleteWhere {
            VerificationTokenTable.expiresAt lessEq OffsetDateTime.now()
        }

        logger.info { "Deleted $deletedTokens expired verification tokens." }
    }
}
