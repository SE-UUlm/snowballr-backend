package se.uulm.snowballr.backend.access.rules

import se.uulm.snowballr.backend.model.dto.user.User
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Check whether the requesting user is a server admin.
 */
@CheckReturnValue
fun isServerAdmin() = AccessRule<Unit> { requester, _ -> requester.isServerAdmin }

/**
 * Check whether the requesting user and the target user are the same by checking whether they have the same user ID.
 */
@CheckReturnValue
fun isSameUserById() = AccessRule<UUID> { requester, targetId -> requester.id == targetId }

/**
 * Check whether the requesting user is a server admin or the same user as the target user.
 */
@CheckReturnValue
fun isServerAdminOrSameUser() = isServerAdmin().forTarget<UUID>().orElse(isSameUserById())

/**
 * Check whether the target user is active (according to [User.isActive]).
 */
@CheckReturnValue
fun isTargetUserActive() = AccessRule<User> { _, target -> target.isActive }

/**
 * Check whether the requester is a server admin (according to [isServerAdmin]) **OR** the target user is active
 * (according to [isTargetUserActive]).
 */
@CheckReturnValue
fun isServerAdminOrTargetUserActive() = isServerAdmin().forTarget<User>().orElse(isTargetUserActive())

/**
 * Check whether the target user is not a server admin (according to [User.isServerAdmin]).
 */
@CheckReturnValue
fun isTargetUserNotAdmin() = AccessRule<User> { _, target -> !target.isServerAdmin }
