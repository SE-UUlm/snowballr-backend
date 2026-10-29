package se.uulm.snowballr.backend.model.incoming.projectmember

import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole

data class UpdateMemberRoleRequest(
    val newRole: MemberRole,
)
