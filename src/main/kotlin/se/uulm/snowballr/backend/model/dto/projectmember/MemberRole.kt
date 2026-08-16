package se.uulm.snowballr.backend.model.dto.projectmember

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
}
