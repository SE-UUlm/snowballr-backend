package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.Nel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.model.ValidationIssue
import se.uulm.snowballr.backend.model.dto.project.ProjectField
import se.uulm.snowballr.backend.model.dto.project.ProjectInfoField
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateDecisionMatrix
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateSimilarityThreshold
import se.uulm.snowballr.backend.validation.ProjectSettingsValidator.validateSnowballingType
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.Project.Create

/**
 * A validator for [Project] related requests.
 */
object ProjectValidator {
    const val NAME_MAX_LENGTH = 100

    private const val FIELD_PROJECT_ID = "project.id"
    private const val FIELD_PROJECT_NAME = "project.name"
    private const val FIELD_PROJECT_STATUS = "project.status"
    private const val FIELD_SNOWBALLING_TYPE = "project.settings.snowballing_type"
    private const val FIELD_SIMILARITY_THRESHOLD = "project.settings.similarity_threshold"
    private const val FIELD_DECISION_MATRIX = "project.settings.decision_matrix"

    fun validateCreateRequest(request: Create): EitherNel<ValidationIssue, Unit> = either {
        ensureProjectNameValidity(request.name)
    }.toEitherNel()

    fun validateUpdateRequest(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        validateUpdateFieldMask(request).bind()
        val selectedFields = FieldMaskUtil.normalize(request.mask).pathsList.toSet()
        validateUpdateProjectFields(request.project, selectedFields)
    }

    fun validateGetInformationRequest(request: Project.Information.Get): EitherNel<ValidationIssue, Unit> = either {
        val allowedFields = ProjectInfoField.entries.map { getGrpcPathsForProjectInfoField(it) }

        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, allowedFields, allowEmpty = true)
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

    fun getGrpcPathsForProjectField(field: ProjectField) = when (field) {
        ProjectField.NAME -> "project.name"
        ProjectField.STATUS -> "project.status"
        ProjectField.SIMILARITY_THRESHOLD -> "project.settings.similarity_threshold"
        ProjectField.SNOWBALLING_TYPE -> "project.settings.snowballing_type"
        ProjectField.REVIEW_MAYBE_ALLOWED -> "project.settings.review_maybe_allowed"
        ProjectField.FETCHERS -> "project.settings.fetchers"
        ProjectField.NUMBER_OF_REVIEWERS -> "project.settings.decision_matrix.number_of_reviewers"
        ProjectField.DECISION_MATRIX_PATTERNS -> "project.settings.decision_matrix.patterns"
    }

    fun getGrpcPathsForProjectInfoField(field: ProjectInfoField) = when (field) {
        ProjectInfoField.PROJECT_PROGRESS -> "project_progress"
        ProjectInfoField.CREATION_DATE -> "creation_date"
        ProjectInfoField.LAST_STAGE_STARTED -> "last_stage_started"
    }

    /**
     * Ensures that the provided project name is valid.
     * It checks that the project name is not blank and does not exceed the maximum length defined by [NAME_MAX_LENGTH].
     *
     * @param name The project name to validate.
     * @param fieldName The name of the field being validated, used for error reporting. Defaults to "name".
     */
    private fun Raise<ValidationIssue>.ensureProjectNameValidity(name: String, fieldName: String = "name") =
        ensureTextFieldValidity(fieldName, name, NAME_MAX_LENGTH)

    private fun validateUpdateFieldMask(request: Project.Update): EitherNel<ValidationIssue, Unit> = either {
        val allowedPaths = listOf(FIELD_PROJECT_ID) + ProjectField.entries.map { getGrpcPathsForProjectField(it) }
        ensureFieldMaskIsValid(request.mask, allowedPaths)
    }.toEitherNel()

    private fun Raise<Nel<ValidationIssue>>.validateUpdateProjectFields(project: Project, selectedFields: Set<String>) {
        @Suppress("NamedArguments")
        zipOrAccumulate(
            { ensureIdValidity("id", project.id) },
            { validateProjectName(project, selectedFields) },
            { validateProjectStatus(project, selectedFields) },
            { validateSnowballingType(project.settings, selectedFields, FIELD_SNOWBALLING_TYPE) },
            { validateSimilarityThreshold(project.settings, selectedFields, FIELD_SIMILARITY_THRESHOLD) },
            { validateDecisionMatrix(project.settings, selectedFields, FIELD_DECISION_MATRIX) },
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
}
