package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import se.uulm.snowballr.backend.model.UnknownRequest
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass

/**
 * Validates a given request object and returns either a collection of validation issues
 * or an indication that the validation was successful.
 *
 * This function delegates validation to specific validators depending on the type of the provided request.
 * If the request type is unknown, an error containing [UnknownRequest] is returned.
 *
 * @param T The generic type of the request being validated.
 * @param request The request object to be validated.
 * @return An [EitherNel] containing a collection of [ValidationIssue] objects if validation issues are found,
 *         or `Unit` if the validation is successful.
 */
fun <T> validateRequest(request: T): EitherNel<ValidationIssue, Unit> =
    when (request) {
        // Project
        is ProjectOuterClass.Project.Create -> ProjectValidator.validateCreateRequest(request)
        // Criterion
        is CriterionOuterClass.Criterion.Create -> CriterionValidator.validateCreateRequest(request)
        else -> Either.Left(nonEmptyListOf(UnknownRequest))
    }

/**
 * Creates a [arrow.core.NonEmptyList] from this [Either].
 *
 * While some validation methods use [arrow.core.raise.zipOrAccumulate] to process several input validation conditions,
 * others might require only validating one parameter and use a simple [arrow.core.raise.either].
 * For the latter, one can use this method to create a [arrow.core.NonEmptyList] to get the same data type as the other
 * methods that accumulate the validation results.
 */
fun Either<ValidationIssue, Unit>.toEitherNel() =
    when (this) {
        is Either.Left -> Either.Left(nonEmptyListOf(this.value))
        is Either.Right -> Either.Right(Unit)
    }
