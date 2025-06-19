package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import se.uulm.snowballr.backend.table.PaperTable

/**
 * Represents the "citation" table that defines a bidirectional relationship between papers using composite keys.
 *
 * This table is designed to establish a many-to-many association between papers, where each citation entry connects
 * a paper to another paper that it cites.
 *
 * Columns:
 * - [paperId]: Foreign key referencing the [PaperTable] to identify the citing paper.
 * - [citedPaperId]: Foreign key referencing the [PaperTable] to identify the cited paper.
 *
 * Primary Key:
 * - Composite primary key consisting of [paperId] and [citedPaperId].
 */
object CitationTable : CompositeIdTable("citation") {
    /**
     * Reference to the associated paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the paper is deleted
     * - `onUpdate=CASCADE` so that when the paper ID is updated, the foreign key ID is updated too
     */
    val paperId = reference("paper_id", PaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the associated cited paper.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the cited paper is deleted
     * - `onUpdate=CASCADE` so that when the cited paper ID is updated, the foreign key ID is updated too
     */
    val citedPaperId = reference("cited_paper_id", PaperTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    init {
        addIdColumn(paperId)
        addIdColumn(citedPaperId)
    }

    override val primaryKey = PrimaryKey(paperId, citedPaperId)
}
