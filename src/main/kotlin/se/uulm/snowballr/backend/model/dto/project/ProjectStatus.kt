package se.uulm.snowballr.backend.model.dto.project

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
}
