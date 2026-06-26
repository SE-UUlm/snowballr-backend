package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import se.uulm.snowballr.backend.model.dto.ProjectPaper
import se.uulm.snowballr.backend.model.dto.ProjectPaperWithPaper
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.table.PaperTable
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.createdAt
import se.uulm.snowballr.backend.table.createdBy
import se.uulm.snowballr.backend.table.modifiedAt
import se.uulm.snowballr.backend.table.modifiedBy
import se.uulm.snowballr.backend.table.toPaper
import java.time.OffsetDateTime

/**
 * Represents the "project_paper" table, which defines an association between projects and papers in the database.
 * Each entry in this table links a specific paper with a specific project.
 *
 * Columns:
 * - [paperId]: Foreign key referencing the [PaperTable.id] column. Represents the paper.
 * - [projectId]: Foreign key referencing the [ProjectTable.id] column. Represents the project.
 * - [localPaperId]: Represents the unique identifier for the paper specific to the project as a [Long].
 * - [stage]: Represents the stage of the associated paper within the project as a [Long].
 * - [decision]: Represents the decision related to the paper, as per the [PaperDecision] enumeration.
 * - [createdAt]: Represents the timestamp of when the project paper was created as an [OffsetDateTime].
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the project paper.
 * - [modifiedAt]: Represents the timestamp of when the project paper was last modified as an [OffsetDateTime].
 * - [modifiedBy]: A foreign key referencing the user table, representing the user who last modified the project paper.
 *
 * Primary Key:
 * - Composite primary key consisting of [paperId] and [projectId].
 */
object ProjectPaperTable : UUIDTable("project_paper") {
    /**
     * Reference to the associated paper.
     *
     * - `onDelete=RESTRICT` so that no paper can be deleted who is referenced by the entity
     * - `onUpdate=CASCADE` so that when the paper ID is updated, the foreign key ID is updated too
     */
    val paperId = reference("paper_id", PaperTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    /**
     * Reference to the associated project.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    init {
        uniqueIndex(paperId, projectId)
    }

    val localPaperId = long("local_paper_id")
    val stage = long("stage")
    val decision = enumeration<PaperDecision>("decision")

    // Metadata

    val createdAt = createdAt()
    val createdBy = createdBy()
    val modifiedAt = modifiedAt()
    val modifiedBy = modifiedBy()
}

/**
 * Creates a [ProjectPaper] from this [ResultRow].
 */
fun ResultRow.toProjectPaper() = ProjectPaper(
    id = this[ProjectPaperTable.id].value,
    paperId = this[ProjectPaperTable.paperId].value,
    projectId = this[ProjectPaperTable.projectId].value,
    localPaperId = this[ProjectPaperTable.localPaperId],
    stage = this[ProjectPaperTable.stage],
    decision = this[ProjectPaperTable.decision],
    createdAt = this[ProjectPaperTable.createdAt],
    createdBy = this[ProjectPaperTable.createdBy].value,
    modifiedAt = this[ProjectPaperTable.modifiedAt],
    modifiedBy = this[ProjectPaperTable.modifiedBy]?.value,
)

/**
 * Creates a [ProjectPaperWithPaper] from this [ResultRow].
 */
fun ResultRow.toProjectPaperWithPaper() = ProjectPaperWithPaper(
    projectPaper = toProjectPaper(),
    paper = toPaper(),
)
