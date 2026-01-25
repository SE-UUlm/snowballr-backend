@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.isServerAdmin
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserStatus
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Check whether the requesting user and the target user are the same by checking whether they have the same user id.
 */
@CheckReturnValue
fun isSameUserById() = AccessRule<UUID> { requester, targetId -> requester.id == targetId }

/**
 * Check whether the target user is active, i.e., has the status `USER_STATUS_ACTIVE` or `USER_STATUS_ACTIVE_UNCONFIRMED`.
 */
@CheckReturnValue
fun isTargetUserActive() = AccessRule<User> { _, target ->
    target.status == UserStatus.USER_STATUS_ACTIVE || target.status == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
}

/**
 * Check whether the target user is *not* a server admin.
 */
@CheckReturnValue
fun targetUserIsNotAdmin() = AccessRule<User> { _, target -> !target.isServerAdmin() }

/**
 * Check whether the requesting user is a server admin or the same user as the target user.
 */
@CheckReturnValue
fun isServerAdminOrSameUser() = isServerAdmin().forTarget<UUID>().orElse(isSameUserById())

/**
 * Check whether the requesting user is in the same project as the target user.
 *
 * @param projectMemberRepo The repository to check project membership.
 */
@CheckReturnValue
private fun isInSameProject(projectMemberRepo: IProjectMemberTableRepo) = AccessRule<UUID> { requester, targetId ->
    projectMemberRepo.getMembersInSameProjectsAsUser(targetId).any { it.userId == requester.id }
}

/**
 * Check whether the current user is allowed to read the target user based on specific access control rules.
 *
 * @param projectMemberRepo The repository to check project membership.
 * @param identifierType The identifier type used to identify the target user (used for constructing the correct
 * exception message, defaults to [IdentifierType.ID]).
 * @return An [AccessRule] that checks whether the current user has read permissions for a target user.
 */
@CheckReturnValue
fun isAllowedToReadUser(
    projectMemberRepo: IProjectMemberTableRepo,
    identifierType: IdentifierType = IdentifierType.ID,
) = isServerAdminOrSameUser()
    .orElse(isInSameProject(projectMemberRepo))
    .orElseThrow { currentUser, targetUserId ->
        UnauthorizedReadException(currentUser.id, targetUserId, EntityType.USER, identifierType)
    }
