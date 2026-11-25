package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ReviewOuterClass.Review
import snowballr.ReviewOuterClass.Review.Create

/**
 * A validator for [Review] related requests.
 */
object ReviewValidator {
    fun validateCreateRequest(request: Create): EitherNel<ValidationIssue, Unit> = either {
        ensureIdValidity("project_id", request.projectId)
        ensureIdValidity("paper_id", request.paperId)
        ensureEnumNotUnspecified("decision", request.decision)
        ensureIdListValidity("selected_criteria_ids", request.selectedCriteriaIdsList)
        ensureIdListDistinct("selected_criteria_ids", request.selectedCriteriaIdsList)
    }.toEitherNel()
}
