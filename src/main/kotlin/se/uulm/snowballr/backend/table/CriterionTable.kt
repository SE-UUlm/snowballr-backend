package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.model.dto.Criterion
import snowballr.CriterionOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "criterion" and provides a mapping for managing criterion-related entities in the
 * database.
 *
 * Columns:
 * - [tag]: Represents the tag of the criterion as a [String].
 * - [name]: Represents the name of the criterion as a [String].
 * - [description]: Represents the description of the criterion as a [String].
 * - [category]: Represents the category of the criterion as an enumeration value from
 * [CriterionOuterClass.CriterionCategory].
 * - [projectId]: A foreign key referencing the project table, because a criterion always belongs to a project.
 * - [createdAt]: Represents the timestamp of when the criterion was created as an [OffsetDateTime].
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the criterion.
 */
object CriterionTable : UUIDTable("criterion") {
    val tag = text("tag")
    val name = text("name")
    val description = text("description")
    val category = enumeration<CriterionOuterClass.CriterionCategory>("category")

    /**
     * Reference to the project, to which the criterion belongs to.
     *
     * - `onDelete=CASCADE` so that all criteria are deleted when the according project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    // Metadata

    val createdAt = createdAt()
    val createdBy = createdBy()

    // Methods

    /**
     * Creates a [Criterion] from this [ResultRow].
     */
    fun ResultRow.toCriterion() = Criterion(
        id = this[id].value,
        tag = this[tag],
        name = this[name],
        description = this[description],
        category = this[category],
        projectId = this[projectId].value,
        createdAt = this[createdAt],
        createdBy = this[createdBy].value,
    )
}
