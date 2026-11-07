package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.v1.core.JoinType
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption
import org.jetbrains.exposed.v1.jdbc.batchInsert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewWithSelectedCriteriaIds
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.invalidargument.CriteriaNotInProjectException
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ReviewTable
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
     * @param projectPaperId The unique identifier of the project paper whose reviews are to be fetched.
     * @return A list of reviews associated with the given project paper.
     */
    suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<Review>

    /**
     * Retrieves all reviews along with their selected criteria for the specified project paper.
     *
     * @param projectPaperId The unique identifier of the project paper whose reviews with selected criteria are to be
     * fetched.
     * @return A list of [ReviewWithSelectedCriteriaIds] associated with the given project paper.
     */
    suspend fun getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
        projectPaperId: UUID,
    ): List<ReviewWithSelectedCriteriaIds>

    /**
     * Creates a new review in the database with the provided [request] data.
     *
     * Checking the selected criteria and inserting the review happen in one transaction, and the selected criteria
     * are locked against deletion until the review references them.
     *
     * @param request The review request containing the project paper that was reviewed, the review decision and
     * the selected project criteria to justify the decision.
     * @param projectId The ID of the project the reviewed project paper belongs to.
     * @param userId The ID of the user who created the review.
     * @return A [Result] containing the created [Review], or a [CriteriaNotInProjectException] if at least one
     * selected criterion does not exist or does not belong to the project with the passed [projectId].
     */
    suspend fun createReview(request: CreateReviewRequest, projectId: UUID, userId: UUID): Result<Review>
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
    private fun getReviewByIdOrNull(id: UUID): Review? = ReviewTable.getEntityByIdOrNull(id, ResultRow::toReview)

    override suspend fun getReviewById(id: UUID): Result<Review> = db.query {
        getEntityByKeyAsResult(::getReviewByIdOrNull, EntityType.REVIEW, id)
    }

    override suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<Review> = db.query {
        ReviewTable.getEntities(ResultRow::toReview) { ReviewTable.projectPaperId eq projectPaperId }
    }

    override suspend fun getAllReviewsWithSelectedCriteriaIdsForProjectPaper(
        projectPaperId: UUID,
    ): List<ReviewWithSelectedCriteriaIds> = db.query {
        ReviewTable.join(
            ReviewHasCriterionTable,
            onColumn = ReviewTable.id,
            otherColumn = ReviewHasCriterionTable.reviewId,
            joinType = JoinType.LEFT,
        ).selectAll()
            .where { ReviewTable.projectPaperId eq projectPaperId }
            .groupBy { it[ReviewTable.id].value }
            .map { (_, rows) ->
                val review = rows.first().toReview()
                val selectedCriteriaIds = rows.mapNotNull { row ->
                    row.getOrNull(ReviewHasCriterionTable.criterionId)?.value
                }
                ReviewWithSelectedCriteriaIds(review, selectedCriteriaIds)
            }
    }

    override suspend fun createReview(request: CreateReviewRequest, projectId: UUID, userId: UUID): Result<Review> =
        db.query {
            val foreignCriteriaIds = getForeignCriteriaIdsAndLockOthers(request.selectedCriteriaIds, projectId)
            if (foreignCriteriaIds.isNotEmpty()) {
                return@query Result.failure(CriteriaNotInProjectException(foreignCriteriaIds, projectId))
            }

            val review = ReviewTable.insertAndGet(ResultRow::toReview) {
                it[projectPaperId] = request.projectPaperId
                it[ReviewTable.userId] = userId
                it[decision] = request.decision
            }

            ReviewHasCriterionTable.batchInsert(request.selectedCriteriaIds) { criterionId ->
                this[ReviewHasCriterionTable.reviewId] = review.id
                this[ReviewHasCriterionTable.criterionId] = criterionId
            }

            Result.success(review)
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
