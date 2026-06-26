package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
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
    val localPaperId: Long,
    val stage: Long,
    val decision: PaperDecision,
    val createdAt: OffsetDateTime,
    val createdBy: UUID,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
)

/**
 * Checks whether this [ProjectPaper] has a final decision.
 *
 * A [ProjectPaper] is considered to have a final decision if its decision is set to
 * [PaperDecision.PAPER_DECISION_ACCEPTED] or [PaperDecision.PAPER_DECISION_DECLINED].
 */
fun ProjectPaper.hasFinalDecision() = this.decision == PaperDecision.PAPER_DECISION_ACCEPTED ||
    this.decision == PaperDecision.PAPER_DECISION_DECLINED

/**
 * Checks whether this [ProjectPaper] has no final decision yet.
 *
 * A [ProjectPaper] is considered to have no final decision if its decision is set to
 * [PaperDecision.PAPER_DECISION_UNREVIEWED] or [PaperDecision.PAPER_DECISION_IN_REVIEW].
 */
fun ProjectPaper.hasNoFinalDecision() = !this.hasFinalDecision()
