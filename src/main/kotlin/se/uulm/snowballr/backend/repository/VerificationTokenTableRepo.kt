package se.uulm.snowballr.backend.repository

import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.dto.VerificationToken
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
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
    private val envReader: EnvReader,
) : IVerificationTokenTableRepo {
    override suspend fun saveVerificationToken(userId: UUID, token: String) {
        val verificationTokenLifeTimeInDays = envReader.env.lifetime.verificationTokenLifeTimeInDays.toLong()
        val expirationDate = OffsetDateTime.now().plusDays(verificationTokenLifeTimeInDays)

        db.query {
            VerificationTokenTable.insertAndGet(ResultRow::toVerificationToken) {
                it[VerificationTokenTable.userId] = userId
                it[VerificationTokenTable.token] = token
                it[VerificationTokenTable.expiresAt] = expirationDate
            }
        }
    }

    override suspend fun getVerificationTokenByValue(token: String): Result<VerificationToken> = db.query {
        val result = VerificationTokenTable.getEntityOrNull(ResultRow::toVerificationToken) {
            VerificationTokenTable.token eq token
        }

        wrapAsResult(result, VerificationTokenNotFoundException())
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
