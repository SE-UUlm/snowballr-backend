package se.uulm.snowballr.backend

import kotlinx.datetime.Instant
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.dto.User
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This class acts as a collection of builder methods to create DTOs for testing purposes.
 */
@Suppress("LongParameterList")
object DataBuilder {
    fun createExampleProject(
        id: UUID = UUID.randomUUID(),
        name: String = "Test Project",
        status: ProjectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE,
        currentStage: Long = 0,
        maxStage: Long = 0,
        similarityThreshold: Float = 0.5F,
        snowballingType: SnowballingType = SnowballingType.SNOWBALLING_TYPE_BOTH,
        reviewMaybeAllowed: Boolean = true,
        reviewDecisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix.getDefaultInstance(),
        fetcherApis: List<String> = emptyList(),
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
        projectId: UUID? = UUID.randomUUID(),
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID = UUID.randomUUID(),
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

    fun createExampleUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test.email@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.USER_ROLE_UNSPECIFIED,
        status: UserStatus = UserStatus.USER_STATUS_UNSPECIFIED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
        deletedAt: OffsetDateTime? = null,
    ) = User(
        id = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        role = role,
        status = status,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        deletedAt = deletedAt,
    )

    fun createExampleProjectMember(
        projectId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        id: String = "$projectId-$userId",
        role: MemberRole = MemberRole.MEMBER_ROLE_UNSPECIFIED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
    ) = ProjectMember(
        id = id,
        projectId = projectId,
        userId = userId,
        role = role,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )

    fun createExamplePaper(
        id: UUID = UUID.randomUUID(),
        title: String = "Title",
        externalId: String? = "ExternalId",
        abstract: String = "Abstract",
        publishedAt: Instant? = Instant.fromEpochSeconds(0),
        publisher: String? = "Publisher",
        publicationType: String? = "PublicationType",
        publicationName: String? = "PublicationName",
        pdfId: UUID? = UUID.randomUUID(),
        fetcherMetadata: Map<String, String> = emptyMap(),
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
        modifiedBy: UUID? = null,
    ) = Paper(
        id,
        title,
        externalId,
        abstract,
        publishedAt,
        publisher,
        publicationType,
        publicationName,
        pdfId,
        fetcherMetadata,
        createdAt,
        modifiedAt,
        modifiedBy,
    )
}
