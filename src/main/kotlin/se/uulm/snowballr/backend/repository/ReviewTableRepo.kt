package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.table.ReviewTable
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
     * Returns a review by its ID or throws a [NotFoundException] if the review with the passed [id] doesn't exist.
     */
    suspend fun getReviewById(id: UUID): Review

    /**
     * Retrieves all reviews associated with the specified project paper.
     *
     * @param projectPaperId The unique identifier of the project paper whose reviews are to be fetched.
     * @return A list of reviews associated with the given project paper.
     */
    suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<Review>
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

    override suspend fun getReviewById(id: UUID): Review = db.query {
        getReviewByIdOrNull(id) ?: throw NotFoundException(EntityType.REVIEW, id.toString())
    }

    override suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<Review> = db.query {
        ReviewTable
            .selectAll()
            .where { ReviewTable.projectPaperId eq projectPaperId }
            .map { it.toReview() }
    }
}
