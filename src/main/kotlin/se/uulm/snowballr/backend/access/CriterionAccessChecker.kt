package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.forTarget
import se.uulm.snowballr.backend.access.rules.isServerAdmin
import se.uulm.snowballr.backend.access.rules.orElse
import se.uulm.snowballr.backend.access.rules.orElseThrow
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.criterion.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedExceptionFactory
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID

interface ICriterionAccessChecker {
    /**
     * Checks whether the current user is allowed to create a criterion.
     *
     * Conditions for project criteria:
     * - The user is a project admin of the project the criterion is being created in, **OR** a server admin.
     * - The project is active (not archived or deleted), but settings are not locked.
     *
     * Conditions for user criteria:
     * - none
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param request The request to create a criterion.
     * @throws UnauthorizedCreateException if the user is not allowed to create a criterion.
     * @throws EntityNotActiveException if the project (case of project criterion) is not active.
     * @throws FailedPreconditionException if the project (case of project criterion) is active, but the settings are
     * locked.
     */
    suspend fun isAllowedToCreateCriterion(currentUser: User, request: CreateCriterionRequest)

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
     * - Additionally, if the criterion is a project criterion, the project must be active (not archived or deleted)
     * and the settings must not be locked.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param criterion The criterion that is being accessed.
     * @throws UnauthorizedUpdateException if the user is not allowed to update the criterion.
     * @throws EntityNotActiveException if the project (case of project criterion) is not active.
     * @throws FailedPreconditionException if the project (case of project criterion) is active, but the settings are
     * locked.
     */
    suspend fun isAllowedToUpdateCriterion(currentUser: User, criterion: Criterion)

    /**
     * Checks whether the current user is allowed to update the target criterion.
     *
     * Conditions:
     * - The criterion is a user criterion created by the current user, **OR** a project criterion created in a project
     * the user is a project admin of, **OR** the user is a server admin.
     * - Additionally, if the criterion is a project criterion, the project must be active (not archived or deleted)
     * and the settings must not be locked.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param criterion The criterion that is being accessed.
     * @throws UnauthorizedUpdateException if the user is not allowed to update the criterion.
     * @throws EntityNotActiveException if the project (case of project criterion) is not active.
     * @throws FailedPreconditionException if the project (case of project criterion) is active, but the settings are
     * locked.
     */
    suspend fun isAllowedToDeleteCriterion(currentUser: User, criterion: Criterion)

    /**
     * Checks whether the current user is allowed to use the target criterion as a default criterion in their settings.
     *
     * Conditions:
     * - The criterion is a user criterion created by the current user. Server admins are not exempt, since the default
     * criteria are copied into every project the user creates.
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param criterion The criterion that should be used as a default criterion.
     * @throws UnauthorizedReadException if the criterion is not a user criterion of the current user.
     */
    suspend fun isAllowedToUseAsDefaultCriterion(currentUser: User, criterion: Criterion)
}

class CriterionAccessChecker(
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectAccessChecker: IProjectAccessChecker,
) : ICriterionAccessChecker {
    override suspend fun isAllowedToCreateCriterion(currentUser: User, request: CreateCriterionRequest) {
        if (request.projectId == null) return

        projectAccessChecker.isProjectOrServerAdmin(AccessType.CREATE, EntityType.CRITERION)
            .checkFor(currentUser, request.projectId)
        projectAccessChecker.isProjectActiveButNotLockedById().checkFor(currentUser, request.projectId)
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
        isAllowedToUpdateOrDeleteCriterion(currentUser, criterion, AccessType.UPDATE)
    }

    override suspend fun isAllowedToDeleteCriterion(currentUser: User, criterion: Criterion) {
        isAllowedToUpdateOrDeleteCriterion(currentUser, criterion, AccessType.DELETE)
    }

    override suspend fun isAllowedToUseAsDefaultCriterion(currentUser: User, criterion: Criterion) {
        isCreatorOfCriterion()
            .orElseThrow { user, target ->
                UnauthorizedReadException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)
    }

    private fun isCreatorOfCriterion() = AccessRule<Criterion> { currentUser, criterion ->
        criterion is UserCriterion && currentUser.id == criterion.createdBy
    }

    private fun isUserInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> projectMemberRepo.isProjectMember(target.projectId, requester.id)
        }
    }

    private fun isUserAdminInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> isProjectAdmin(target.projectId, requester.id)
        }
    }

    private suspend fun isProjectAdmin(projectId: UUID, userId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getAllProjectAdmins(projectId)
        return projectMembers.any { it.userId == userId }
    }

    private suspend fun isAllowedToUpdateOrDeleteCriterion(
        currentUser: User,
        criterion: Criterion,
        accessType: AccessType,
    ) {
        isCreatorOfCriterion()
            .orElse(isUserAdminInProjectOfCriterion())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedExceptionFactory.createForAccessType(accessType, user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)

        if (criterion is ProjectCriterion) {
            projectAccessChecker.isProjectActiveButNotLockedById().checkFor(currentUser, criterion.projectId)
        }
    }
}
