package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserRole
import java.util.UUID

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [UnauthorizedException] is thrown.
 *
 * @param user The user to verify
 * @param getException Getter method for the subtype of [UnauthorizedException], which is thrown
 * when the user is not a server admin.
 */
fun verifyServerAdminRole(user: User, getException: (String) -> UnauthorizedException) {
    if (user.role != UserRole.USER_ROLE_ADMIN) {
        throw getException(user.id.toString())
    }
}

/**
 * Authorizes access to a user's resources based on the requesting user's identity and role.
 *
 * This method checks whether the requesting user is the same as the target user identified by [targetUserId]. If the
 * users differ, the method verifies that the requesting user has the server admin role. If neither condition is met, an
 * [UnauthorizedException] is thrown.
 *
 * @param currentUser The user who is attempting to access the resource.
 * @param targetUserId The ID of the user whose resource is being accessed.
 * @param userRepo The user repository used to retrieve user data.
 * @param accessType The type of access being attempted, used to construct the exception if unauthorized.
 */
suspend fun authorizeAccessTo(currentUser: User, targetUserId: UUID, userRepo: IUserTableRepo, accessType: AccessType) {
    val targetUser = userRepo.getUserById(targetUserId).getOrThrow()

    if (currentUser.id != targetUser.id) {
        verifyServerAdminRole(currentUser) {
            UnauthorizedException.Single(EntityType.USER, targetUserId.toString(), accessType, it)
        }
    }
}

/**
 * Checks whether a user is a member of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve project member data.
 * @param projectId The unique identifier of the project.
 * @param currentUserId The unique identifier of the user to be checked for membership.
 * @return `true` if the user is a member of the project, `false` otherwise.
 */
suspend fun isProjectMember(projectMemberRepo: IProjectMemberTableRepo, projectId: UUID, currentUserId: UUID): Boolean {
    val projectMembers = projectMemberRepo.getProjectMembers(projectId)
    return projectMembers.any { it.userId == currentUserId }
}

/**
 * Ensures that the current user is a member of the specified project. If the user is not a project member,
 * verifies if the user has server admin privileges; otherwise, throws an [UnauthorizedException.Single].
 *
 * @param projectMemberRepo The repository used to access project membership data.
 * @param projectId The unique identifier of the project to check membership for.
 * @param currentUser The current user accessing the project.
 */
suspend fun ensureCurrentUserIsProjectMember(
    projectMemberRepo: IProjectMemberTableRepo,
    projectId: UUID,
    currentUser: User,
) {
    if (isProjectMember(projectMemberRepo, projectId, currentUser.id)) return

    verifyServerAdminRole(currentUser) {
        throw UnauthorizedException.Single(EntityType.PROJECT, projectId.toString(), AccessType.READ, it)
    }
}
