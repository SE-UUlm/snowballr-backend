package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.table.ProjectTable
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectTable].
 */
data class Project(
    val id: Int,
    val name: String,
    val status: ProjectOuterClass.ProjectStatus,
    val currentStage: Long,
    val maxStage: Long,
    val similarityThreshold: Float,
    val snowballingType: ProjectOuterClass.SnowballingType,
    val reviewMaybeAllowed: Boolean,
    val reviewDecisionMatrix: ProjectOuterClass.ReviewDecisionMatrix,
    val fetcherApis: List<FetcherApi>,
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
    val settingsBuilder =
        ProjectOuterClass.Project.Settings
            .newBuilder()
            .setSimilarityThreshold(this.similarityThreshold)
            .setDecisionMatrix(this.reviewDecisionMatrix)
            .setSnowballingType(this.snowballingType)
            .setReviewMaybeAllowed(this.reviewMaybeAllowed)
    for (api in this.fetcherApis) {
        settingsBuilder.addFetcherApis(api.name)
    }
    val settings = settingsBuilder.build()

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
