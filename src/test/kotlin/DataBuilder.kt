package se.uulm.snowballr.backend

import se.uulm.snowballr.backend.model.FetcherApi
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Project
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This class acts as a collection of builder methods to create DTOs for testing purposes.
 */
@Suppress("LongParameterList")
object DataBuilder {
    fun createExampleProject(
        id: Int = 0,
        name: String = "Test Project",
        status: ProjectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE,
        currentStage: Long = 0,
        maxStage: Long = 0,
        similarityThreshold: Float = 0.5F,
        snowballingType: SnowballingType = SnowballingType.SNOWBALLING_TYPE_BOTH,
        reviewMaybeAllowed: Boolean = true,
        reviewDecisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix.getDefaultInstance(),
        fetcherApis: List<FetcherApi> = emptyList(),
        currentStageStartedAt: OffsetDateTime = OffsetDateTime.now(),
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID = UUID.randomUUID(),
        modifiedAt: OffsetDateTime? = null,
        modifiedBy: UUID? = null,
        deletedAt: OffsetDateTime? = null,
        deletedBy: UUID? = null,
        archivedAt: OffsetDateTime? = null,
        archivedBy: UUID? = null,
    ) = Project(
        id = id,
        name = name,
        status = status,
        currentStage = currentStage,
        maxStage = maxStage,
        similarityThreshold = similarityThreshold,
        snowballingType = snowballingType,
        reviewMaybeAllowed = reviewMaybeAllowed,
        reviewDecisionMatrix = reviewDecisionMatrix,
        fetcherApis = fetcherApis,
        currentStageStartedAt = currentStageStartedAt,
        createdAt = createdAt,
        createdBy = createdBy,
        modifiedAt = modifiedAt,
        modifiedBy = modifiedBy,
        deletedAt = deletedAt,
        deletedBy = deletedBy,
        archivedAt = archivedAt,
        archivedBy = archivedBy,
    )

    fun createExampleCriterion(
        id: UUID = UUID.randomUUID(),
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
        projectId: Int = 0,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID? = null,
    ) = Criterion(
        id = id,
        tag = tag,
        name = name,
        description = description,
        category = category,
        projectId = projectId,
        createdAt = createdAt,
        createdBy = createdBy,
    )
}
