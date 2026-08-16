package se.uulm.snowballr.backend

import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.paper.Author
import se.uulm.snowballr.backend.model.dto.paper.ExternalId
import se.uulm.snowballr.backend.model.dto.paper.ExternalIdType
import se.uulm.snowballr.backend.model.dto.paper.Paper
import se.uulm.snowballr.backend.model.dto.project.DecisionMatrixPattern
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectmember.InvitationToken
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMember
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaper
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperFull
import se.uulm.snowballr.backend.model.dto.projectpaper.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserSettings
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.dto.user.VerificationToken
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.fetcher.FetcherInformation
import se.uulm.snowballr.backend.model.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.fetcher.FetcherOptionsSchema
import se.uulm.snowballr.backend.model.fetcher.FetcherPaper
import se.uulm.snowballr.backend.model.fetcher.Link
import se.uulm.snowballr.backend.table.patternOf
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
        status: ProjectStatus = ProjectStatus.ACTIVE,
        currentStage: Int = 0,
        maxStage: Int = 0,
        similarityThreshold: Float = 0.5F,
        snowballingType: SnowballingType = SnowballingType.BOTH,
        reviewMaybeAllowed: Boolean = true,
        reviewDecisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix(1, emptyList()),
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
        category: CriterionCategory = CriterionCategory.INCLUSION,
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
        category: CriterionCategory = CriterionCategory.INCLUSION,
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
        role: UserRole = UserRole.DEFAULT,
        status: UserStatus = UserStatus.ACTIVE,
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
        role: MemberRole = MemberRole.DEFAULT,
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
        decisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix(1, emptyList()),
        fetchers: FetcherMap = emptyMap(),
        snowballingType: SnowballingType = SnowballingType.BOTH,
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
        externalIds: List<ExternalId> = emptyList(),
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
        externalIds = externalIds,
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
        localPaperId: Int = 0,
        stage: Int = 0,
        decision: PaperDecision = PaperDecision.ACCEPTED,
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
        decision: ReviewDecision = ReviewDecision.ACCEPTED,
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
        ReviewDecision.ACCEPTED to 1,
        ReviewDecision.DECLINED to 1,
        result = PaperDecision.IN_REVIEW,
    )
    private val ACCEPT_ANY_PATTERN = patternOf(
        ReviewDecision.ACCEPTED to 1,
        result = PaperDecision.ACCEPTED,
    )
    private val DECLINE_ANY_PATTERN = patternOf(
        ReviewDecision.DECLINED to 1,
        result = PaperDecision.DECLINED,
    )
    private val MAYBE_MAYBE_PATTERN = patternOf(
        ReviewDecision.MAYBE to 2,
        result = PaperDecision.IN_REVIEW,
    )

    fun createExampleReviewDecisionMatrix(
        numberOfReviewers: Int = 2,
        patterns: List<DecisionMatrixPattern> = listOf(
            ACCEPT_DECLINE_PATTERN,
            ACCEPT_ANY_PATTERN,
            DECLINE_ANY_PATTERN,
            MAYBE_MAYBE_PATTERN,
        ),
    ) = ReviewDecisionMatrix(
        numberOfReviewers = numberOfReviewers,
        patterns = patterns,
    )

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
        externalIds: List<ExternalId> = emptyList(),
        abstract: String = "Abstract",
        year: Int = 2025,
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        fetcherMetadata: Map<String, String> = emptyMap(),
        authors: List<Author> = emptyList(),
    ) = FetcherPaper(
        title = title,
        externalIds = externalIds,
        abstract = abstract,
        year = year,
        publisher = publisher,
        publicationType = publicationType,
        publicationName = publicationName,
        authors = authors,
        fetcherMetadata = fetcherMetadata,
    )

    fun createExampleExternalId(type: ExternalIdType = ExternalIdType.DOI, value: String = "10.1234/5678") = ExternalId(
        type = type,
        value = value,
    )

    fun createExampleFetcherInformation(
        name: String = "Fetcher",
        description: String = "Description",
        links: List<Link> = emptyList(),
        optionSchema: Map<String, FetcherOptionsSchema> = emptyMap(),
    ) = FetcherInformation(
        name = name,
        description = description,
        links = links,
        optionsSchema = optionSchema,
    )

    fun createExampleFetcherOptionsSchema(
        name: String = "Option",
        description: String = "Description",
        isRequired: Boolean = false,
        isSecret: Boolean = false,
        defaultValue: String? = null,
    ) = FetcherOptionsSchema(
        name = name,
        description = description,
        isRequired = isRequired,
        isSecret = isSecret,
        defaultValue = defaultValue,
    )
}
