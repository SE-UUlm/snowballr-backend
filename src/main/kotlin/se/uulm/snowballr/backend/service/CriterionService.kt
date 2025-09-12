package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.Criterion.UserCriterion
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcCriteria
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Base
import snowballr.ProjectOuterClass
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

interface ICriterionService {
    /**
     * Service implementation of [SnowballRService.getCriterionById].
     */
    suspend fun getCriterionById(request: Base.Id): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.createCriterion].
     */
    suspend fun createCriterion(request: GrpcCriterion.Create): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.updateCriterion].
     *
     * @param request The update request containing the criterion details to be modified.
     * @return The updated criterion after the changes have been applied.
     */
    suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.getAllCriteriaForProject].
     */
    suspend fun getAllCriteriaForProject(request: Base.Id): GrpcCriterion.List
}

/**
 * The [CriterionService] class handles operations related to projects by implementing the [ICriterionService] interface.
 *
 * The `CriterionService` class provides functionality for managing criteria, including
 * creating, retrieving, and updating them. It also handles validation of access permissions,
 * preconditions, and interactions with underlying repositories.
 *
 * This service ensures that all operations related to criteria are performed
 * in accordance with the project and user access rules.
 *
 * @param repo Interface for persistence and retrieval operations related to criteria.
 * @param userRepo Interface for operations related to user management.
 * @param projectRepo Interface for operations related to project retrieval.
 * @param projectMemberRepo Interface for operations related to project member management.
 */
class CriterionService(
    private val repo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : ICriterionService {
    /**
     * Checks if the given user has the required permissions to access a project criterion.
     *
     * This function verifies if the user has permission to access a criterion
     * that belongs to an active project and throws exceptions when access is unauthorized
     * or when attempting to access a criterion of an inactive project.
     *
     * @param criterion The [ProjectCriterion] to check the permission for or null, if it does not already exist.
     * @param projectId The projectId of the [ProjectOuterClass.Project] to check the permission for.
     * @param currentUser The user whose permissions are being validated.
     * @param accessType The type of access being requested (e.g., READ, UPDATE).
     *
     * @throws SnowballRException.UnauthorizedException If the user does not have access permissions
     *         to the criterion.
     * @throws SnowballRException.FailedPreconditionException If attempting to access a criterion
     *         in a project that is not active.
     */
    private suspend fun checkProjectCriterionPermission(
        criterion: ProjectCriterion?,
        projectId: UUID,
        currentUser: User,
        accessType: AccessType,
    ) {
        val project = projectRepo.getProjectById(projectId).getOrThrow()
        val members = when (accessType) {
            AccessType.READ -> projectMemberRepo.getProjectMembers(project.id)
            AccessType.CREATE, AccessType.UPDATE -> projectMemberRepo.getAllProjectAdmins(project.id)
            AccessType.DELETE -> emptyList()
        }

        if (!members.any { it.userId == currentUser.id }) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        if ((accessType == AccessType.UPDATE || accessType == AccessType.CREATE) &&
            project.status != ProjectOuterClass.ProjectStatus.PROJECT_STATUS_ACTIVE
        ) {
            verifyServerAdminRole(currentUser) {
                val message = if (accessType == AccessType.CREATE) {
                    "Cannot create a criterion for the project with the id: $projectId."
                } else {
                    "Cannot update criterion with the ID: ${criterion?.id ?: "<unknown>"}"
                }
                throw SnowballRException.FailedPreconditionException("The project is not active. $message")
            }
        }
    }

    /**
     * Checks if the current user has permission to access the provided criterion.
     *
     * If the criterion is a user criterion but was created by another user,
     * the method verifies whether the current user has server admin permissions. If the current user
     * does not have such permissions, an [UnauthorizedException.Single] is thrown.
     *
     * @param criterion The criterion to be validated. This object may or may not have an associated project or creator.
     * @param currentUser The user whose permissions need to be validated.
     * @param accessType The type of access being requested (e.g., READ, UPDATE).
     *
     * @throws UnauthorizedException.Single If the current user does not have permissions to access the criterion.
     */
    private fun checkUserCriterionPermission(criterion: UserCriterion, currentUser: User, accessType: AccessType) {
        if (criterion.createdBy == currentUser.id) return

        verifyServerAdminRole(currentUser) {
            throw UnauthorizedException.Single(
                EntityType.CRITERION,
                criterion.id.toString(),
                accessType,
                it,
            )
        }
    }

    /**
     * Verifies that the given user has the required permissions to access the specified criterion.
     * Depending on whether the criterion belongs to a project or is user-specific, the appropriate permission
     * checks are performed by delegating to either `checkProjectCriterionPermission` or `checkUserCriterionPermission`.
     *
     * @param criterion The criterion object whose permissions need to be validated.
     *                  It may either belong to a project or be user-specific.
     * @param currentUser The user whose permissions are being checked against the given criterion.
     * @param accessType The type of access being requested on the criterion (e.g., READ, UPDATE, DELETE).
     */
    private suspend fun checkCriterionPermission(criterion: Criterion, currentUser: User, accessType: AccessType) {
        when (criterion) {
            is ProjectCriterion -> checkProjectCriterionPermission(
                criterion,
                criterion.projectId,
                currentUser,
                accessType,
            )
            is UserCriterion -> checkUserCriterionPermission(criterion, currentUser, accessType)
        }
    }

    override suspend fun getCriterionById(request: Base.Id): GrpcCriterion {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val criterionId = parseUUID(request.id, EntityType.CRITERION)
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        checkCriterionPermission(criterion, currentUser, AccessType.READ)
        return criterion.toGrpcCriterion()
    }

    override suspend fun createCriterion(request: GrpcCriterion.Create): GrpcCriterion {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        if (request.projectId.isNotEmpty()) {
            checkProjectCriterionPermission(
                null,
                parseUUID(request.projectId, EntityType.PROJECT),
                currentUser,
                AccessType.CREATE,
            )
        }
        return repo.createCriterion(request, currentUser.id).toGrpcCriterion()
    }

    override suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val criterionId = parseUUID(request.criterion.id, EntityType.CRITERION)
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        checkCriterionPermission(criterion, currentUser, AccessType.UPDATE)
        return repo.updateCriterion(request).toGrpcCriterion()
    }

    override suspend fun getAllCriteriaForProject(request: Base.Id): GrpcCriterion.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val projectId = parseUUID(request.id, EntityType.PROJECT)

        if (!projectRepo.doesProjectExistById(projectId)) {
            throw NotFoundException(EntityType.PROJECT, projectId.toString())
        }

        val projectMembers = projectMemberRepo.getProjectMembers(projectId)

        if (!projectMembers.any { it.userId == currentUser.id }) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        return repo.getAllProjectCriteria(projectId).toGrpcCriteria()
    }
}
