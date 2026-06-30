package se.uulm.snowballr.backend.model.outgoing

import snowballr.ProjectOuterClass

data class ProjectDecisionStatistics(
    val statistics: List<ProjectDecisionCount>,
)

fun ProjectDecisionStatistics.toGrpc(): ProjectOuterClass.Project.Information.DecisionStatistics =
    ProjectOuterClass.Project.Information.DecisionStatistics.newBuilder()
        .addAllStatistics(statistics.map { it.toGrpc() })
        .build()
