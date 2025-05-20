package se.uulm.snowballr.backend.service.criterion

import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.service.ICreateCriterion
import snowballr.CriterionOuterClass

/**
 * The [CreateCriterion] class provides an implementation for the [ICreateCriterion] interface.
 *
 * It serves as a wrapper around the [ICriterionTableRepo] repository, delegating the responsibility
 * of creating a criterion to the underlying repository implementation.
 *
 * @constructor Initializes the [CreateCriterion] with a given criterion repository.
 * @param repo The repository responsible for handling persistence operations related to criteria.
 */
class CreateCriterion(
    private val repo: ICriterionTableRepo,
) : ICreateCriterion {
    override suspend fun createCriterion(request: CriterionOuterClass.Criterion.Create): CriterionOuterClass.Criterion =
        repo.createCriterion(request)
}
