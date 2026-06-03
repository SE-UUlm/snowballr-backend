package se.uulm.snowballr.backend.model.dto

import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [InvitationToken].
 */
data class InvitationToken(
    val id: UUID,
    val email: String,
    val projectId: UUID,
    val token: String,
    val expiresAt: OffsetDateTime,
)
