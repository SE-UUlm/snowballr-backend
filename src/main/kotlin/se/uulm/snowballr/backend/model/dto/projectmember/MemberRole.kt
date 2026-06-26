package se.uulm.snowballr.backend.model.dto.projectmember

import snowballr.ProjectOuterClass

/**
 * Role of a user inside a project.
 */
enum class MemberRole {
    /**
     * Default project member role. No elevated rights.
     */
    MEMBER_ROLE_DEFAULT,

    /**
     * Admin project member role. Has elevated rights.
     */
    MEMBER_ROLE_ADMIN,

    ;

    companion object {
        fun fromGrpc(role: ProjectOuterClass.MemberRole): MemberRole = when (role) {
            ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT -> MEMBER_ROLE_DEFAULT
            ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN -> MEMBER_ROLE_ADMIN
            ProjectOuterClass.MemberRole.UNRECOGNIZED, ProjectOuterClass.MemberRole.MEMBER_ROLE_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        MEMBER_ROLE_DEFAULT -> ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        MEMBER_ROLE_ADMIN -> ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN
    }
}
