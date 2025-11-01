package se.uulm.snowballr.backend.model.jwt

import java.util.Date
import java.util.UUID

/**
 * Represents validated JWT claims.
 */
data class ParsedJwtAuthClaims(
    val userId: UUID,
    val issuedAt: Date?,
    val expiration: Date?,
)
