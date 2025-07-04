package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.SessionTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of a [SessionTable]
 */
data class Session(
    val id: UUID,
    val userId: UUID,
    val revoked: Boolean,
    val expiresAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
)
