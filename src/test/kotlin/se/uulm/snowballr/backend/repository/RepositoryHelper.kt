package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.insertAndGetId
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.association.ReviewTable
import se.uulm.snowballr.backend.table.association.toProjectMember
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ProjectOuterClass.SnowballingType
import snowballr.ReviewOuterClass
import snowballr.UserOuterClass
import java.util.UUID

/**
 * This class acts as a collection of create methods to create database entries for testing purposes.
 */
object RepositoryHelper {
    lateinit var db: IDatabase

    /**
     * Creates an example user in the database with the specified email and fake user details.
     *
     * @param email The email address for the example user to be created.
     * @return The uuid of the created user
     */
    suspend fun createExampleUser(email: String) = db.query {
        UserTable
            .insertAndGetId {
                it[UserTable.email] = email
                it[firstName] = "Test"
                it[lastName] = "User"
                it[passwordHash] = "1234"
                it[role] = UserOuterClass.UserRole.USER_ROLE_DEFAULT
                it[status] = UserOuterClass.UserStatus.USER_STATUS_ACTIVE
            }.value
    }

    /**
     * Assigns a user to a project by inserting an entry into the ProjectMemberTable with the default member role.
     *
     * @param userId The unique identifier of the user to be assigned to the project.
     * @param projectId The unique identifier of the project to which the user is being assigned.
     * @return The created project member instance
     */
    suspend fun assignUserToProject(userId: UUID, projectId: UUID) = db.query {
        ProjectMemberTable.insertAndGet(ResultRow::toProjectMember, EntityType.PROJECT_MEMBER) {
            it[ProjectMemberTable.userId] = userId
            it[ProjectMemberTable.projectId] = projectId
            it[role] = ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        }
    }

    /**
     * Creates a user with the specified email and assigns the user to a project.
     *
     * @param email The email address of the user to be created.
     * @param projectId The unique identifier of the project to which the user will be assigned.
     */
    suspend fun createAndAssignUserToProject(email: String, projectId: UUID) = db.query {
        val userId = createExampleUser(email)
        assignUserToProject(userId, projectId)
    }

    /**
     * Creates an example paper in the database with the specified properties.
     */
    @Suppress("LongParameterList")
    suspend fun insertPaperAndGetId(
        title: String = "Title",
        externalId: String = UUID.randomUUID().toString(),
        abstract: String = "Abstract",
        year: Int = 2025,
        publisher: String = "Publisher",
        publicationType: String = "PublicationType",
        publicationName: String = "PublicationName",
        fetcherMetadata: Map<String, String> = emptyMap(),
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
        }.value
    }

    @Suppress("LongParameterList")
    suspend fun insertProjectAndGetId(
        name: String = "Test Project",
        status: ProjectStatus = ProjectStatus.PROJECT_STATUS_ACTIVE,
        currentStage: Long = 0,
        maxStage: Long = 0,
        similarityThreshold: Float = 0F,
        snowballingType: SnowballingType = SnowballingType.SNOWBALLING_TYPE_BOTH,
        reviewMaybeAllowed: Boolean = true,
        reviewDecisionMatrix: ReviewDecisionMatrix = ReviewDecisionMatrix.getDefaultInstance(),
        fetcherApis: Map<String, Map<String, String>> = emptyMap(),
        createdBy: UUID,
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
            }.value
    }

    @Suppress("LongParameterList")
    suspend fun insertProjectPaperAndGetId(
        paperId: UUID,
        projectId: UUID,
        localPaperId: Long = 0,
        stage: Long = 0,
        decision: ProjectOuterClass.PaperDecision = ProjectOuterClass.PaperDecision.PAPER_DECISION_ACCEPTED,
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

    @Suppress("LongParameterList")
    suspend fun insertReviewAndGetId(
        projectPaperId: UUID,
        userId: UUID,
        decision: ReviewOuterClass.ReviewDecision = ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED,
    ): UUID = db.query {
        ReviewTable.insertAndGetId {
            it[ReviewTable.projectPaperId] = projectPaperId
            it[ReviewTable.userId] = userId
            it[ReviewTable.decision] = decision
        }.value
    }

    @Suppress("LongParameterList")
    suspend fun insertCriterionAndGetId(
        tag: String = "Test Tag",
        name: String = "Test Criterion",
        description: String = "Test Description",
        category: CriterionCategory = CriterionCategory.CRITERION_CATEGORY_EXCLUSION,
        projectId: UUID,
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

    suspend fun assignCriterionToReview(reviewId: UUID, criterionId: UUID) = db.query {
        ReviewHasCriterionTable.insert {
            it[ReviewHasCriterionTable.reviewId] = reviewId
            it[ReviewHasCriterionTable.criterionId] = criterionId
        }
    }
}
