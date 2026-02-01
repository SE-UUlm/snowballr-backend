package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.v1.core.ReferenceOption
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.dao.id.java.UUIDTable
import se.uulm.snowballr.backend.model.dto.InvitationToken
import java.time.OffsetDateTime

/**
 * Represents the database table "invitation_token" and provides a mapping for managing verification tokens in the database.
 *
 * Columns:
 * - [email]: Represents the email address of the user to whom the invitation is sent as a [String].
 * - [projectId]: Foreign key referencing the project table, because an invitation always belongs to a project.
 * - [token]: Represents the verification token (nano id) as a [String]. The token is obfuscated for security purposes.
 * - [expiresAt]: Represents the expiration timestamp of the verification token as an [OffsetDateTime].
 */
object InvitationTokenTable : UUIDTable("invitation_token") {
    val email = text("email")

    /**
     * Reference to the associated project.
     *
     * - `onDelete=CASCADE` so that the entity is deleted when the project is deleted
     * - `onUpdate=CASCADE` so that when the project ID is updated, the foreign key ID is updated too
     */
    val projectId = reference("project_id", ProjectTable, ReferenceOption.CASCADE, ReferenceOption.CASCADE)
    val token = obfuscatedText("token").uniqueIndex()
    val expiresAt = expiresAt()

    init {
        uniqueIndex(email, projectId)
    }
}

/**
 * Creates an [InvitationToken] from this [ResultRow].
 */
fun ResultRow.toInvitationToken() = InvitationToken(
    id = this[InvitationTokenTable.id].value,
    email = this[InvitationTokenTable.email],
    projectId = this[InvitationTokenTable.projectId].value,
    token = this[InvitationTokenTable.token],
    expiresAt = this[InvitationTokenTable.expiresAt],
)
