package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID

interface ICriterionAccessChecker {
    /**
     * Checks whether the current user is allowed to create a project criterion.
     *
     * Conditions:
     * - The user is a project member of the project the criterion is being created in, **OR** a server admin.
     * - The project is active (not archived or deleted).
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project the criterion is being created in.
     * @throws UnauthorizedCreateException if the user is not allowed to create a project criterion.
     * @throws EntityNotActiveException if the project is not active.
     */
    suspend fun isAllowedToCreateProjectCriterion(currentUser: User, projectId: UUID)

    /**
     * Checks whether the current user is allowed to read the target criterion.
     *
     * Conditions:
     * - The criterion is a user criterion created by the current user, **OR** a project criterion created in a project
     * the user is a member of, **OR** the user is a server admin.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param criterion The criterion that is being accessed.
     * @throws UnauthorizedReadException if the user is not allowed to read the criterion.
     */
    suspend fun isAllowedToReadCriterion(currentUser: User, criterion: Criterion)

    /**
     * Checks whether the current user is allowed to update the target criterion.
     *
     * Conditions:
     * - The criterion is a user criterion created by the current user, **OR** a project criterion created in a project
     * the user is a project admin of, **OR** the user is a server admin.
     * - Additionally, if the criterion is a project criterion, the project must be active (not archived or deleted).
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param criterion The criterion that is being accessed.
     * @throws UnauthorizedUpdateException if the user is not allowed to update the criterion.
     * @throws EntityNotActiveException if the criterion is a project criterion and the project is not active.
     */
    suspend fun isAllowedToUpdateCriterion(currentUser: User, criterion: Criterion)
}

class CriterionAccessChecker(
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectAccessChecker: IProjectAccessChecker,
) : ICriterionAccessChecker {
    override suspend fun isAllowedToCreateProjectCriterion(currentUser: User, projectId: UUID) {
        projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
            .checkFor(currentUser, projectId)
        projectAccessChecker.isProjectActiveById().checkFor(currentUser, projectId)
    }

    override suspend fun isAllowedToReadCriterion(currentUser: User, criterion: Criterion) {
        isCreatorOfCriterion()
            .orElse(isUserInProjectOfCriterion())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedReadException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)
    }

    override suspend fun isAllowedToUpdateCriterion(currentUser: User, criterion: Criterion) {
        isCreatorOfCriterion()
            .orElse(isUserAdminInProjectOfCriterion())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedUpdateException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)

        if (criterion is ProjectCriterion) {
            projectAccessChecker.isProjectActiveById().checkFor(currentUser, criterion.projectId)
        }
    }

    private fun isCreatorOfCriterion() = AccessRule<Criterion> { currentUser, criterion ->
        criterion is UserCriterion && currentUser.id == criterion.createdBy
    }

    private fun isUserInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> isProjectMember(target.projectId, requester.id)
        }
    }

    private fun isUserAdminInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> isProjectAdmin(target.projectId, requester.id)
        }
    }

    private suspend fun isProjectMember(projectId: UUID, userId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getProjectMembers(projectId)
        return projectMembers.any { it.userId == userId }
    }

    private suspend fun isProjectAdmin(projectId: UUID, userId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getAllProjectAdmins(projectId)
        return projectMembers.any { it.userId == userId }
    }
}
