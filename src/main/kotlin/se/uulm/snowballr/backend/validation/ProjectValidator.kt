package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
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
object ProjectValidator {
    const val NAME_MAX_LENGTH = 100
    const val SIMILARITY_THRESHOLD_MIN_VALUE = 0.2f
    const val SIMILARITY_THRESHOLD_MAX_VALUE = 1.0f
    const val NUMBER_OF_REVIEWERS_MIN_VALUE = 1
    const val NUMBER_OF_REVIEWERS_MAX_VALUE = 10

    private const val FIELD_PROJECT_NAME = "project.name"
    private const val FIELD_PROJECT_STATUS = "project.status"
    private const val FIELD_SNOWBALLING_TYPE = "project.settings.snowballing_type"
    private const val FIELD_SIMILARITY_THRESHOLD = "project.settings.similarity_threshold"
    private const val FIELD_DECISION_MATRIX = "project.settings.decision_matrix"
    private val UNALLOWED_UPDATE_MASK_FIELDS = listOf("project.current_stage", "project.max_stage")

    fun validateCreateRequest(request: Create): EitherNel<ValidationIssue, Unit> = either {
        ensureProjectNameValidity(request.name)
    }.toEitherNel()

    fun validateUpdateRequest(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        validateUpdateFieldMask(request).bind()
        val selectedFields = FieldMaskUtil.normalize(request.mask).pathsList.toSet()
        validateUpdateProjectFields(request.project, selectedFields)
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

    private fun validateUpdateFieldMask(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        ensureFieldMaskIsValid(request.mask, Project.Update.getDescriptor(), UNALLOWED_UPDATE_MASK_FIELDS)
    }.toEitherNel()

    private fun Raise<Nel<ValidationIssue>>.validateUpdateProjectFields(project: Project, selectedFields: Set<String>) {
        @Suppress("NamedArguments")
        zipOrAccumulate(
            { ensureIdValidity("id", project.id) },
            { validateProjectName(project, selectedFields) },
            { validateProjectStatus(project, selectedFields) },
            { validateSnowballingType(project, selectedFields) },
            { validateSimilarityThreshold(project, selectedFields) },
            { validateDecisionMatrix(project, selectedFields) },
        ) { _, _, _, _, _, _ -> }
    }

    private fun Raise<ValidationIssue>.validateProjectName(project: Project, selectedFields: Set<String>) {
        if (FIELD_PROJECT_NAME in selectedFields) {
            ensureProjectNameValidity(project.name, FIELD_PROJECT_NAME)
        }
    }

    private fun Raise<ValidationIssue>.validateProjectStatus(project: Project, selectedFields: Set<String>) {
        if (FIELD_PROJECT_STATUS in selectedFields) {
            ensureEnumNotUnspecified(FIELD_PROJECT_STATUS, project.status)
        }
    }

    private fun Raise<ValidationIssue>.validateSnowballingType(project: Project, selectedFields: Set<String>) {
        if (FIELD_SNOWBALLING_TYPE in selectedFields) {
            ensureEnumNotUnspecified(FIELD_SNOWBALLING_TYPE, project.settings.snowballingType)
        }
    }

    private fun Raise<ValidationIssue>.validateSimilarityThreshold(project: Project, selectedFields: Set<String>) {
        if (FIELD_SIMILARITY_THRESHOLD in selectedFields) {
            ensureNumberFieldInRange(
                FIELD_SIMILARITY_THRESHOLD,
                project.settings.similarityThreshold,
                SIMILARITY_THRESHOLD_MIN_VALUE,
                SIMILARITY_THRESHOLD_MAX_VALUE,
            )
        }
    }

    private fun Raise<ValidationIssue>.validateDecisionMatrix(project: Project, selectedFields: Set<String>) {
        val decisionMatrixFields = selectedFields.filter { it.startsWith(FIELD_DECISION_MATRIX) }
        if (decisionMatrixFields.isNotEmpty()) {
            ensureDecisionMatrixValidity(decisionMatrixFields, project.settings.decisionMatrix)
        }
    }
}
