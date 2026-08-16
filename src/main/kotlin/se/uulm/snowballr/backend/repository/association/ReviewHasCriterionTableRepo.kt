package se.uulm.snowballr.backend.repository.association

import org.jetbrains.exposed.v1.core.eq
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.repository.getEntities
import se.uulm.snowballr.backend.table.association.ReviewHasCriterionTable
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [ReviewHasCriterionTable].
 *
 * This interface provides abstraction for handling persistence and retrieval operations for selected criteria of a review. By
 * using this interface, the functionality for managing selected criteria of a review can remain decoupled from the specifics of the database
 * layer.
 */
fun interface IReviewHasCriterionTableRepo {
    suspend fun getSelectedCriteriaIdsForReviewById(reviewId: UUID): List<UUID>
}

/**
 * Repository implementation for managing the [ReviewHasCriterionTable] in the database.
 *
 * This class provides functionality to handle persistence and retrieval operations for selected criteria of a review by
 * leveraging the database abstraction defined in [IDatabase]. It facilitates CRUD operations on selected criteria of a review and
 * ensures database transactions are handled properly.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
class ReviewHasCriterionTableRepo(
    private val db: IDatabase,
) : IReviewHasCriterionTableRepo {
    override suspend fun getSelectedCriteriaIdsForReviewById(reviewId: UUID): List<UUID> = db.query {
        ReviewHasCriterionTable.getEntities(mapper = { it[ReviewHasCriterionTable.criterionId].value }) {
            ReviewHasCriterionTable.reviewId eq reviewId
        }
    }
}
