package se.uulm.snowballr.backend.model.dto.projectpaper

import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectPaperTable].
 */
data class ProjectPaper(
    val id: UUID,
    val paperId: UUID,
    val projectId: UUID,
    val localPaperId: Int,
    val stage: Int,
    val decision: PaperDecision,
    val createdAt: OffsetDateTime,
    val createdBy: UUID,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
)

/**
 * Checks whether this [ProjectPaper] has a final decision.
 *
 * A [ProjectPaper] is considered to have a final decision if its decision is set to [PaperDecision.ACCEPTED] or
 * [PaperDecision.DECLINED].
 */
fun ProjectPaper.hasFinalDecision() = this.decision == PaperDecision.ACCEPTED || this.decision == PaperDecision.DECLINED

/**
 * Checks whether this [ProjectPaper] has no final decision yet.
 *
 * A [ProjectPaper] is considered to have no final decision if its decision is set to [PaperDecision.UNREVIEWED] or
 * [PaperDecision.IN_REVIEW].
 */
fun ProjectPaper.hasNoFinalDecision() = !this.hasFinalDecision()
