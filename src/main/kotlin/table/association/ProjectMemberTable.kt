package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.model.dto.ProjectMember
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.createdAt
import se.uulm.snowballr.backend.table.modifiedAt
import se.uulm.snowballr.backend.table.userReference
import snowballr.ProjectOuterClass
import java.time.OffsetDateTime

/**
 * Represents the "project_member" table, which defines the relationship between projects and their members.
 *
 * This table establishes an association between a project and a user, defining the user's role within the project.
 *
 * Columns:
 * - [projectId]: Foreign key referencing the [ProjectTable], representing the associated project.
 * - [userId]: Foreign key referencing the [UserTable], representing the associated user.
 * - [role]: Enumeration representing the member's role in the project, as defined in [ProjectOuterClass.MemberRole].
 * - [createdAt]: Represents the timestamp of when the project member was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the project member was last modified as an [OffsetDateTime].
 *
 * Primary Key:
 * - Composite primary key consisting of [projectId] and [userId].
 */
object ProjectMemberTable : CompositeIdTable("project_member") {
    /**
     * Reference to the associated project.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the user who is the member.
     *
     * - `onDelete=RESTRICT` so that no user can be deleted who is referenced by the entity
     * - `onUpdate=CASCADE` so that when the user ID is updated, the foreign key ID is updated too
     */
    val userId = userReference("user_id", ReferenceOption.RESTRICT, ReferenceOption.CASCADE)

    init {
        addIdColumn(projectId)
        addIdColumn(userId)
    }

    override val primaryKey = PrimaryKey(projectId, userId)

    val role = enumeration<ProjectOuterClass.MemberRole>("role")

    // Metadata

    val createdAt = createdAt()
    val modifiedAt = modifiedAt()

    // Methods

    /**
     * Creates a [ProjectMember] from this [ResultRow].
     */
    fun ResultRow.toProjectMember() =
        ProjectMember(
            id = this[id].value,
            projectId = this[projectId].value,
            userId = this[userId].value,
            role = this[role],
            createdAt = this[createdAt],
            modifiedAt = this[modifiedAt],
        )
}
