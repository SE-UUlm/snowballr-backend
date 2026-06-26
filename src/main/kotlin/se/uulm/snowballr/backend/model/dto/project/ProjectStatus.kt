package se.uulm.snowballr.backend.model.dto.project

import snowballr.ProjectOuterClass

/**
 * Status of a project. Can be considered the stage in the lifecycle of a project.
 */
enum class ProjectStatus {
    /**
     * Project is active and fully editable, including settings.
     */
    PROJECT_STATUS_ACTIVE,

    /**
     * Project is active, but SLR settings can no longer be changed.
     * This state is reached as soon as the first review has been submitted.
     */
    PROJECT_STATUS_ACTIVE_LOCKED,

    /**
     * Project is read-only and no longer actively used.
     */
    PROJECT_STATUS_ARCHIVED,

    /**
     * Project is marked for deletion.
     */
    PROJECT_STATUS_DELETED,

    /**
     * Project data has been cleared after the project has been soft-deleted.
     */
    PROJECT_STATUS_CLEARED,

    ;

    companion object {
        fun fromGrpc(status: ProjectOuterClass.ProjectStatus): ProjectStatus = when (status) {
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE -> PROJECT_STATUS_ACTIVE
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED -> PROJECT_STATUS_ARCHIVED
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED -> PROJECT_STATUS_DELETED
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED -> PROJECT_STATUS_ACTIVE_LOCKED
            ProjectOuterClass.ProjectStatus.UNRECOGNIZED,
            ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED,
            ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        PROJECT_STATUS_ACTIVE -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
        PROJECT_STATUS_ACTIVE_LOCKED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
        PROJECT_STATUS_ARCHIVED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ARCHIVED
        PROJECT_STATUS_DELETED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_DELETED
        PROJECT_STATUS_CLEARED -> ProjectOuterClass.ProjectStatus.PROJECT_STATUS_UNSPECIFIED
    }
}
