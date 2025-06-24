package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.association.ReviewTable
import snowballr.ReviewOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ReviewTable].
 */
data class Review(
    val id: UUID,
    val projectPaperId: UUID,
    val userId: UUID,
    val decision: ReviewOuterClass.ReviewDecision,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)
