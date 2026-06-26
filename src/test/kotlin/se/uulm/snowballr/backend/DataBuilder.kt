package se.uulm.snowballr.backend

import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.InvitationToken
import se.uulm.snowballr.backend.model.dto.Paper
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.model.dto.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.UserSettings
import se.uulm.snowballr.backend.model.dto.VerificationToken
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.table.patternOf
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.ReviewOuterClass.ReviewDecision
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
        fetchers: FetcherMap = emptyMap(),
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
        fetchers = fetchers,
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

    fun createExampleProjectCriterion(
        id: UUID = UUID.randomUUID(),
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
        projectId: UUID = UUID.randomUUID(),
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID = UUID.randomUUID(),
    ) = Criterion.ProjectCriterion(
        id = id,
        tag = tag,
        name = name,
        description = description,
        category = category,
        projectId = projectId,
        createdAt = createdAt,
        createdBy = createdBy,
    )

    fun createExampleUserCriterion(
        id: UUID = UUID.randomUUID(),
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_UNSPECIFIED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID = UUID.randomUUID(),
    ) = Criterion.UserCriterion(
        id = id,
        tag = tag,
        name = name,
        description = description,
        category = category,
        createdAt = createdAt,
        createdBy = createdBy,
    )

    fun createExampleUser(
        id: UUID = UUID.randomUUID(),
        email: String = "test.email@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        role: UserRole = UserRole.USER_ROLE_DEFAULT,
        status: UserStatus = UserStatus.USER_STATUS_ACTIVE,
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
        role: MemberRole = MemberRole.MEMBER_ROLE_UNSPECIFIED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
    ) = ProjectMember(
        projectId = projectId,
        userId = userId,
        role = role,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )

    fun createExampleUserSettings(
        showHotkeys: Boolean = true,
        reviewMode: Boolean = false,
        criteriaIds: List<UUID> = emptyList(),
        similarityThreshold: Float = 0.5f,
        decisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix.getDefaultInstance(),
        fetchers: FetcherMap = emptyMap(),
        snowballingType: SnowballingType = SnowballingType.SNOWBALLING_TYPE_BOTH,
        reviewMaybeAllowed: Boolean = false,
    ) = UserSettings(
        areHotkeysShown = showHotkeys,
        isReviewModeEnabled = reviewMode,
        criteriaIds = criteriaIds,
        similarityThreshold = similarityThreshold,
        decisionMatrix = decisionMatrix,
        fetchers = fetchers,
        snowballingType = snowballingType,
        reviewMaybeAllowed = reviewMaybeAllowed,
    )

    fun createExamplePaper(
        id: UUID = UUID.randomUUID(),
        title: String = "Title",
        externalId: String? = "ExternalId",
        abstract: String = "Abstract",
        year: Int = 2025,
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        pdfId: UUID? = UUID.randomUUID(),
        fetcherMetadata: Map<String, String> = emptyMap(),
        authors: List<Author> = emptyList(),
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
        modifiedBy: UUID? = null,
    ) = Paper(
        id = id,
        title = title,
        externalId = externalId,
        abstract = abstract,
        year = year,
        publisher = publisher,
        publicationType = publicationType,
        publicationName = publicationName,
        pdfId = pdfId,
        authors = authors,
        fetcherMetadata = fetcherMetadata,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
        modifiedBy = modifiedBy,
    )

    fun createExampleProjectPaper(
        id: UUID = UUID.randomUUID(),
        paperId: UUID = UUID.randomUUID(),
        projectId: UUID = UUID.randomUUID(),
        localPaperId: Long = 0,
        stage: Long = 0,
        decision: PaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        createdBy: UUID = UUID.randomUUID(),
        modifiedAt: OffsetDateTime? = null,
        modifiedBy: UUID? = null,
    ) = ProjectPaper(
        id = id,
        paperId = paperId,
        projectId = projectId,
        localPaperId = localPaperId,
        stage = stage,
        decision = decision,
        createdAt = createdAt,
        createdBy = createdBy,
        modifiedAt = modifiedAt,
        modifiedBy = modifiedBy,
    )

    fun createExampleAuthor(firstName: String = "FirstName", lastName: String = "LastName") = Author(
        firstName = firstName,
        lastName = lastName,
    )

    fun createExampleReview(
        id: UUID = UUID.randomUUID(),
        projectPaperId: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        decision: ReviewDecision = ReviewDecision.REVIEW_DECISION_ACCEPTED,
        createdAt: OffsetDateTime = OffsetDateTime.now(),
        modifiedAt: OffsetDateTime? = null,
    ) = Review(
        id = id,
        projectPaperId = projectPaperId,
        userId = userId,
        decision = decision,
        createdAt = createdAt,
        modifiedAt = modifiedAt,
    )

    fun createExampleVerificationToken(
        id: UUID = UUID.randomUUID(),
        userId: UUID = UUID.randomUUID(),
        token: String = "example-token",
        expiresAt: OffsetDateTime = OffsetDateTime.now().plusDays(1),
    ) = VerificationToken(
        id = id,
        userId = userId,
        token = token,
        expiresAt = expiresAt,
    )

    fun createExampleInvitationToken(
        id: UUID = UUID.randomUUID(),
        email: String = "example-email",
        projectId: UUID = UUID.randomUUID(),
        token: String = "example-token",
        expiresAt: OffsetDateTime = OffsetDateTime.now().plusDays(1),
    ) = InvitationToken(
        id = id,
        email = email,
        projectId = projectId,
        token = token,
        expiresAt = expiresAt,
    )

    private val ACCEPT_DECLINE_PATTERN = patternOf(
        ReviewDecision.REVIEW_DECISION_ACCEPTED to 1L,
        ReviewDecision.REVIEW_DECISION_DECLINED to 1L,
        result = PaperDecision.PAPER_DECISION_IN_REVIEW,
    )
    private val ACCEPT_ANY_PATTERN = patternOf(
        ReviewDecision.REVIEW_DECISION_ACCEPTED to 1L,
        result = PaperDecision.PAPER_DECISION_ACCEPTED,
    )
    private val DECLINE_ANY_PATTERN = patternOf(
        ReviewDecision.REVIEW_DECISION_DECLINED to 1L,
        result = PaperDecision.PAPER_DECISION_DECLINED,
    )
    private val MAYBE_MAYBE_PATTERN = patternOf(
        ReviewDecision.REVIEW_DECISION_MAYBE to 2L,
        result = PaperDecision.PAPER_DECISION_IN_REVIEW,
    )

    fun createExampleReviewDecisionMatrix(
        numberOfReviewers: Int = 2,
        pattern: List<ReviewDecisionMatrix.Pattern> = listOf(
            ACCEPT_DECLINE_PATTERN,
            ACCEPT_ANY_PATTERN,
            DECLINE_ANY_PATTERN,
            MAYBE_MAYBE_PATTERN,
        ),
    ): ReviewDecisionMatrix = ReviewDecisionMatrix.newBuilder()
        .setNumberOfReviewers(numberOfReviewers)
        .addAllPatterns(pattern)
        .build()

    fun createExampleProjectMemberWithUser(
        projectMember: ProjectMember = createExampleProjectMember(),
        user: User = createExampleUser(),
    ) = ProjectMemberWithUser(
        projectMember = projectMember,
        user = user,
    )

    fun createExampleProjectPaperFull(
        projectPaper: ProjectPaper = createExampleProjectPaper(),
        paper: Paper = createExamplePaper(),
        reviewsWithSelectedCriteria: List<ReviewWithSelectedCriteriaIds> = emptyList(),
    ) = ProjectPaperFull(
        projectPaper = projectPaper,
        paper = paper,
        reviewsWithSelectedCriteria = reviewsWithSelectedCriteria,
    )

    fun createExampleProjectPaperWithPaper(
        projectPaper: ProjectPaper = createExampleProjectPaper(),
        paper: Paper = createExamplePaper(),
    ) = ProjectPaperWithPaper(
        projectPaper = projectPaper,
        paper = paper,
    )

    fun createExampleReviewWithSelectedCriteriaIds(
        review: Review = createExampleReview(),
        selectedCriteriaIds: List<UUID> = emptyList(),
    ) = ReviewWithSelectedCriteriaIds(
        review = review,
        selectedCriteriaIds = selectedCriteriaIds,
    )

    fun createExampleFetcherEnqueueJob(
        projectPaper: ProjectPaper = createExampleProjectPaper(),
        triggeringUserId: UUID = UUID.randomUUID(),
    ) = FetcherEnqueueJob(
        projectPaper = projectPaper,
        triggeringUserId = triggeringUserId,
    )

    fun createExampleFetcherPaper(
        title: String = "Title",
        externalId: String? = "ExternalId",
        abstract: String = "Abstract",
        year: Int = 2025,
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        fetcherMetadata: Map<String, String> = emptyMap(),
        authors: List<Author> = emptyList(),
    ) = FetcherPaper(
        title = title,
        externalId = externalId,
        abstract = abstract,
        year = year,
        publisher = publisher,
        publicationType = publicationType,
        publicationName = publicationName,
        authors = authors,
        fetcherMetadata = fetcherMetadata,
    )
}
