package se.uulm.snowballr.backend.access.rules

import se.uulm.snowballr.backend.model.dto.isServerAdmin
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Check whether the requesting user is a server admin.
 */
@CheckReturnValue
fun isServerAdmin() = AccessRule<Unit> { requester, _ -> requester.isServerAdmin() }

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
