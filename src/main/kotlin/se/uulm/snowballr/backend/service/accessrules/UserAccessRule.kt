@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Represents an [AccessRule] to a user entity.
 */
fun interface UserAccessRule : AccessRule<User>

/**
 * Check whether the requesting user and the target user are the same by checking
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
 * Check whether the requesting user is a server admin or the same user as the target user.
 */
val isServerAdminOrSameUser = isServerAdmin.forTarget<UUID>().orElse(isSameUserById)

/**
 * Check whether the requesting user is in the same project as the target user.
 *
 * @param projectMemberRepo The repository to check project membership.
 */
@CheckReturnValue
fun isInSameProject(projectMemberRepo: IProjectMemberTableRepo) = UUIDAccessRule { requester, targetId ->
    projectMemberRepo
        .getMembersInSameProjectsAsUser(targetId)
        .any { it.userId == requester.id }
}

/**
 * Check whether the current user is allowed to read the target user based on specific access control rules.
 *
 * @param projectMemberRepo The repository to check project membership.
 * @param identifierType The identifier type used to identify the target user (used for constructing the correct exception message, defaults to [IdentifierType.ID]).
 * @return An [AccessRule] that checks whether the current user has read permissions for a target user.
 */
@CheckReturnValue
fun isAllowedToReadUser(
    projectMemberRepo: IProjectMemberTableRepo,
    identifierType: IdentifierType = IdentifierType.ID,
): AccessRule<UUID> {
    return isServerAdminOrSameUser
        .orElse(isInSameProject(projectMemberRepo))
        .orElseThrow { currentUser, targetUserId ->
            UnauthorizedException.Single(
                EntityType.USER,
                targetUserId.toString(),
                AccessType.READ,
                currentUser.id.toString(),
                identifierType,
            )
        }
}

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [UnauthorizedException] is thrown.
 *
 * @param user The user to verify
 * @param getException Getter method for the subtype of [UnauthorizedException], which is thrown
 * when the user is not a server admin.
 */
suspend fun verifyServerAdminRole(user: User, getException: (String) -> UnauthorizedException) {
    isServerAdmin.forTarget<User>()
        .orElseThrow(getException(user.id.toString()))
        .checkFor(user, user)
}
