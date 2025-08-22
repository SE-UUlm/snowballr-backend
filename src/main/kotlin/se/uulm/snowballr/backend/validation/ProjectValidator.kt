package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.validation.ProjectValidator.NAME_MAX_LENGTH
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Create

/**
 * A validator for [Project] related requests.
 */
object ProjectValidator {
    const val NAME_MAX_LENGTH = 100
    const val SIMILARITY_THRESHOLD_MIN_VALUE = 0.0f
    const val SIMILARITY_THRESHOLD_MAX_VALUE = 1.0f

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

        val project = request.project
        zipOrAccumulate(
            { ensureIdValidity("id", project.id) },
            {
                if ("project.name" in selectedFields) {
                    ensureProjectNameValidity(project.name)
                }
            },
            {
                if ("project.status" in selectedFields) {
                    ensureEnumNotUnspecified("status", project.status)
                }
            },
            {
                if ("project.settings.snowballing_type" in selectedFields) {
                    ensureEnumNotUnspecified("snowballing_type", project.settings.snowballingType)
                }
            },
            {
                if ("project.settings.similarity_threshold" in selectedFields) {
                    ensureNumberFieldInRange(
                        "similarity_threshold",
                        project.settings.similarityThreshold,
                        SIMILARITY_THRESHOLD_MIN_VALUE,
                        SIMILARITY_THRESHOLD_MAX_VALUE,
                    )
                }
            },
        ) { _, _, _, _, _ -> }
    }

    fun validateInviteRequest(request: Project.Member.Invite): EitherNel<ValidationIssue, Unit> = either {
        ensureFieldNonBlank("projectId", request.projectId)
        ensureEmailValidity(request.userEmail)
    }.toEitherNel()

    fun validateAcceptRequest(request: Project.Member.Accept): EitherNel<ValidationIssue, Unit> = either {
        ensureFieldNonBlank("token", request.token)
    }.toEitherNel()

    /**
     * Ensures that the provided project name is valid.
     * It checks that the project name is not blank and does not exceed the maximum length defined by [NAME_MAX_LENGTH].
     *
     * @param name The project name to validate.
     */
    fun Raise<ValidationIssue>.ensureProjectNameValidity(name: String) =
        ensureTextFieldValidity("name", name, NAME_MAX_LENGTH)
}
