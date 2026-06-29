package se.uulm.snowballr.backend.model.dto.project

import snowballr.ProjectOuterClass

/**
 * Status of a project. Can be considered the stage in the lifecycle of a project.
 */
enum class ProjectStatus {
    /**
     * Project is active and fully editable, including settings.
     */
    ACTIVE,

    /**
     * Project is active, but SLR settings can no longer be changed.
     * This state is reached as soon as the first review has been submitted.
     */
    ACTIVE_LOCKED,

    /**
     * Project is read-only and no longer actively used.
     */
    ARCHIVED,

    /**
     * Project is marked for deletion.
     */
    DELETED,

    /**
     * Project data has been cleared after the project has been soft-deleted.
     */
    CLEARED,

    ;

    companion object {
        fun fromGrpc(status: ProjectOuterClass.ProjectStatus): ProjectStatus = when (status) {
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE -> ACTIVE
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED -> ARCHIVED
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED -> DELETED
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED -> ACTIVE_LOCKED
            ProjectOuterClass.ProjectStatus.UNRECOGNIZED,
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        ACTIVE -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
        ACTIVE_LOCKED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        ARCHIVED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED
        DELETED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED
        CLEARED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED
    }
}
