package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.CriterionOuterClass.Criterion

const val CRITERION_TAG_MAX_LENGTH = 10
const val CRITERION_NAME_MAX_LENGTH = 50
const val CRITERION_DESCRIPTION_MAX_LENGTH = 200

private const val FIELD_TAG = "tag"
private const val FIELD_NAME = "name"
private const val FIELD_DESCRIPTION = "description"
private const val FIELD_CATEGORY = "category"
private const val FIELD_PROJECT_ID = "project_id"
private const val FIELD_ID = "id"

private const val MASK_TAG = "criterion.tag"
private const val MASK_NAME = "criterion.name"
private const val MASK_DESCRIPTION = "criterion.description"
private const val MASK_CATEGORY = "criterion.category"

/**
 * A validator for [Criterion] related requests.
 */
object CriterionValidator {
    fun validateCreateRequest(request: Criterion.Create): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            {
                if (request.projectId.isNotEmpty()) {
                    ensureIdValidity(FIELD_PROJECT_ID, request.projectId)
                }
            },
            {
                ensureTextFieldValidity(FIELD_TAG, request.tag, CRITERION_TAG_MAX_LENGTH)
            },
            {
                ensureTextFieldValidity(FIELD_NAME, request.name, CRITERION_NAME_MAX_LENGTH)
            },
            {
                ensureTextFieldValidity(FIELD_DESCRIPTION, request.description, CRITERION_DESCRIPTION_MAX_LENGTH)
            },
            {
                ensureEnumNotUnspecified(FIELD_CATEGORY, request.category)
            },
        ) { _, _, _, _, _ -> }
    }

    fun validateUpdateRequest(request: Criterion.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, Criterion.Update.getDescriptor())
        }

        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        val selectedFields = request.mask.pathsList.toSet()

        zipOrAccumulate(
            { ensureIdValidity(FIELD_ID, request.criterion.id) },
            {
                if (MASK_TAG in selectedFields) {
                    ensureTextFieldValidity(FIELD_TAG, request.criterion.tag, CRITERION_TAG_MAX_LENGTH)
                }
            },
            {
                if (MASK_NAME in selectedFields) {
                    ensureTextFieldValidity(FIELD_NAME, request.criterion.name, CRITERION_NAME_MAX_LENGTH)
                }
            },
            {
                if (MASK_DESCRIPTION in selectedFields) {
                    ensureTextFieldValidity(
                        FIELD_DESCRIPTION,
                        request.criterion.description,
                        CRITERION_DESCRIPTION_MAX_LENGTH,
                    )
                }
            },
            {
                if (MASK_CATEGORY in selectedFields) {
                    ensureEnumNotUnspecified(FIELD_CATEGORY, request.criterion.category)
                }
            },
        ) { _, _, _, _, _ -> }
    }
}
