package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.toGrpcCriteria
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forTarget
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isCreatorOfCriterion
import se.uulm.snowballr.backend.service.accessrules.isProjectActive
import se.uulm.snowballr.backend.service.accessrules.isProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.isProjectExistent
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.isUserAdminInProjectOfCriterion
import se.uulm.snowballr.backend.service.accessrules.isUserInProjectOfCriterion
import se.uulm.snowballr.backend.service.accessrules.orElse
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import snowballr.Base
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
    override suspend fun getCriterionById(request: Base.Id): GrpcCriterion = withUser(userRepo) { currentUser ->
        val criterionId = parseUUID(request.id, EntityType.CRITERION)
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        isCreatorOfCriterion()
            .orElse(isUserInProjectOfCriterion(projectMemberRepo))
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedReadException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)

        criterion.toGrpcCriterion()
    }

    override suspend fun createCriterion(request: GrpcCriterion.Create): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            if (request.projectId.isNotEmpty()) {
                val projectId = parseUUID(request.projectId, EntityType.PROJECT)

                isProjectAdmin(projectMemberRepo)
                    .orElse(isServerAdmin().forTarget())
                    .orElseThrow { user, target ->
                        UnauthorizedCreateException(user.id, target, EntityType.CRITERION)
                    }
                    .checkFor(currentUser, projectId)

                val project = projectRepo.getProjectById(projectId).getOrThrow()

                isProjectActive().checkFor(currentUser, project)
            }

            repo.createCriterion(request, currentUser.id).toGrpcCriterion()
        }

    override suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            val criterionId = parseUUID(request.criterion.id, EntityType.CRITERION)
            val criterion = repo.getCriterionById(criterionId).getOrThrow()

            if (criterion is Criterion.ProjectCriterion) {
                val project = projectRepo.getProjectById(criterion.projectId).getOrThrow()

                isProjectActive().checkFor(currentUser, project)
            }

            isCreatorOfCriterion()
                .orElse(isUserAdminInProjectOfCriterion(projectMemberRepo))
                .orElse(isServerAdmin().forTarget())
                .orElseThrow { user, target ->
                    UnauthorizedUpdateException(user.id, target.id, EntityType.CRITERION)
                }
                .checkFor(currentUser, criterion)

            repo.updateCriterion(request).toGrpcCriterion()
        }

    override suspend fun getAllCriteriaForProject(request: Base.Id): GrpcCriterion.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.id, EntityType.PROJECT)

            isAllowedToReadProject(projectMemberRepo)
                .andAlso(isProjectExistent(projectRepo))
                .checkFor(currentUser, projectId)

            repo.getAllProjectCriteria(projectId).toGrpcCriteria()
        }
}
