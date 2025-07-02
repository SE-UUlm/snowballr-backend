package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Base

/**
 * A validator for [Base] related requests.
 */
object BaseValidator {
    fun validateId(request: Base.Id): EitherNel<ValidationIssue, Unit> = either {
        ensureIdValidity("id", request.id)
    }.toEitherNel()

    fun validateEmail(request: Base.Email): EitherNel<ValidationIssue, Unit> = either {
        ensureEmailValidity(request.email)
    }.toEitherNel()
}
