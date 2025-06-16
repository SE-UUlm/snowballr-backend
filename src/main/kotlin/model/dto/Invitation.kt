package se.uulm.snowballr.backend.model.dto

import kotlinx.datetime.Instant
import se.uulm.snowballr.backend.table.association.InvitationTable
import java.util.UUID

/**
 * DTO of [InvitationTable].
 */
data class Invitation(
    val id: Int,
    val projectId: Int,
    val userId: UUID,
    val token: String,
    val validUntil: Instant,
)
