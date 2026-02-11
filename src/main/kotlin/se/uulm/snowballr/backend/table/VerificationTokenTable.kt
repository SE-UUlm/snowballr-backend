package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import se.uulm.snowballr.backend.model.dto.VerificationToken
import java.time.OffsetDateTime

/**
 * Represents the database table "verification_token" and provides a mapping for managing verification tokens in the database.
 *
 * Columns:
 * - [userId]: Foreign key referencing the [UserTable], representing the user associated with the verification token.
 * - [token]: Represents the verification token (nano id) as a [String]. The token is obfuscated for security purposes.
 * - [expiresAt]: Represents the expiration timestamp of the verification token as an [OffsetDateTime].
 */
object VerificationTokenTable : UUIDTable("verification_token") {
    /**
     * Reference to the associated user.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the user is deleted
     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
     */
    val userId = userReference("user_id", ReferenceOption.CASCADE, ReferenceOption.CASCADE).uniqueIndex()
    val token = obfuscatedText("token").uniqueIndex()
    val expiresAt = timestampWithTimeZone("expires_at")
}

/**
 * Creates a [VerificationToken] from this [ResultRow].
 */
fun ResultRow.toVerificationToken() = VerificationToken(
    id = this[VerificationTokenTable.id].value,
    userId = this[VerificationTokenTable.userId].value,
    token = this[VerificationTokenTable.token],
    expiresAt = this[VerificationTokenTable.expiresAt],
)
