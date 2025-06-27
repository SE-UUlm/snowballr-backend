package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.nonEmptyListOf
import io.grpc.health.v1.HealthCheckRequest
import se.uulm.snowballr.backend.model.UnknownRequest
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass

/**
 * Email regex.
 *
 * See: https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression/201378#201378
 */
@Suppress("MaxLineLength", "StringShouldBeRawString")
val EMAIL_REGEX =
    Regex(
        "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])",
    )

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
fun <T> validateRequest(request: T): EitherNel<ValidationIssue, Unit> = when (request) {
    // Healthcheck
    is HealthCheckRequest -> Either.Right(Unit)
    // Project
    is ProjectOuterClass.Project.Create -> ProjectValidator.validateCreateRequest(request)
    // Criterion
    is CriterionOuterClass.Criterion.Create -> CriterionValidator.validateCreateRequest(request)
    // Base
    is Base.Id -> BaseValidator.validateId(request)
    is Base.Email -> BaseValidator.validateEmail(request)
    is Base.Nothing -> Either.Right(Unit)
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
fun Either<ValidationIssue, Unit>.toEitherNel() = when (this) {
    is Either.Left -> Either.Left(nonEmptyListOf(this.value))
    is Either.Right -> Either.Right(Unit)
}
