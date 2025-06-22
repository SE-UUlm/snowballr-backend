package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectPaperTable].
 */
data class ProjectPaper(
    val id: UUID,
    val paperId: UUID,
    val projectId: UUID,
    val localPaperId: Long,
    val stage: Long,
    val decision: ProjectOuterClass.PaperDecision,
    val createdAt: OffsetDateTime,
    val createdBy: UUID,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
)
