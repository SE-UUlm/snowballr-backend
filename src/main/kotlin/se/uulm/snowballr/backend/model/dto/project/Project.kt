package se.uulm.snowballr.backend.model.dto.project

import se.uulm.snowballr.backend.model.fetcher.FetcherMap
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
    val status: ProjectStatus,
    val currentStage: Int,
    val maxStage: Int,
    val similarityThreshold: Float,
    val snowballingType: SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val reviewDecisionMatrix: ReviewDecisionMatrix,
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
            .setDecisionMatrix(this.reviewDecisionMatrix.toGrpc())
            .setSnowballingType(this.snowballingType.toGrpc())
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
        .setStatus(this.status.toGrpc())
        .setCurrentStage(this.currentStage.toLong())
        .setMaxStage(this.maxStage.toLong())
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

/**
 * Checks whether the project is active.
 *
 * A project is considered active if its status is either [ProjectStatus.ACTIVE] or [ProjectStatus.ACTIVE_LOCKED].
 */
fun Project.isActive() = this.status == ProjectStatus.ACTIVE || this.status == ProjectStatus.ACTIVE_LOCKED

/**
 * Checks whether the project is deleted.
 *
 * A project is considered deleted if its status is [ProjectStatus.DELETED].
 */
fun Project.isDeleted() = this.status == ProjectStatus.DELETED
