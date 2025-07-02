package se.uulm.snowballr.backend.table.association

import org.jetbrains.exposed.dao.id.CompositeIdTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import se.uulm.snowballr.backend.model.dto.Invitation
import se.uulm.snowballr.backend.table.ProjectTable
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.userReference
import java.time.Instant

/**
 * Represents the "invitation" table, defining the relationship between projects and invited users, along with the
 * associated metadata such as invitation tokens and expiration times.
 *
 * Columns:
 * - [projectId]: Foreign key referencing the [ProjectTable], representing the associated project.
 * - [userId]: Foreign key referencing the [UserTable], representing the invited user.
 * - [token]: A text field representing the invitation token as a [String].
 * - [validUntil]: A timestamp indicating the validity period of the invitation as an [Instant].
 *
 * Primary Key:
 * - Composite primary key consisting of [projectId] and [userId].
 */
object InvitationTable : CompositeIdTable("invitation") {
    /**
     * Reference to the associated project.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)

    /**
     * Reference to the invited user.
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

    val token = text("token")
    val validUntil = timestamp("valid_until")

    // Methods

    /**
     * Creates an [Invitation] from this [ResultRow].
     */
    fun ResultRow.toInvitation() = Invitation(
        id = this[id].toString(),
        projectId = this[projectId].value,
        userId = this[userId].value,
        token = this[token],
        validUntil = this[validUntil],
    )
}
