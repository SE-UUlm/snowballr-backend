package se.uulm.snowballr.backend.model.dto.user

import se.uulm.snowballr.backend.table.VerificationTokenTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [VerificationTokenTable].
 */
data class VerificationToken(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: OffsetDateTime,
)
