package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.createdAt
import java.time.OffsetDateTime

/**
 * Represents the "reading_list" table, defining the relationship between papers and users, typically used to
 * track papers associated with a specific user as part of a reading list.
 *
 * Columns:
 * - [paperId]: Foreign key referencing the [PaperTable]. Represents the paper included in the reading list.
 * - [userId]: Foreign key referencing the [UserTable]. Represents the user the reading list belongs to.
 * - [createdAt]: Timestamp indicating when the reading list entry was created as an [OffsetDateTime].
 *
 * Primary Key:
 * - Composite primary key consisting of [userId] and [paperId].
 */
object ReadingListTable : CompositeIdTable("reading_list") {
    /**
     * Reference to the associated paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the paper is deleted
     * - `onUpdate=CASCADE` so that when the paper ID is updated, the foreign key ID is updated too
     */
    val paperId = reference("paper_id", PaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the user who created the entity.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the user is deleted
     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
     */
    val userId = reference("user_id", UserTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    init {
        addIdColumn(paperId)
        addIdColumn(userId)
    }

    override val primaryKey = PrimaryKey(userId, paperId)

    // Metadata

    val createdAt = createdAt()
}
