package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.AccessRuleCompoundUUID
import se.uulm.snowballr.backend.access.rules.andAlso
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.forProperty
import se.uulm.snowballr.backend.access.rules.isSameUserById
import se.uulm.snowballr.backend.access.rules.orElse
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedDeleteException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import java.util.UUID

interface IProjectMemberAccessChecker {
    /**
     * Checks whether the current user is allowed to update the role of a project member.
     *
     * Conditions:
     * - The user is a project admin of the project, **OR** a server admin.
     * - The project exists.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project in which the member role is being updated.
     * @throws UnauthorizedUpdateException if the user is not allowed to update the member role.
     * @throws ProjectNotFoundException if the project does not exist.
     */
    suspend fun isAllowedToUpdateMemberRole(currentUser: User, projectId: UUID)

    /**
     * Checks whether the current user is allowed to remove a project member.
     *
     * Conditions:
     * - The user is the same as the member being removed, **OR** the user is a project admin of the project, **OR** a
     * server admin.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param memberUserId The ID of the project member that should be removed.
     * @param projectId The ID of the project from which the member should be removed.
     * @throws UnauthorizedDeleteException if the user is not allowed to remove the member.
     */
    suspend fun isAllowedToRemoveMember(currentUser: User, memberUserId: UUID, projectId: UUID)

    /**
     * Checks whether the current user is allowed to remove a project invitation.
     *
     * Conditions:
     * - The user is a project admin of the project, **OR** a server admin.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project from which the invitation should be removed.
     * @throws UnauthorizedDeleteException if the user is not allowed to remove the invitation.
     */
    suspend fun isAllowedToRemoveInvitation(currentUser: User, projectId: UUID)
}

class ProjectMemberAccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
) : IProjectMemberAccessChecker {
    override suspend fun isAllowedToUpdateMemberRole(currentUser: User, projectId: UUID) {
        projectAccessChecker.isProjectOrServerAdmin(AccessType.UPDATE)
            .andAlso(projectAccessChecker.isProjectExistent())
            .checkFor(currentUser, projectId)
    }

    override suspend fun isAllowedToRemoveMember(currentUser: User, memberUserId: UUID, projectId: UUID) {
        val userProjectCompound = AccessRuleCompoundUUID(memberUserId, projectId)

        isSameUserById()
            .forProperty(AccessRuleCompoundUUID::firstTarget)
            .orElse(
                projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE)
                    .forProperty(AccessRuleCompoundUUID::secondTarget),
            )
            .checkFor(currentUser, userProjectCompound)
    }

    override suspend fun isAllowedToRemoveInvitation(currentUser: User, projectId: UUID) {
        projectAccessChecker.isProjectOrServerAdmin(AccessType.DELETE).checkFor(currentUser, projectId)
    }
}
