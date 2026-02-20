package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.isProjectActive
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import java.util.UUID

fun interface IProjectPaperAccessChecker {
    /**
     * Checks whether the current user is allowed to add a paper to the specified project.
     *
     * Conditions:
     * - The user is a project admin of the project, **OR** a server admin.
     * - The project is active (not archived or deleted).
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project to which a paper is being added.
     * @param projectResult The result of fetching the project, used to check if the project is active.
     * @throws UnauthorizedCreateException if the user is not allowed to add a paper to the project.
     * @throws EntityNotActiveException if the project is not active.
     */
    suspend fun isAllowedToAddPaperToProject(currentUser: User, projectId: UUID, projectResult: Result<Project>)
}

class ProjectPaperAccessChecker(
    private val projectAccessChecker: IProjectAccessChecker,
) : IProjectPaperAccessChecker {
    override suspend fun isAllowedToAddPaperToProject(
        currentUser: User,
        projectId: UUID,
        projectResult: Result<Project>,
    ) {
        projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE).checkFor(currentUser, projectId)
        isProjectActive().checkFor(currentUser, projectResult.getOrThrow())
    }
}
