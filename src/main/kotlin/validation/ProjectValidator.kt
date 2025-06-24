package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.ProjectOuterClass.Project

const val PROJECT_NAME_MAX_LENGTH = 100

/**
 * A validator for [Project] related requests.
 */
object ProjectValidator {
    fun validateCreateRequest(request: Project.Create): EitherNel<ValidationIssue, Unit> =
        either {
            ensureFieldNonBlank("name", request.name)
            ensureFieldLength("name", request.name, PROJECT_NAME_MAX_LENGTH)
        }.toEitherNel()
}
