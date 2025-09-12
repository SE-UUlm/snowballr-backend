package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.Raise
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project.Paper as GrpcProjectPaper

/**
 * A validator for [GrpcProjectPaper] related requests.
 */
object ProjectPaperValidator {
    fun validateGetRequest(request: GrpcProjectPaper.Get): EitherNel<ValidationIssue, Unit> = either {
        zipOrAccumulate(
            { ensureIdValidity("id", request.projectId) },
            {
                ensureRelativeProjectPaperIdValidity(request.relativeProjectPaperId)
            },
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
    fun Raise<ValidationIssue>.ensureRelativeProjectPaperIdValidity(relativeId: String) {
        ensureFieldNonBlank("relative_project_paper_id", relativeId)
        ensureStringIsLongConvertible(relativeId)
    }
}
