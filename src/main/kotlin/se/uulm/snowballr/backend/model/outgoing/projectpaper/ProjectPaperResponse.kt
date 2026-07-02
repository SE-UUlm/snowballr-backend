package se.uulm.snowballr.backend.model.outgoing.projectpaper

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import java.util.UUID

data class ProjectPaperResponse(
    val id: UUID,
    val stage: Int,
    val decision: PaperDecision,
    val localPaperId: Int,
    val paper: PaperResponse,
    val reviews: List<ReviewResponse>,
) {
    companion object {
        fun fromProjectPaperWithPaper(
            paper: ProjectPaperWithPaper,
            backwardReferencedIds: List<UUID>,
            reviews: List<ReviewResponse>,
        ) = ProjectPaperResponse(
            id = paper.projectPaper.id,
            stage = paper.projectPaper.stage,
            decision = paper.projectPaper.decision,
            localPaperId = paper.projectPaper.localPaperId,
            paper = PaperResponse.fromPaper(paper.paper, backwardReferencedIds),
            reviews = reviews,
        )
    }
}
