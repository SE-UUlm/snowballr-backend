package se.uulm.snowballr.backend.model.outgoing.project

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision

data class ProjectDecisionCount(
    val decision: PaperDecision,
    val count: Int,
)
