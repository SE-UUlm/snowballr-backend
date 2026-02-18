package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.isActive
import se.uulm.snowballr.backend.model.dto.isServerAdmin
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.UserOuterClass.UserStatus
import java.util.UUID
import javax.annotation.CheckReturnValue

interface IUserAccessChecker {
    /**
     * Check whether the requesting user and the target user are the same by checking whether they have the same user id.
     */
    @CheckReturnValue
    fun isSameUserById(): AccessRule<UUID>

    /**
     * Check whether the target user is active, i.e., their status is set to [UserStatus.USER_STATUS_ACTIVE] or
     * [UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED].
     */
    @CheckReturnValue
    fun isTargetUserActive(): AccessRule<User>

    /**
     * Check whether the requesting user is a server admin or the target user is active.
     *
     * This allows server admins to access inactive users, while non-admins can only access active users.
     */
    @CheckReturnValue
    fun isServerAdminOrTargetUserActive(): AccessRule<User>

    /**
     * Check whether the target user is *not* a server admin.
     */
    @CheckReturnValue
    fun isTargetUserNotAdmin(): AccessRule<User>

    /**
     * Check whether the requesting user is a server admin or the same user as the target user.
     */
    @CheckReturnValue
    fun isServerAdminOrSameUser(): AccessRule<UUID>

    /**
     * Check whether the current user is allowed to read the target user based on specific access control rules.
     *
     * @param identifierType The identifier type used to identify the target user (used for constructing the correct
     * exception message, defaults to [IdentifierType.ID]).
     */
    @CheckReturnValue
    fun isAllowedToReadUser(identifierType: IdentifierType = IdentifierType.ID): AccessRule<UUID>
}

class UserAccessChecker(
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IUserAccessChecker {
    override fun isSameUserById() = AccessRule<UUID> { requester, targetId -> requester.id == targetId }

    override fun isTargetUserActive() = AccessRule<User> { _, target -> target.isActive() }

    override fun isServerAdminOrTargetUserActive() = isServerAdmin().forTarget<User>().orElse(isTargetUserActive())

    override fun isTargetUserNotAdmin() = AccessRule<User> { _, target -> !target.isServerAdmin() }

    override fun isServerAdminOrSameUser() = isServerAdmin().forTarget<UUID>().orElse(isSameUserById())

    override fun isAllowedToReadUser(identifierType: IdentifierType) = isServerAdminOrSameUser()
        .orElse(isInSameProject())
        .orElseThrow { currentUser, targetUserId ->
            UnauthorizedReadException(currentUser.id, targetUserId, EntityType.USER, identifierType)
        }

    /**
     * Check whether the requesting user is in the same project as the target user.
     */
    @CheckReturnValue
    private fun isInSameProject() = AccessRule<UUID> { requester, targetId ->
        projectMemberRepo.getMembersInSameProjectsAsUser(targetId).any { it.userId == requester.id }
    }
}
