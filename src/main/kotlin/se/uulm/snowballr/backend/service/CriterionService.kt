package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcCriteria
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedCreateException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedUpdateException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.service.accessrules.IAccessChecker
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forTarget
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.orElse
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
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
    suspend fun getAllCriteriaForProject(projectId: UUID): GrpcCriterion.List
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
 * @param accessChecker Interface for checking access permissions based on defined rules.
 */
class CriterionService(
    private val repo: ICriterionTableRepo,
    private val userRepo: IUserTableRepo,
    private val accessChecker: IAccessChecker,
) : ICriterionService {
    override suspend fun getCriterionById(criterionId: UUID): GrpcCriterion = withUser(userRepo) { currentUser ->
        val criterion = repo.getCriterionById(criterionId).getOrThrow()

        isAllowedToReadCriterion(currentUser, criterion)

        criterion.toGrpcCriterion()
    }

    override suspend fun createCriterion(request: GrpcCriterion.Create): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            if (request.projectId.isNotEmpty()) {
                val projectId = parseUUID(request.projectId, EntityType.PROJECT)

                isAllowedToCreateCriterion(currentUser, projectId)
            }

            repo.createCriterion(request, currentUser.id).toGrpcCriterion()
        }

    override suspend fun updateCriterion(request: GrpcCriterion.Update): GrpcCriterion =
        withUser(userRepo) { currentUser ->
            val criterionId = parseUUID(request.criterion.id, EntityType.CRITERION)
            val criterion = repo.getCriterionById(criterionId).getOrThrow()

            isAllowedToUpdateCriterion(currentUser, criterion)

            if (criterion is Criterion.ProjectCriterion) {
                accessChecker.isProjectActiveById().checkFor(currentUser, criterion.projectId)
            }

            repo.updateCriterion(request).toGrpcCriterion()
        }

    override suspend fun getAllCriteriaForProject(projectId: UUID): GrpcCriterion.List =
        withUser(userRepo) { currentUser ->
            accessChecker.isAllowedToReadProject().checkFor(currentUser, projectId)

            repo.getAllProjectCriteria(projectId).toGrpcCriteria()
        }

    private suspend fun isAllowedToReadCriterion(currentUser: User, criterion: Criterion) {
        accessChecker.isCreatorOfCriterion()
            .orElse(accessChecker.isUserInProjectOfCriterion())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedReadException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)
    }

    @Suppress("RedundantSuspendModifier", "RedundantSuppression")
    private suspend fun isAllowedToCreateCriterion(currentUser: User, projectId: UUID) {
        accessChecker.isProjectAdmin()
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedCreateException(user.id, target, EntityType.CRITERION)
            }
            .checkFor(currentUser, projectId)

        accessChecker.isProjectActiveById().checkFor(currentUser, projectId)
    }

    private suspend fun isAllowedToUpdateCriterion(currentUser: User, criterion: Criterion) {
        accessChecker.isCreatorOfCriterion()
            .orElse(accessChecker.isUserAdminInProjectOfCriterion())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedUpdateException(user.id, target.id, EntityType.CRITERION)
            }
            .checkFor(currentUser, criterion)
    }
}
