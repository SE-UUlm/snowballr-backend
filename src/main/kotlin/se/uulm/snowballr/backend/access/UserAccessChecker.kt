package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.access.rules.andAlso
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.forProperty
import se.uulm.snowballr.backend.access.rules.forTarget
import se.uulm.snowballr.backend.access.rules.isSameUserById
import se.uulm.snowballr.backend.access.rules.isServerAdmin
import se.uulm.snowballr.backend.access.rules.isServerAdminOrSameUser
import se.uulm.snowballr.backend.access.rules.orElse
import se.uulm.snowballr.backend.access.rules.orElseThrow
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.isActive
import se.uulm.snowballr.backend.model.dto.isServerAdmin
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

interface IUserAccessChecker {
    /**
     * Checks whether the current user is allowed to read another user.
     *
     * Conditions:
     * - The user is a server admin, **OR** the same user, **OR** in the same project as the other user
     * - The user is a server admin, **OR** the target user is active
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param targetUser The user that is being read.
     * @param identifierType The type used to identify the other user.
     * @throws UnauthorizedReadException if the user is not allowed to read the target user.
     * @throws UserNotFoundException if the user doesn't exist and [identifierType] is [UserIdentifierType.ID]
     * @throws UserNotFoundByEmailException if the user doesn't exist and [identifierType] is [UserIdentifierType.EMAIL]
     */
    suspend fun isAllowedToReadUser(currentUser: User, targetUser: User, identifierType: UserIdentifierType)

    /**
     * Checks whether the current user is allowed to read all users.
     *
     * Even if the check fails, the user may still be allowed to read specific users. This check is about reading all
     * stored users.
     *
     * Conditions:
     * - The user is a server admin
     *
     * @throws UnauthorizedReadAllException if the user is not allowed to read all users.
     */
    suspend fun isAllowedToReadAllUsers(currentUser: User)

    /**
     * Checks whether the current user is allowed to update another user.
     *
     * **Note:** For performing an access check for a user role update use [isAllowedToUpdateUserRole].
     *
     * Conditions:
     * - The user is the same user **OR** the user is a server admin **AND** the target user is active
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param targetUser The user that is being updated.
     * @throws EntityNotActiveException if the target user is not active.
     * @throws UnauthorizedUpdateException if the user is not allowed to update the target user.
     */
    suspend fun isAllowedToUpdateUser(currentUser: User, targetUser: User)

    /**
     * Checks whether the current user is allowed to update the role of another user.
     *
     * Conditions:
     * - The user is a server admin
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param targetUserId The ID of the user that is being updated
     * @throws UnauthorizedUpdateException if the user is not allowed to update the role of the target user
     */
    suspend fun isAllowedToUpdateUserRole(currentUser: User, targetUserId: UUID)

    /**
     * Checks whether the current user is allowed to delete another user.
     *
     * Conditions:
     * - The user is a server admin **OR** the same user
     * - The target user is not a server admin **OR** it's the same user
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param targetUser The user that is being deleted.
     * @throws UnauthorizedReadException if the user is not allowed to delete the target user.
     * @throws FailedPreconditionException if the target user is a server admin.
     */
    suspend fun isAllowedToDeleteUser(currentUser: User, targetUser: User)
}

class UserAccessChecker(
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IUserAccessChecker {
    override suspend fun isAllowedToReadUser(currentUser: User, targetUser: User, identifierType: UserIdentifierType) {
        isAllowedToReadUser(identifierType)
            .forProperty(User::id)
            .andAlso(isServerAdminOrTargetUserActive())
            .orElseThrow { _, _ ->
                when (identifierType) {
                    UserIdentifierType.ID -> UserNotFoundException(targetUser.id)
                    UserIdentifierType.EMAIL -> UserNotFoundByEmailException(targetUser.email)
                }
            }
            .checkFor(currentUser, targetUser)
    }

    override suspend fun isAllowedToReadAllUsers(currentUser: User) {
        isServerAdmin()
            .orElseThrow(UnauthorizedReadAllException(currentUser.id, EntityType.USER))
            .checkFor(currentUser)
    }

    override suspend fun isAllowedToUpdateUser(currentUser: User, targetUser: User) {
        isSameUserById()
            .forProperty(User::id)
            .orElse(
                isServerAdmin().forTarget<User>()
                    .andAlso(
                        isTargetUserActive()
                            .orElseThrow(EntityNotActiveException(EntityType.USER, targetUser.id)),
                    ),
            )
            .orElseThrow(UnauthorizedUpdateException(currentUser.id, targetUser.id, EntityType.USER))
            .checkFor(currentUser, targetUser)
    }

    override suspend fun isAllowedToUpdateUserRole(currentUser: User, targetUserId: UUID) {
        isServerAdmin().forTarget<UUID>()
            .orElseThrow(UnauthorizedUpdateException(currentUser.id, targetUserId, EntityType.USER))
            .checkFor(currentUser, targetUserId)
    }

    override suspend fun isAllowedToDeleteUser(currentUser: User, targetUser: User) {
        isServerAdminOrSameUser()
            .orElseThrow(UnauthorizedReadException(currentUser.id, targetUser.id, EntityType.USER))
            .forProperty(User::id)
            .andAlso(
                isTargetUserNotAdmin()
                    .orElse(isSameUserById().forProperty(User::id))
                    .orElseThrow(
                        FailedPreconditionException(
                            "The user with the id ${targetUser.id} can not be deleted because the user is an admin.",
                        ),
                    ),
            )
            .checkFor(currentUser, targetUser)
    }

    @CheckReturnValue
    private fun isTargetUserActive() = AccessRule<User> { _, target -> target.isActive() }

    @CheckReturnValue
    private fun isServerAdminOrTargetUserActive() = isServerAdmin().forTarget<User>().orElse(isTargetUserActive())

    @CheckReturnValue
    private fun isTargetUserNotAdmin() = AccessRule<User> { _, target -> !target.isServerAdmin() }

    @CheckReturnValue
    private fun isAllowedToReadUser(identifierType: UserIdentifierType) = isServerAdminOrSameUser()
        .orElse(isInSameProject())
        .orElseThrow { currentUser, targetUserId ->
            UnauthorizedReadException(currentUser.id, targetUserId, EntityType.USER, identifierType.toIdentifierType())
        }

    /**
     * Check whether the requesting user is in the same project as the target user.
     */
    @CheckReturnValue
    private fun isInSameProject() = AccessRule<UUID> { requester, targetId ->
        projectMemberRepo.getMembersInSameProjectsAsUser(targetId).any { it.userId == requester.id }
    }
}
