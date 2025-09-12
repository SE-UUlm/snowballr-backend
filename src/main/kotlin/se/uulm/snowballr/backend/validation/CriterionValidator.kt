package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.CriterionOuterClass.Criterion

/**
 * A validator for [Criterion] related requests.
 */
object CriterionValidator {
    const val TAG_MAX_LENGTH = 10
    const val NAME_MAX_LENGTH = 50
    const val DESCRIPTION_MAX_LENGTH = 200

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

    fun validateCreateRequest(request: Criterion.Create): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            {
                if (request.projectId.isNotEmpty()) {
                    ensureIdValidity(FIELD_PROJECT_ID, request.projectId)
                }
            },
            {
                ensureTextFieldValidity(FIELD_TAG, request.tag, TAG_MAX_LENGTH)
            },
            {
                ensureTextFieldValidity(FIELD_NAME, request.name, NAME_MAX_LENGTH)
            },
            {
                ensureTextFieldValidity(FIELD_DESCRIPTION, request.description, DESCRIPTION_MAX_LENGTH)
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

        val criterion = request.criterion
        zipOrAccumulate(
            { ensureIdValidity(FIELD_ID, criterion.id) },
            {
                if (MASK_TAG in selectedFields) {
                    ensureTextFieldValidity(FIELD_TAG, criterion.tag, TAG_MAX_LENGTH)
                }
            },
            {
                if (MASK_NAME in selectedFields) {
                    ensureTextFieldValidity(FIELD_NAME, criterion.name, NAME_MAX_LENGTH)
                }
            },
            {
                if (MASK_DESCRIPTION in selectedFields) {
                    ensureTextFieldValidity(FIELD_DESCRIPTION, criterion.description, DESCRIPTION_MAX_LENGTH)
                }
            },
            {
                if (MASK_CATEGORY in selectedFields) {
                    ensureEnumNotUnspecified(FIELD_CATEGORY, criterion.category)
                }
            },
        ) { _, _, _, _, _ -> }
    }
}
