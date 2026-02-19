package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.isProjectActive
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import java.util.UUID

fun interface IInvitationAccessChecker {
    /**
     * Checks whether the current user is allowed to invite a user to a project.
     *
     * Conditions:
     * - The user is a project admin **OR** the user is a server admin
     * - The project is active
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project to which a user is invited.
     * @param projectResult The result of a project request.
     * @throws UnauthorizedException if the user is not allowed to invite a user to the project
     * @throws EntityNotActiveException if the project is not active
     */
    suspend fun isAllowedToInviteUserToProject(currentUser: User, projectId: UUID, projectResult: Result<Project>)
}

class InvitationAccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
) : IInvitationAccessChecker {
    override suspend fun isAllowedToInviteUserToProject(
        currentUser: User,
        projectId: UUID,
        projectResult: Result<Project>,
    ) {
        projectAccessChecker.isProjectOrServerAdmin(currentUser, projectId, AccessType.READ)
        isProjectActive().checkFor(currentUser, projectResult.getOrThrow())
    }
}
