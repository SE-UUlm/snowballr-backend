package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import se.uulm.snowballr.backend.auth.JwtUtils
import se.uulm.snowballr.backend.model.dto.Session
import se.uulm.snowballr.backend.table.SessionTable.expiresAt
import se.uulm.snowballr.backend.table.SessionTable.revoked
import se.uulm.snowballr.backend.table.SessionTable.userId
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/**
 * Represents the database table "session" and provides a mapping for managing user sessions in the database.
 *
 * Columns:
 * - [userId]: A foreign key reference to the user who owns the session.
 * - [revoked]: A boolean indicating whether the session has been revoked.
 * - [expiresAt]: A timestamp indicating when the session expires.
 * - [createdAt]: A timestamp indicating when the session was created.
 */
object SessionTable : UUIDTable("session") {
    val userId = userReference("session_by", ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val revoked = bool("revoked").default(false)

    val expiresAt =
        timestampWithTimeZone("expires_at").clientDefault {
            OffsetDateTime.now().plus(JwtUtils.REFRESH_TOKEN_EXPIRATION_MS, ChronoUnit.MILLIS)
        }

    // Metadata

    val createdAt = createdAt()
}

/**
 * Creates a [Session] from this [ResultRow].
 */
fun ResultRow.toSession() = Session(
    id = this[SessionTable.id].value,
    userId = this[SessionTable.userId].value,
    revoked = this[SessionTable.revoked],
    expiresAt = this[SessionTable.expiresAt],
    createdAt = this[SessionTable.createdAt],
)
