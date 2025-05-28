package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.CriterionOuterClass.Criterion

const val CRITERION_TAG_MAX_LENGTH = 10
const val CRITERION_NAME_MAX_LENGTH = 50
const val CRITERION_DESCRIPTION_MAX_LENGTH = 200

/**
 * An object that validates the requests for [Criterion] objects.
 */
object CriterionValidator {
    fun validateCreateRequest(request: Criterion.Create): EitherNel<ValidationIssue, Unit> =
        either {
            zipOrAccumulate(
                { ensureFieldNonBlank("project_id", request.projectId) },
                {
                    ensureFieldNonBlank("tag", request.tag)
                    ensureFieldLength("tag", request.tag, CRITERION_TAG_MAX_LENGTH)
                },
                {
                    ensureFieldNonBlank("name", request.name)
                    ensureFieldLength("name", request.name, CRITERION_NAME_MAX_LENGTH)
                },
                {
                    ensureFieldNonBlank("description", request.description)
                    ensureFieldLength("description", request.description, CRITERION_DESCRIPTION_MAX_LENGTH)
                },
                {
                    ensureEnumNotUnspecified("category", request.category)
                },
            ) { _, _, _, _, _ -> }
        }
}
