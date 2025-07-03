package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.toGrpcCriterion
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import snowballr.CriterionOuterClass

interface ICriterionService {
    /**
     * Service implementation of [SnowballRService.createCriterion].
     */
    suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create): CriterionOuterClass.Criterion
}

/**
 * The [CriterionService] class handles operations for criteria by providing
 * an implementation of the [ICriterionService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of criterion CRUD operations,
 * delegating the actual persistence operations to the [ICriterionTableRepo] repository.
 *
 * @constructor Initializes the [CriterionService] with a criterion repository.
 * @param repo The repository responsible for handling persistence operations related to criteria.
 */
class CriterionService(
    private val repo: ICriterionTableRepo,
) : ICriterionService {
    override suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create): CriterionOuterClass.Criterion {
        // TODO: remove dummy user when user management is implemented
        val requestingUserId = parseUUID(dummyUserId!!, EntityType.USER)
        return repo.createCriterion(request, requestingUserId).toGrpcCriterion()
    }
}
