package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.OutOfRangeValue
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Create

/**
 * A validator for [Project] related requests.
 */
object ProjectValidator {
    const val PROJECT_NAME_MAX_LENGTH = 100

    fun validateCreateRequest(request: Create): EitherNel<ValidationIssue, Unit> = either {
        ensureProjectNameValidity(request.name)
    }.toEitherNel()

    fun validateUpdateRequest(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, Project.Update.getDescriptor())
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        // Only proceed with field validation if the mask is valid
        val selectedFields = request.mask.pathsList.toSet()

        zipOrAccumulate(
            { ensureIdValidity("id", request.project.id) },
            {
                if ("project.name" in selectedFields) {
                    ensureProjectNameValidity(request.project.name)
                }
            },
            {
                if ("project.status" in selectedFields) {
                    ensureEnumNotUnspecified("status", request.project.status)
                }
            },
            {
                if ("project.settings.snowballing_type" in selectedFields) {
                    ensureEnumNotUnspecified("snowballing_type", request.project.settings.snowballingType)
                }
            },
            {
                if ("project.settings.similarity_threshold" in selectedFields) {
                    ensureSimilarityThresholdValidity(request.project.settings.similarityThreshold)
                }
            },
        ) { _, _, _, _, _ -> }
    }

    /**
     * Ensures that the provided project name is valid.
     * It checks that the project name is not blank and does not exceed the maximum length defined by [PROJECT_NAME_MAX_LENGTH].
     *
     * @param name The project name to validate.
     */
    fun Raise<ValidationIssue>.ensureProjectNameValidity(name: String) =
        ensureTextFieldValidity("name", name, PROJECT_NAME_MAX_LENGTH)

    fun Raise<ValidationIssue>.ensureSimilarityThresholdValidity(threshold: Float) {
        ensure(threshold in 0.0f..1.0f) {
            OutOfRangeValue("similarity_threshold", threshold, 0.0f, 1.0f)
        }
    }
}
