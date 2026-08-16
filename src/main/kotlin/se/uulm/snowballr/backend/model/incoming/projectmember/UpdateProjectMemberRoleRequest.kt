package se.uulm.snowballr.backend.model.incoming.projectmember

import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import java.util.UUID

data class UpdateProjectMemberRoleRequest(
    val projectId: UUID,
    val userId: UUID,
    val newRole: MemberRole,
)
