package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Create
import snowballr.ProjectOuterClass.ReviewDecisionMatrix

/**
 * A validator for [Project] related requests.
 */
@Suppress("StringLiteralDuplication")
object ProjectValidator {
    const val NAME_MAX_LENGTH = 100
    const val SIMILARITY_THRESHOLD_MIN_VALUE = 0.0f
    const val SIMILARITY_THRESHOLD_MAX_VALUE = 1.0f
    const val NUMBER_OF_REVIEWERS_MIN_VALUE = 1
    const val NUMBER_OF_REVIEWERS_MAX_VALUE = 10

    const val FIELD_DECISION_MATRIX = "project.settings.decision_matrix"

    fun validateCreateRequest(request: Create): EitherNel<ValidationIssue, Unit> = either {
        ensureProjectNameValidity(request.name)
    }.toEitherNel()

    @Suppress("CognitiveComplexMethod", "kotlin:S3776")
    fun validateUpdateRequest(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(
                request.mask,
                Project.Update.getDescriptor(),
                listOf("project.current_stage", "project.max_stage"),
            )
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        // Only proceed with field validation if the mask is valid
        val selectedFields = FieldMaskUtil.normalize(request.mask).pathsList.toSet()

        val project = request.project
        @Suppress("NamedArguments")
        zipOrAccumulate(
            { ensureIdValidity("id", project.id) },
            {
                if ("project.name" in selectedFields) {
                    ensureProjectNameValidity(project.name, "project.name")
                }
            },
            {
                if ("project.status" in selectedFields) {
                    ensureEnumNotUnspecified("project.status", project.status)
                }
            },
            {
                if ("project.settings.snowballing_type" in selectedFields) {
                    ensureEnumNotUnspecified("project.settings.snowballing_type", project.settings.snowballingType)
                }
            },
            {
                if ("project.settings.similarity_threshold" in selectedFields) {
                    ensureNumberFieldInRange(
                        "project.settings.similarity_threshold",
                        project.settings.similarityThreshold,
                        SIMILARITY_THRESHOLD_MIN_VALUE,
                        SIMILARITY_THRESHOLD_MAX_VALUE,
                    )
                }
            },
            {
                val decisionMatrixFields = selectedFields.filter { it.startsWith(FIELD_DECISION_MATRIX) }
                if (decisionMatrixFields.isNotEmpty()) {
                    ensureDecisionMatrixValidity(decisionMatrixFields, project.settings.decisionMatrix)
                }
            },
        ) { _, _, _, _, _, _ -> }
    }

    fun validateGetInformationRequest(request: Project.Information.Get): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, Project.Information.getDescriptor(), allowEmpty = true)
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        either { ensureIdValidity("project_id", request.projectId) }.toEitherNel().bind()
    }

    fun validateGetDecisionStatisticsRequest(
        request: Project.Information.DecisionStatistics.Get,
    ): EitherNel<ValidationIssue, Unit> = either {
        ensureIdValidity("project_id", request.projectId)
        ensureStageValidity(request.stage)
    }.toEitherNel()

    /**
     * Ensures that the provided project name is valid.
     * It checks that the project name is not blank and does not exceed the maximum length defined by [NAME_MAX_LENGTH].
     *
     * @param name The project name to validate.
     * @param fieldName The name of the field being validated, used for error reporting. Defaults to "name".
     */
    private fun Raise<ValidationIssue>.ensureProjectNameValidity(name: String, fieldName: String = "name") =
        ensureTextFieldValidity(fieldName, name, NAME_MAX_LENGTH)

    private fun Raise<ValidationIssue>.ensureDecisionMatrixValidity(
        selectedFields: List<String>,
        decisionMatrix: ReviewDecisionMatrix,
    ) {
        val decisionMatrixFields = selectedFields.map { it.substringAfter("$FIELD_DECISION_MATRIX.") }.toSet()

        if ("number_of_reviewers" in decisionMatrixFields) {
            ensureNumberFieldInRange(
                "$FIELD_DECISION_MATRIX.number_of_reviewers",
                decisionMatrix.numberOfReviewers,
                NUMBER_OF_REVIEWERS_MIN_VALUE,
                NUMBER_OF_REVIEWERS_MAX_VALUE,
            )
        }
    }
}
