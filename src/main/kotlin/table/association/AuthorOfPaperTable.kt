package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import se.uulm.snowballr.backend.table.AuthorTable
import se.uulm.snowballr.backend.table.PaperTable

/**
 * Represents the "author_of_paper" table, defining the relationship between authors and papers in the database.
 *
 * This table is used to establish a many-to-many association between authors and their authored papers.
 *
 * Columns:
 * - [paperId]: Foreign key referencing the [PaperTable.id] column, representing the associated paper.
 * - [authorId]: Foreign key referencing the [AuthorTable.id] column, representing the associated author.
 *
 * Primary Key:
 * - Composite primary key consisting of [paperId] and [authorId].
 */
object AuthorOfPaperTable : CompositeIdTable("author_of_paper") {
    /**
     * Reference to the associated paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the paper is deleted
     * - `onUpdate=CASCADE` so that when the paper ID is updated, the foreign key ID is updated too
     */
    val paperId = reference("paper_id", PaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the author of the paper.
     *
     * - `onDelete=RESTRICT` so that no author can be deleted who is referenced by the entity
     * - `onUpdate=CASCADE` so that when the author ID is updated, the foreign key ID is updated too
     */
    val authorId = reference("author_id", AuthorTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    init {
        addIdColumn(paperId)
        addIdColumn(authorId)
    }

    override val primaryKey = PrimaryKey(paperId, authorId)
}
