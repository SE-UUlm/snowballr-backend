package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.InvalidId
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

/**
 * A validator for [GrpcProjectPaper] related requests.
 */
object ProjectPaperValidator {
    private const val PROJECT_ID_KEY = "project_id"
    const val QUERY_MAX_LENGTH = 50

    fun validateGetRequest(request: GrpcProjectPaper.Get): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureIdValidity(PROJECT_ID_KEY, request.projectId) },
            {
                ensureRelativeProjectPaperIdValidity(request.relativeProjectPaperId)
            },
        ) { _, _ -> }
    }

    fun validateAddRequest(request: GrpcProjectPaper.Add): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureIdValidity(PROJECT_ID_KEY, request.projectId) },
            { ensureIdValidity("paper_id", request.paperId) },
            { ensureStageValidity(request.stage) },
        ) { _, _, _ -> }
    }

    fun validateSearchQueryRequest(request: GrpcProjectPaper.SearchQuery): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureIdValidity(PROJECT_ID_KEY, request.projectId) },
            { ensureTextFieldValidity("query", request.query, QUERY_MAX_LENGTH) },
        ) { _, _ -> }
    }

    /**
     * Ensures the validity of a given relative project paper ID.
     *
     * This function performs the following validations:
     * - Ensures the ID is non-blank.
     * - Ensures the ID can be converted to a `Long`.
     *
     * @param relativeId The relative project paper ID to validate.
     */
    private fun Raise<ValidationIssue>.ensureRelativeProjectPaperIdValidity(relativeId: String) {
        ensureLongIdValidity("relative_project_paper_id", relativeId)
    }

    /**
     * Ensures that the given string value can be converted to a `Long` and is not negative.
     *
     * If the value cannot be converted to a `Long` or is negative, this function raises a validation issue
     * of type [InvalidId].
     *
     * @param name The name of the ID field that has an invalid format.
     * @param value The string value to check for `Long` convertibility.
     */
    private fun Raise<ValidationIssue>.ensureLongIdValidity(name: String, value: String) {
        ensure(value.toLongOrNull() != null && value.toLong() >= 0L) {
            InvalidId(name, value)
        }
    }
}
