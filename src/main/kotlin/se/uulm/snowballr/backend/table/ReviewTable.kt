package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.table.association.ProjectPaperTable
import java.time.OffsetDateTime

/**
 * Represents the "review" table, defining the relationship between project papers and users, which is typically used to
 * associate users with reviews of specific project papers.
 *
 * Columns:
 * - [projectPaperId]: Foreign key referencing the [ProjectPaperTable], representing the project paper being reviewed.
 * - [userId]: Foreign key referencing the user, with cascade delete and nullify on update, representing the reviewer.
 * - [createdAt]: Represents the timestamp of when the review was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the review was last modified as an [OffsetDateTime].
 */
object ReviewTable : UUIDTable("review") {
    /**
     * Reference to the associated project paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the project paper is deleted
     * - `onUpdate=CASCADE` so that when the project paper ID is updated, the foreign key ID is updated too
     */
    val projectPaperId =
        reference("project_paper_id", ProjectPaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the reviewing user.
     *
     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by the entity
     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
     */
    val userId = userReference("user_id", ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    init {
        uniqueIndex(projectPaperId, userId)
    }

    val decision = enumeration<ReviewDecision>("decision")

    // Metadata

    val createdAt = createdAt()
    val modifiedAt = modifiedAt()
}

/**
 * Creates a [Review] from this [ResultRow].
 */
fun ResultRow.toReview() = Review(
    id = this[ReviewTable.id].value,
    projectPaperId = this[ReviewTable.projectPaperId].value,
    userId = this[ReviewTable.userId].value,
    decision = this[ReviewTable.decision],
    createdAt = this[ReviewTable.createdAt],
    modifiedAt = this[ReviewTable.modifiedAt],
)
