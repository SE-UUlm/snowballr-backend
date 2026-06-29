package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import java.time.OffsetDateTime

/**
 * Represents the database table "criterion" and provides a mapping for managing criterion-related entities in the
 * database.
 *
 * Columns:
 * - [tag]: Represents the tag of the criterion as a [String].
 * - [name]: Represents the name of the criterion as a [String].
 * - [description]: Represents the description of the criterion as a [String].
 * - [category]: Represents the category of the criterion as an enumeration value from [CriterionCategory].
 * - [projectId]: A foreign key referencing the project table, because a criterion always belongs to a project.
 * - [createdAt]: Represents the timestamp of when the criterion was created as an [OffsetDateTime].
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the criterion.
 */
object CriterionTable : UUIDTable("criterion") {
    val tag = text("tag")
    val name = text("name")
    val description = text("description")
    val category = enumeration<CriterionCategory>("category")

    /**
     * Reference to the project, to which the criterion belongs to.
     *
     * - `onDelete=CASCADE` so that all criteria are deleted when the according project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = optReference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    // Metadata

    val createdAt = createdAt()
    val createdBy = createdBy()
}

/**
 * Creates a [Criterion] from this [ResultRow].
 */
fun ResultRow.toCriterion(): Criterion =
    if (this[CriterionTable.projectId] == null) this.toUserCriterion() else this.toProjectCriterion()

/**
 * Creates a [Criterion.UserCriterion] from this [ResultRow].
 */
fun ResultRow.toUserCriterion(): Criterion.UserCriterion {
    require(this[CriterionTable.projectId] == null) { "Project ID must be null for a user criterion" }
    return Criterion.UserCriterion(
        id = this[CriterionTable.id].value,
        tag = this[CriterionTable.tag],
        name = this[CriterionTable.name],
        description = this[CriterionTable.description],
        category = this[CriterionTable.category],
        createdAt = this[CriterionTable.createdAt],
        createdBy = this[CriterionTable.createdBy].value,
    )
}

/**
 * Creates a [Criterion.ProjectCriterion] from this [ResultRow].
 */
fun ResultRow.toProjectCriterion(): Criterion.ProjectCriterion {
    val projectId = this[CriterionTable.projectId]
    requireNotNull(projectId) { "Project ID must not be null for a project criterion" }
    return Criterion.ProjectCriterion(
        id = this[CriterionTable.id].value,
        tag = this[CriterionTable.tag],
        name = this[CriterionTable.name],
        description = this[CriterionTable.description],
        category = this[CriterionTable.category],
        projectId = projectId.value,
        createdAt = this[CriterionTable.createdAt],
        createdBy = this[CriterionTable.createdBy].value,
    )
}
