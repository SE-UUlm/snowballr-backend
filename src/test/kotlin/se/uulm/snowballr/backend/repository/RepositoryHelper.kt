@file:Suppress("LongParameterList")

package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.fetcher.FetcherMap
import se.uulm.snowballr.backend.model.dto.Author
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.InvitationTokenTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.VerificationTokenTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.MemberRole
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.ReviewOuterClass.ReviewDecision
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * This class acts as a collection of create methods to create database entries for testing purposes.
 */
object RepositoryHelper {
    lateinit var db: IDatabase

    /**
     * Creates an example user in the database with the specified properties.
     *
     * @return The UUID of the created user.
     */
    suspend fun insertUserAndGetId(
        email: String = "test.user@example.com",
        firstName: String = "Test",
        lastName: String = "User",
        passwordHash: String = "passwordHash",
        role: UserRole = UserRole.USER_ROLE_DEFAULT,
        status: UserStatus = UserStatus.USER_STATUS_ACTIVE,
        deletedAt: OffsetDateTime? = null,
    ) = db.query {
        UserTable
            .insertAndGetId {
                it[UserTable.email] = email
                it[UserTable.firstName] = firstName
                it[UserTable.lastName] = lastName
                it[UserTable.passwordHash] = passwordHash
                it[UserTable.role] = role
                it[UserTable.status] = status
                it[UserTable.deletedAt] = deletedAt
            }.value
    }

    /**
     * Assigns a user to a project by creating an entry in the [ProjectMemberTable].
     *
     * @param userId The ID of the user to be assigned to the project.
     * @param projectId The ID of the project to which the user is being assigned.
     * @return The created project member entity.
     */
    suspend fun assignUserToProject(userId: UUID, projectId: UUID) = db.query {
        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember) {
            it[ProjectMemberTable.userId] = userId
            it[ProjectMemberTable.projectId] = projectId
            it[role] = MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    /**
     * Creates a user with the specified email and assigns them to a project.
     *
     * @param email The email address of the user to be created.
     * @param projectId The ID of the project to which the user will be assigned.
     */
    suspend fun createAndAssignUserToProject(email: String, projectId: UUID) = db.query {
        val userId = insertUserAndGetId(email)
        assignUserToProject(userId, projectId)
    }

    /**
     * Creates an example paper in the database with the specified properties.
     *
     * @return The UUID of the created paper.
     */
    suspend fun insertPaperAndGetId(
        title: String = "Title",
        externalId: String = UUID.randomUUID().toString(),
        abstract: String = "Abstract",
        year: Int = 2025,
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        fetcherMetadata: Map<String, String> = emptyMap(),
        authors: List<Author> = emptyList(),
    ): UUID = db.query {
        PaperTable.insertAndGetId {
            it[PaperTable.title] = title
            it[PaperTable.externalId] = externalId
            it[PaperTable.abstract] = abstract
            it[PaperTable.year] = year
            it[PaperTable.publisher] = publisher
            it[PaperTable.publicationType] = publicationType
            it[PaperTable.publicationName] = publicationName
            it[PaperTable.fetcherMetadata] = fetcherMetadata
            it[PaperTable.authors] = authors
        }.value
    }

    /**
     * Creates an example project in the database with the specified properties.
     *
     * @return The UUID of the created project.
     */
    suspend fun insertProjectAndGetId(
        name: String = "Test Project",
        status: ProjectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE,
        currentStage: Long = 0,
        maxStage: Long = 0,
        similarityThreshold: Float = 0F,
        snowballingType: SnowballingType = SnowballingType.SNOWBALLING_TYPE_BOTH,
        reviewMaybeAllowed: Boolean = true,
        reviewDecisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix.getDefaultInstance(),
        fetcherApis: FetcherMap = emptyMap(),
        createdBy: UUID,
        deletedAt: OffsetDateTime? = null,
    ): UUID = db.query {
        ProjectTable
            .insertAndGetId {
                it[ProjectTable.name] = name
                it[ProjectTable.status] = status
                it[ProjectTable.currentStage] = currentStage
                it[ProjectTable.maxStage] = maxStage
                it[ProjectTable.similarityThreshold] = similarityThreshold
                it[ProjectTable.snowballingType] = snowballingType
                it[ProjectTable.reviewMaybeAllowed] = reviewMaybeAllowed
                it[ProjectTable.reviewDecisionMatrixBinary] = reviewDecisionMatrix.toByteArray()
                it[ProjectTable.fetchers] = fetcherApis
                it[ProjectTable.createdBy] = createdBy
                it[ProjectTable.deletedAt] = deletedAt
            }.value
    }

    /**
     * Creates an example project paper in the database with the specified properties.
     *
     * @return The UUID of the created project paper.
     */
    suspend fun insertProjectPaperAndGetId(
        paperId: UUID,
        projectId: UUID,
        localPaperId: Long = 0,
        stage: Long = 0,
        decision: PaperDecision = PaperDecision.PAPER_DECISION_ACCEPTED,
        createdBy: UUID,
    ): UUID = db.query {
        ProjectPaperTable.insertAndGetId {
            it[ProjectPaperTable.paperId] = paperId
            it[ProjectPaperTable.projectId] = projectId
            it[ProjectPaperTable.localPaperId] = localPaperId
            it[ProjectPaperTable.stage] = stage
            it[ProjectPaperTable.decision] = decision
            it[ProjectPaperTable.createdBy] = createdBy
        }.value
    }

    /**
     * Creates an example review in the database with the specified properties.
     *
     * @return The UUID of the created review.
     */
    suspend fun insertReviewAndGetId(
        projectPaperId: UUID,
        userId: UUID,
        decision: ReviewDecision = ReviewDecision.REVIEW_DECISION_ACCEPTED,
    ): UUID = db.query {
        ReviewTable.insertAndGetId {
            it[ReviewTable.projectPaperId] = projectPaperId
            it[ReviewTable.userId] = userId
            it[ReviewTable.decision] = decision
        }.value
    }

    /**
     * Creates an example criterion in the database with the specified properties.
     *
     * @return The UUID of the created criterion.
     */
    suspend fun insertCriterionAndGetId(
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
        projectId: UUID? = null,
        createdBy: UUID,
    ): UUID = db.query {
        CriterionTable.insertAndGetId {
            it[CriterionTable.tag] = tag
            it[CriterionTable.name] = name
            it[CriterionTable.description] = description
            it[CriterionTable.category] = category
            it[CriterionTable.projectId] = projectId
            it[CriterionTable.createdBy] = createdBy
        }.value
    }

    /**
     * Assigns a criterion to a review by inserting an association into the [ReviewHasCriterionTable].
     *
     * @param reviewId The ID of the review to which the criterion is assigned.
     * @param criterionId The ID of the criterion to be assigned to the review.
     */
    suspend fun assignCriterionToReview(reviewId: UUID, criterionId: UUID) = db.query {
        ReviewHasCriterionTable.insert {
            it[ReviewHasCriterionTable.reviewId] = reviewId
            it[ReviewHasCriterionTable.criterionId] = criterionId
        }
    }

    /**
     * Creates a test verification token (default "secure-random-invitation-token-123") in the database
     * for the specified user.
     */
    suspend fun insertTestVerificationToken(
        userId: UUID,
        token: String = "secure-random-invitation-token-123",
        expiresAt: OffsetDateTime = OffsetDateTime.now().plusDays(1),
    ) {
        db.query {
            VerificationTokenTable.insert {
                it[VerificationTokenTable.userId] = userId
                it[VerificationTokenTable.token] = token
                it[VerificationTokenTable.expiresAt] = expiresAt
            }
        }
    }

    /**
     * Creates a test invitation token (default "secure-random-invitation-token-123") in the database
     * for the specified properties.
     */
    suspend fun insertTestInvitationToken(
        email: String,
        projectId: UUID,
        token: String = "secure-random-invitation-token-123",
        expiresAt: OffsetDateTime = OffsetDateTime.now().plusDays(1),
    ) {
        db.query {
            InvitationTokenTable.insert {
                it[InvitationTokenTable.email] = email
                it[InvitationTokenTable.projectId] = projectId
                it[InvitationTokenTable.token] = token
                it[InvitationTokenTable.expiresAt] = expiresAt
            }
        }
    }
}
