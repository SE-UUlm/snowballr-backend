package se.uulm.snowballr.backend.model.dto.projectmember

import se.uulm.snowballr.backend.model.dto.user.User

/**
 * Represents a data transfer object that combines information about a project member
 * and the associated user.
 *
 * This class encapsulates the relationship between a project member's attributes
 * and the user details, making it useful for cases where both sets of information
 * are required together.
 *
 * @property projectMember The project member's details, including their role and
 * association with the project.
 * @property user The user information associated with the project member, including
 * personal details and account status.
 */
data class ProjectMemberWithUser(
    val projectMember: ProjectMember,
    val user: User,
)
