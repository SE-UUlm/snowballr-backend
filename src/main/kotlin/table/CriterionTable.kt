package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import snowballr.CriterionOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "criterion" and provides a mapping for managing criterion-related entities in the
 * database.
 *
 * Columns:
 * - [tag]: Represents the tag of the criterion as text.
 * - [name]: Represents the name of the criterion as text.
 * - [description]: Represents the description of the criterion as text.
 * - [category]: Represents the category of the criterion as an enumeration value from
 * [CriterionOuterClass.CriterionCategory].
 * - [createdAt]: Represents the timestamp of when the criterion was created as a [OffsetDateTime].
 * - [projectId]: A foreign key referencing the project table, because a criterion always belongs to a project.
 * - [createdBy]: A foreign key referencing the user table, representing the user who created the criterion.
 */
object CriterionTable : UUIDTable("criterion") {
    val tag = text("tag")
    val name = text("name")
    val description = text("description")
    val category = enumeration("status", CriterionOuterClass.CriterionCategory::class)
//    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }

    /**
     * Reference to the project, to which the criterion belongs to.
     *
     * - `onDelete=CASCADE` so that all criteria are deleted when the according project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

//    /**
//     * Reference to the user who created the criterion.
//     *
//     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by a criterion
//     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
//     */
//    val createdBy = reference("create_by", UserTable, ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    /**
     * Creates a [CriterionOuterClass.Criterion] object from a database [ResultRow].
     */
    fun ResultRow.toCriterion(): CriterionOuterClass.Criterion =
        CriterionOuterClass.Criterion
            .newBuilder()
            .setId(this[id].value.toString())
            .setTag(this[tag])
            .setName(this[name])
            .setDescription(this[description])
            .setCategory(this[category])
            .build()
}
