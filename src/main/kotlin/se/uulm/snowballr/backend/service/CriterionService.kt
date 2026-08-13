package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.access.ICriterionAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.CriterionField
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import java.util.UUID

private val logger = KotlinLogging.logger {}

interface ICriterionService {
    /**
     * Service implementation of [SnowballRService.getCriterionById].
     */
    suspend fun getCriterionById(criterionId: UUID): Criterion

    /**
     * Service implementation of [SnowballRService.createCriterion].
     */
    suspend fun createCriterion(request: CreateCriterionRequest): Criterion

    /**
     * Service implementation of [SnowballRService.updateCriterion].
     */
    suspend fun updateCriterion(request: UpdateCriterionRequest, fields: Set<CriterionField>): Criterion

    /**
     * Service implementation of [SnowballRService.getAllCriteriaForProject].
     */
    suspend fun getAllCriteriaForProject(projectId: UUID): List<Criterion.ProjectCriterion>
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
    override suspend fun getCriterionById(criterionId: UUID): Criterion = withUser(userRepo) { currentUser ->
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        accessChecker.isAllowedToReadCriterion(currentUser, criterion)

        criterion
    }

    override suspend fun createCriterion(request: CreateCriterionRequest): Criterion =
        withUser(userRepo) { currentUser ->
            if (request.projectId != null) {
                accessChecker.isAllowedToCreateProjectCriterion(currentUser, request.projectId)
            }

            val criterion = repo.createCriterion(request, currentUser.id)
            logger.info {
                val owner = request.projectId?.let { "project $it" } ?: "the user's defaults"
                "Criterion ${criterion.id} ('${criterion.tag}') created for $owner"
            }
            criterion
        }

    override suspend fun updateCriterion(request: UpdateCriterionRequest, fields: Set<CriterionField>): Criterion =
        withUser(userRepo) { currentUser ->
            val criterion = repo.getCriterionById(request.criterionId).getOrThrow()

            accessChecker.isAllowedToUpdateCriterion(currentUser, criterion)

            val updatedCriterion = repo.updateCriterion(request, fields)
            logger.info { "Criterion ${request.criterionId} updated: ${fields.joinToString()}" }
            updatedCriterion
        }

    override suspend fun getAllCriteriaForProject(projectId: UUID): List<Criterion.ProjectCriterion> =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            repo.getAllProjectCriteria(projectId)
        }
}
