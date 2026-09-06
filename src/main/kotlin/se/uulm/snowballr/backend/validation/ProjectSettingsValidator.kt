package se.uulm.snowballr.backend.validation

import arrow.core.raise.Raise
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ReviewDecisionMatrix

object ProjectSettingsValidator {
    const val SIMILARITY_THRESHOLD_MIN_VALUE = 0.2f
    const val SIMILARITY_THRESHOLD_MAX_VALUE = 1.0f
    const val NUMBER_OF_REVIEWERS_MIN_VALUE = 1
    const val NUMBER_OF_REVIEWERS_MAX_VALUE = 10

    fun Raise<ValidationIssue>.validateSnowballingType(
        projectSettings: Project.Settings,
        selectedFields: Set<String>,
        fieldName: String,
    ) {
        if (fieldName in selectedFields) {
            ensureEnumNotUnspecified(fieldName, projectSettings.snowballingType)
        }
    }

    fun Raise<ValidationIssue>.validateSimilarityThreshold(
        projectSettings: Project.Settings,
        selectedFields: Set<String>,
        fieldName: String,
    ) {
        if (fieldName in selectedFields) {
            ensureNumberFieldInRange(
                fieldName,
                projectSettings.similarityThreshold,
                SIMILARITY_THRESHOLD_MIN_VALUE,
                SIMILARITY_THRESHOLD_MAX_VALUE,
            )
        }
    }

    fun Raise<ValidationIssue>.validateDecisionMatrix(
        projectSettings: Project.Settings,
        selectedFields: Set<String>,
        fieldName: String,
    ) {
        val decisionMatrixFields = selectedFields.filter { it.startsWith(fieldName) }
        if (decisionMatrixFields.isNotEmpty()) {
            ensureDecisionMatrixValidity(decisionMatrixFields, projectSettings.decisionMatrix, fieldName)
        }
    }

    private fun Raise<ValidationIssue>.ensureDecisionMatrixValidity(
        selectedFields: List<String>,
        decisionMatrix: ReviewDecisionMatrix,
        fieldName: String,
    ) {
        val decisionMatrixFields = selectedFields.map { it.substringAfter("$fieldName.") }.toSet()

        if ("number_of_reviewers" in decisionMatrixFields) {
            ensureNumberFieldInRange(
                "$fieldName.number_of_reviewers",
                decisionMatrix.numberOfReviewers,
                NUMBER_OF_REVIEWERS_MIN_VALUE,
                NUMBER_OF_REVIEWERS_MAX_VALUE,
            )
        }
    }
}
