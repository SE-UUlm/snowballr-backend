package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.access.ICriterionAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.toGrpcCriteria
import se.uulm.snowballr.backend.model.dto.criterion.toGrpcCriterion
import se.uulm.snowballr.backend.model.incoming.CreateCriterionRequest
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion

interface ICriterionService {
    /**
     * Service implementation of [SnowballRService.getCriterionById].
     */
    suspend fun getCriterionById(criterionId: UUID): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.createCriterion].
     */
    suspend fun createCriterion(request: CreateCriterionRequest): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.updateCriterion].
     */
    suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion

    /**
     * Service implementation of [SnowballRService.getAllCriteriaForProject].
     */
    suspend fun getAllCriteriaForProject(projectId: UUID): GrpcCriterion.List
}

/**
 * The [CriterionService] class handles operations related to projects by implementing the [ICriterionService]
 * interface.
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
 * @param accessChecker Interface for checking access permissions for criteria based on defined rules.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
class CriterionService(
    private val repo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val accessChecker: ICriterionAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
) : ICriterionService {
    override suspend fun getCriterionById(criterionId: UUID): GrpcCriterion = withUser(userRepo) { currentUser ->
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        accessChecker.isAllowedToReadCriterion(currentUser, criterion)

        criterion.toGrpcCriterion()
    }

    override suspend fun createCriterion(request: CreateCriterionRequest): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            if (request.projectId != null) {
                accessChecker.isAllowedToCreateProjectCriterion(currentUser, request.projectId)
            }

            repo.createCriterion(request, currentUser.id).toGrpcCriterion()
        }

    override suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            val criterionId = parseUUID(request.criterion.id, EntityType.CRITERION)
            val criterion = repo.getCriterionById(criterionId).getOrThrow()

            accessChecker.isAllowedToUpdateCriterion(currentUser, criterion)

            repo.updateCriterion(request).toGrpcCriterion()
        }

    override suspend fun getAllCriteriaForProject(projectId: UUID): GrpcCriterion.List =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            repo.getAllProjectCriteria(projectId).toGrpcCriteria()
        }
}
