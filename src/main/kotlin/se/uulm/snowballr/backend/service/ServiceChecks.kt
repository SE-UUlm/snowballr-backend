package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.repository.IUserTableRepo
import snowballr.UserOuterClass.UserRole
import java.util.UUID

/**
 * Verifies that the [user] has the role [UserRole.USER_ROLE_ADMIN].
 *
 * If the user is not a server admin, a [SnowballRException.UnauthorizedException] is thrown.
 *
 * @param user The user to verify
 * @param getException Getter method for the subtype of [SnowballRException.UnauthorizedException], which is thrown
 * when the user is not a server admin.
 */
fun verifyServerAdminRole(user: User, getException: (String) -> SnowballRException.UnauthorizedException) {
    if (user.role != UserRole.USER_ROLE_ADMIN) {
        throw getException(user.id.toString())
    }
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
    val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
    val requestedUser = userRepo.getUserById(requestedUserId).getOrThrow()

    if (currentUser.id != requestedUser.id) {
        verifyServerAdminRole(currentUser) {
            SnowballRException.UnauthorizedException.Single(
                EntityType.USER,
                requestedUserId.toString(),
                accessType,
                it,
            )
        }
    }
}
