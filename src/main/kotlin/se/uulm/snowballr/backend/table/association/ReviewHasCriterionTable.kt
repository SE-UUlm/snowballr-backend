package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import se.uulm.snowballr.backend.table.CriterionTable
import se.uulm.snowballr.backend.table.ReviewTable

/**
 * Represents the "review_has_criterion" table, defining the many-to-many relationship between reviews and criteria.
 *
 * This table is used to associate specific review entries with the criteria they are evaluated against.
 *
 * Columns:
 * - [reviewId]: Foreign key referencing the [ReviewTable.id], representing the associated review.
 * - [criterionId]: Foreign key referencing the [CriterionTable], representing the associated criterion.
 *
 * Primary Key:
 * - Composite primary key consisting of [reviewId] and [criterionId].
 */
object ReviewHasCriterionTable : CompositeIdTable("review_has_criterion") {
    /**
     * Reference to the associated review.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the review is deleted
     * - `onUpdate=CASCADE` so that when the review ID is updated, the foreign key ID is updated too
     */
    val reviewId = reference("review_id", ReviewTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the associated criterion.
     *
     * - `onDelete=RESTRICT` so that a criterion cannot be deleted if an associated entity exists. Note that this also
     *   applies when the criterion is deleted via the project cascade, so these associations must be deleted before
     *   the project is hard-deleted (see `ProjectTableRepo.attemptToDeleteProject`).
     * - `onUpdate=CASCADE` so that when the criterion ID is updated, the foreign key ID is updated too
     */
    val criterionId = reference("criterion_id", CriterionTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    init {
        addIdColumn(reviewId)
        addIdColumn(criterionId)
    }

    override val primaryKey = PrimaryKey(reviewId, criterionId)
}
