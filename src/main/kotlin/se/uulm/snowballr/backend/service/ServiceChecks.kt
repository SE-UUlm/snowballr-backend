package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.User
import snowballr.UserOuterClass.UserRole

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [SnowballRException.UnauthorizedException.All] is thrown.
 *
 * @param user The user to verify
 * @param entityType The entity type of the accessed entity.
 */
fun verifyServerAdminRole(user: User, entityType: EntityType) {
    if (user.role != UserRole.USER_ROLE_ADMIN) {
        throw SnowballRException.UnauthorizedException.All(entityType, currentUserId = user.id.toString())
    }
}
