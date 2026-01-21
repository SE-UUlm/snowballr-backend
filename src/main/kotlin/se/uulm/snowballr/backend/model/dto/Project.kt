package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.fetcher.FetcherMap
import se.uulm.snowballr.backend.table.ProjectTable
import snowballr.Fetcher.FetcherOptions
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectTable].
 */
data class Project(
    val id: UUID,
    val name: String,
    val status: ProjectOuterClass.ProjectStatus,
    val currentStage: Long,
    val maxStage: Long,
    val similarityThreshold: Float,
    val snowballingType: ProjectOuterClass.SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val reviewDecisionMatrix: ProjectOuterClass.ReviewDecisionMatrix,
    val fetchers: FetcherMap,
    val currentStageStartedAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val createdBy: UUID,
    val modifiedAt: OffsetDateTime?,
    val modifiedBy: UUID?,
    val deletedAt: OffsetDateTime?,
    val deletedBy: UUID?,
    val archivedAt: OffsetDateTime?,
    val archivedBy: UUID?,
)

/**
 * Creates a [ProjectOuterClass.Project] from this [Project].
 */
fun Project.toGrpcProject(): ProjectOuterClass.Project {
    val settings =
        ProjectOuterClass.Project.Settings
            .newBuilder()
            .setSimilarityThreshold(this.similarityThreshold)
            .setDecisionMatrix(this.reviewDecisionMatrix)
            .setSnowballingType(this.snowballingType)
            .setReviewMaybeAllowed(this.reviewMaybeAllowed)
            .putAllFetchers(
                this.fetchers.mapValues {
                    FetcherOptions
                        .newBuilder()
                        .putAllOptions(it.value)
                        .build()
                },
            )
            .build()

    return ProjectOuterClass.Project
        .newBuilder()
        .setId(this.id.toString())
        .setName(this.name)
        .setStatus(this.status)
        .setCurrentStage(this.currentStage)
        .setMaxStage(this.maxStage)
        .setSettings(settings)
        .build()
}

/**
 * Creates a list of [ProjectOuterClass.Project]s from this list of [Project]s.
 */
fun List<Project>.toGrpcProjects(): ProjectOuterClass.Project.List {
    val builder = ProjectOuterClass.Project.List.newBuilder()
    this.forEach { builder.addProjects(it.toGrpcProject()) }
    return builder.build()
}
