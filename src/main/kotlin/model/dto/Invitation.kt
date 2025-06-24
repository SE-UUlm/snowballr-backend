package se.uulm.snowballr.backend.model.dto

import kotlinx.datetime.Instant
import org.jetbrains.exposed.dao.id.CompositeID
import se.uulm.snowballr.backend.table.association.InvitationTable
import java.util.UUID

/**
 * DTO of [InvitationTable].
 */
data class Invitation(
    val id: CompositeID,
    val projectId: UUID,
    val userId: UUID,
    val token: String,
    val validUntil: Instant,
)
