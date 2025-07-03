package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.User
import snowballr.UserOuterClass.UserRole

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [SnowballRException.UnauthorizedException.All] is thrown.
 *
 * @param user The user to verify
 * @param getException Getter method for the subtype of [SnowballRException.UnauthorizedException.All], which is thrown
 * when the user is not a server admin.
 */
fun verifyServerAdminRole(user: User, getException: (String) -> SnowballRException.UnauthorizedException.All) {
    if (user.role != UserRole.USER_ROLE_ADMIN) {
        throw getException(user.id.toString())
    }
}
