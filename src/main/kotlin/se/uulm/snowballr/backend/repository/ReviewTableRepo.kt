package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.insertAndGetId
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.invalidargument.CriteriaNotInProjectException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectPaperNotFoundException
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ReviewTable
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import se.uulm.snowballr.backend.table.toReview
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [Review].
 *
 * This interface is used to handle persistence and retrieval operations for reviews by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing reviews can remain decoupled from the specifics of the database layer.
 */
interface IReviewTableRepo {
    /**
     * Returns a [Result] containing the review by its ID or a [NotFoundException] if the review with the passed [id]
     * doesn't exist.
     */
    suspend fun getReviewById(id: UUID): Result<Review>

    /**
     * Retrieves all reviews associated with the specified project paper.
     *
     * @param projectId The unique identifier of the project the paper belongs to.
     * @param paperId The unique identifier of the paper whose reviews in the project are to be fetched.
     * @return A list of reviews associated with the given project paper.
     */
    suspend fun getAllReviewsForProjectPaper(projectId: UUID, paperId: UUID): List<Review>

    /**
     * Retrieves all reviews along with their selected criteria for the specified project paper.
     *
     * @param projectId The unique identifier of the project the paper belongs to.
     * @param paperId The unique identifier of the paper whose reviews with selected criteria in the project are to be
     * fetched.
     * @return A list of [ReviewWithSelectedCriteriaIds] associated with the given project paper.
     */
    suspend fun getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
        projectId: UUID,
        paperId: UUID,
    ): List<ReviewWithSelectedCriteriaIds>

    /**
     * Creates a new review in the database with the provided [request] data.
     *
     * Checking the selected criteria and inserting the review happen in one transaction, and the selected criteria
     * are locked against deletion until the review references them.
     *
     * @param request The review request containing the project and paper that were reviewed, the review decision and
     * the selected project criteria to justify the decision.
     * @param userId The ID of the user who created the review.
     * @return A [Result] containing the created [Review], a [ProjectPaperNotFoundException] if the paper is not part
     * of the project, or a [CriteriaNotInProjectException] if at least one selected criterion does not exist or does
     * not belong to the project of the passed [request].
     */
    suspend fun createReview(request: CreateReviewRequest, userId: UUID): Result<Review>
}

/**
 * Repository implementation for managing the [ReviewTable] in the database.
 *
 * This class handles the persistence and retrieval of review data by integrating
 * with the underlying database through the [IDatabase] interface. It provides
 * concrete methods for CRUD operations on review records within the database.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ReviewTableRepo(
    private val db: IDatabase,
) : IReviewTableRepo {
    /**
     * Reviews joined with their project paper, so that [toReview] can read the project and paper ID.
     */
    private val reviewWithProjectPaper = ReviewTable.join(
        ProjectPaperTable,
        JoinType.INNER,
        onColumn = ReviewTable.projectPaperId,
        otherColumn = ProjectPaperTable.id,
    )

    private fun isProjectPaper(projectId: UUID, paperId: UUID): Op<Boolean> =
        (ProjectPaperTable.projectId eq projectId) and (ProjectPaperTable.paperId eq paperId)

    private fun getReviewByIdOrNull(id: UUID): Review? = reviewWithProjectPaper
        .selectAll()
        .where { ReviewTable.id eq id }
        .singleOrNull()
        ?.toReview()

    override suspend fun getReviewById(id: UUID): Result<Review> = db.query {
        getEntityByKeyAsResult(::getReviewByIdOrNull, EntityType.REVIEW, id)
    }

    override suspend fun getAllReviewsForProjectPaper(projectId: UUID, paperId: UUID): List<Review> = db.query {
        reviewWithProjectPaper
            .selectAll()
            .where { isProjectPaper(projectId, paperId) }
            .map(ResultRow::toReview)
    }

    override suspend fun getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
        projectId: UUID,
        paperId: UUID,
    ): List<ReviewWithSelectedCriteriaIds> = db.query {
        reviewWithProjectPaper.join(
            ReviewHasCriterionTable,
            onColumn = ReviewTable.id,
            otherColumn = ReviewHasCriterionTable.reviewId,
            joinType = JoinType.LEFT,
        ).selectAll()
            .where { isProjectPaper(projectId, paperId) }
            .groupBy { it[ReviewTable.id].value }
            .map { (_, rows) ->
                val review = rows.first().toReview()
                val selectedCriteriaIds = rows.mapNotNull { row ->
                    row.getOrNull(ReviewHasCriterionTable.criterionId)?.value
                }
                ReviewWithSelectedCriteriaIds(review, selectedCriteriaIds)
            }
    }

    override suspend fun createReview(request: CreateReviewRequest, userId: UUID): Result<Review> = db.query {
        val (projectId, paperId) = request
        val projectPaperId = ProjectPaperTable
            .select(ProjectPaperTable.id)
            .where { isProjectPaper(projectId, paperId) }
            .singleOrNull()
            ?.get(ProjectPaperTable.id)
            ?: return@query Result.failure(ProjectPaperNotFoundException(paperId, projectId))

        val foreignCriteriaIds = getForeignCriteriaIdsAndLockOthers(request.selectedCriteriaIds, projectId)
        if (foreignCriteriaIds.isNotEmpty()) {
            return@query Result.failure(CriteriaNotInProjectException(foreignCriteriaIds, projectId))
        }

        val reviewId = ReviewTable.insertAndGetId {
            it[ReviewTable.projectPaperId] = projectPaperId
            it[ReviewTable.userId] = userId
            it[decision] = request.decision
        }.value

        ReviewHasCriterionTable.batchInsert(request.selectedCriteriaIds) { criterionId ->
            this[ReviewHasCriterionTable.reviewId] = reviewId
            this[ReviewHasCriterionTable.criterionId] = criterionId
        }

        Result.success(checkNotNull(getReviewByIdOrNull(reviewId)))
    }

    /**
     * Returns the IDs of [criteriaIds] that do not exist or do not belong to the project with the passed [projectId].
     * The remaining criteria are locked with `FOR KEY SHARE`, so they cannot be deleted before the transaction ends.
     */
    private fun getForeignCriteriaIdsAndLockOthers(criteriaIds: List<UUID>, projectId: UUID): List<UUID> {
        if (criteriaIds.isEmpty()) {
            return emptyList()
        }

        val projectCriteriaIds = CriterionTable
            .select(CriterionTable.id)
            .where { (CriterionTable.id inList criteriaIds) and (CriterionTable.projectId eq projectId) }
            .forUpdate(ForUpdateOption.PostgreSQL.ForKeyShare)
            .mapTo(HashSet()) { row -> row[CriterionTable.id].value }

        return criteriaIds.filterNot { id -> id in projectCriteriaIds }
    }
}
