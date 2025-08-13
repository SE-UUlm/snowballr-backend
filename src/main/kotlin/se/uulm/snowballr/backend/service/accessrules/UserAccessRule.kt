@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

/**
 * Represents an [AccessRule] to a user entity.
 */
fun interface UserAccessRule : AccessRule<User>

/**
 * Represents an [AccessRule] to a user only identified by its UUID.
 */
fun interface UUIDAccessRule : AccessRule<UUID>

/**
 * Check whether the requesting user and the target user are the same, by checking
 * whether they have the same user id.
 */
val isSameUserById = UUIDAccessRule { requester, targetId -> requester.id == targetId }

/**
 * Check whether the target user is active, i.e., has the status `USER_STATUS_ACTIVE` or `USER_STATUS_ACTIVE_UNCONFIRMED`.
 */
val isTargetUserActive = UserAccessRule { _, target ->
    target.status == UserStatus.USER_STATUS_ACTIVE ||
        target.status == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
}

/**
 * Check whether the target user is *not* a server admin.
 */
val targetUserIsNotAdmin = UserAccessRule { _, target -> target.role != UserRole.USER_ROLE_ADMIN }

/**
 * Check whether the requesting user is in the same project as the target user.
 *
 * @param projectMemberRepo The repository to check project membership.
 */
fun isInSameProject(projectMemberRepo: IProjectMemberTableRepo) = UUIDAccessRule { requester, targetId ->
    projectMemberRepo
        .getMembersInSameProjectsAsUser(targetId)
        .any { it.userId == requester.id }
}

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException] is thrown.
 *
 * @param user The user to verify
 * @param getException Getter method for the subtype of [se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException], which is thrown
 * when the user is not a server admin.
 */
suspend fun verifyServerAdminRole(user: User, getException: (String) -> UnauthorizedException) {
    isServerAdmin.forTarget<User>()
        .orElseThrow(getException(user.id.toString()))
        .checkFor(user, user)
}

/**
 * Authorizes access to a user's resources based on the requesting user's identity and role.
 *
 * This method checks whether the requesting user is the same as the
 * target user identified by [requestedUserId]. If the users differ, the method verifies that the
 * requesting user has the server admin role. If neither condition is met, an
 * [SnowballRException.UnauthorizedException] is thrown.
 *
 * @param requestedUserId The ID of the user whose resource is being accessed.
 * @param userRepo The user repository used to retrieve user data.
 * @param accessType The type of access being attempted, used to construct the exception if unauthorized.
 */
suspend fun authorizeAccessTo(requestedUserId: UUID, userRepo: IUserTableRepo, accessType: AccessType) {
    val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
    val requestedUser = userRepo.getUserById(requestedUserId)

    isSameUserById
        .orElse(isServerAdmin.forTarget())
        .orElseThrow(
            UnauthorizedException.Single(
                EntityType.USER,
                requestedUserId.toString(),
                accessType,
                currentUser.getOrThrow().id.toString(),
            ),
        )
        .checkFor(
            currentUser.getOrThrow(),
            requestedUser.getOrThrow().id,
        )
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
        UnauthorizedException.Single(EntityType.PROJECT, projectId.toString(), AccessType.READ, it)
    }
}
