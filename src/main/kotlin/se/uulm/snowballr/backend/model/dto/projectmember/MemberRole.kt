package se.uulm.snowballr.backend.model.dto.projectmember

import snowballr.ProjectOuterClass

/**
 * Role of a user inside a project.
 */
enum class MemberRole {
    /**
     * Default project member role. No elevated rights.
     */
    DEFAULT,

    /**
     * Admin project member role. Has elevated rights.
     */
    ADMIN,

    ;

    companion object {
        fun fromGrpc(role: ProjectOuterClass.MemberRole): MemberRole = when (role) {
            ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT -> DEFAULT
            ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN -> ADMIN
            ProjectOuterClass.MemberRole.UNRECOGNIZED, ProjectOuterClass.MemberRole.MEMBER_ROLE_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        DEFAULT -> ProjectOuterClass.MemberRole.MEMBER_ROLE_DEFAULT
        ADMIN -> ProjectOuterClass.MemberRole.MEMBER_ROLE_ADMIN
    }
}
