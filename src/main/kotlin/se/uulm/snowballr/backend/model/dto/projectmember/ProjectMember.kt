package se.uulm.snowballr.backend.model.dto.projectmember

import se.uulm.snowballr.backend.table.association.ProjectMemberTable
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [ProjectMemberTable].
 */
data class ProjectMember(
    val projectId: UUID,
    val userId: UUID,
    val role: MemberRole,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
)

/**
 * Checks whether the project member is a project admin.
 *
 * A project member is considered a project admin if their role is set to [MemberRole.ADMIN].
 */
fun ProjectMember.isProjectAdmin() = this.role == MemberRole.ADMIN
