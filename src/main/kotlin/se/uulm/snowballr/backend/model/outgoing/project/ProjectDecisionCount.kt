package se.uulm.snowballr.backend.model.outgoing.project

import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import snowballr.ProjectOuterClass

data class ProjectDecisionCount(
    val decision: PaperDecision,
    val count: Int,
)

fun ProjectDecisionCount.toGrpc(): ProjectOuterClass.Project.Information.DecisionStatistics.Statistic =
    ProjectOuterClass.Project.Information.DecisionStatistics.Statistic.newBuilder()
        .setDecision(decision.toGrpc())
        .setCount(count.toLong())
        .build()
