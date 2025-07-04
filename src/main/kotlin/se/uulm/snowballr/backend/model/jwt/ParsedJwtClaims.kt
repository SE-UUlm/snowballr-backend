package se.uulm.snowballr.backend.model.jwt

import java.util.Date
import java.util.UUID

/**
 * Represents validated JWT claims.
 */
data class ParsedJwtClaims(
    val userId: UUID,
    val sessionId: UUID,
    val issuedAt: Date?,
    val expiration: Date?,
)
