package se.uulm.snowballr.backend.model.dto.user

import se.uulm.snowballr.backend.model.dto.project.ProjectSettings
import java.util.UUID

/**
 * Settings DTO of a [User].
 */
data class UserSettings(
    /**
     * Whether hotkeys/shortcuts are shown in the frontend GUI.
     */
    val areHotkeysShown: Boolean,
    /**
     * Whether the review mode is enabled in the frontend GUI.
     *
     * If this setting is enabled, the user cannot see the reviews of other project members in order to not get biased.
     */
    val isReviewModeEnabled: Boolean,
    /**
     * The IDs of user criteria that are copied to a new project on creation as project criteria.
     */
    val criteriaIds: List<UUID>,
    /**
     * The default [ProjectSettings] that are copied to a new project on creation.
     */
    val defaultProjectSettings: ProjectSettings,
)
