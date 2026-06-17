package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.NonEmptyList
import arrow.core.nonEmptyListOf
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import io.grpc.health.v1.HealthCheckRequest
import io.grpc.reflection.v1.ServerReflectionRequest
import se.uulm.snowballr.backend.model.UnknownRequest
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Authentication
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.Export
import snowballr.PaperOuterClass.Paper
import snowballr.ProjectOuterClass
import snowballr.ReviewOuterClass
import snowballr.UserOuterClass

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
@Suppress("CyclomaticComplexMethod")
fun <T> validateRequest(request: T): EitherNel<ValidationIssue, Unit> = when (request) {
    // Healthcheck
    is HealthCheckRequest -> Either.Right(Unit)
    // Reflection
    is ServerReflectionRequest -> Either.Right(Unit)
    // Authentication
    is Authentication.RegisterRequest -> AuthenticationValidator.validateRegisterRequest(request)
    is Authentication.VerifyEmailRequest -> AuthenticationValidator.validateVerifyEmailRequest(request)
    is Authentication.LoginRequest -> AuthenticationValidator.validateLoginRequest(request)
    is Authentication.PasswordChangeRequest -> AuthenticationValidator.validateChangePasswordRequest(request)
    // User
    is UserOuterClass.User.Update -> UserValidator.validateUpdateRequest(request)
    // Project
    is ProjectOuterClass.Project.Create -> ProjectValidator.validateCreateRequest(request)
    is ProjectOuterClass.Project.Update -> ProjectValidator.validateUpdateRequest(request)
    is ProjectOuterClass.Project.InviteCandidatesRequest -> Either.Right(Unit)
    is ProjectOuterClass.Project.Information.Get -> ProjectValidator.validateGetInformationRequest(request)
    is ProjectOuterClass.Project.Information.DecisionStatistics.Get ->
        ProjectValidator.validateGetDecisionStatisticsRequest(request)
    // Project Paper
    is ProjectOuterClass.Project.Paper.Get -> ProjectPaperValidator.validateGetRequest(request)
    is ProjectOuterClass.Project.Paper.Add -> ProjectPaperValidator.validateAddRequest(request)
    is ProjectOuterClass.Project.Paper.SearchQuery -> ProjectPaperValidator.validateSearchQueryRequest(request)
    // Project Member
    is ProjectOuterClass.Project.Member.Invite -> ProjectMemberValidator.validateInviteRequest(request)
    is ProjectOuterClass.Project.Member.Accept -> ProjectMemberValidator.validateAcceptRequest(request)
    is ProjectOuterClass.Project.Member.Remove -> ProjectMemberValidator.validateRemoveRequest(request)
    is ProjectOuterClass.Project.Member.Update -> ProjectMemberValidator.validateMemberUpdateRequest(request)
    // Criterion
    is CriterionOuterClass.Criterion.Create -> CriterionValidator.validateCreateRequest(request)
    is CriterionOuterClass.Criterion.Update -> CriterionValidator.validateUpdateRequest(request)
    // Review
    is ReviewOuterClass.Review.Create -> ReviewValidator.validateCreateRequest(request)
    // Base
    is Base.Id -> BaseValidator.validateId(request)
    is Base.Email -> BaseValidator.validateEmail(request)
    is Base.Nothing -> Either.Right(Unit)
    // Paper
    is Paper.Update -> PaperValidator.validateUpdateRequest(request)
    is Paper -> PaperValidator.validateCreateRequest(request)
    // Export
    is Export.ExportRequest -> ExportValidator.validateExportRequest(request)
    // Fallback for unknown request types
    else -> Either.Left(nonEmptyListOf(UnknownRequest))
}

/**
 * Creates a [NonEmptyList] from this [Either].
 *
 * While some validation methods use [zipOrAccumulate] to process several input validation conditions,
 * others might require only validating one parameter and use a simple [either].
 * For the latter, one can use this method to create a [NonEmptyList] to get the same data type as the other
 * methods that accumulate the validation results.
 */
fun Either<ValidationIssue, Unit>.toEitherNel() = when (this) {
    is Either.Left -> Either.Left(nonEmptyListOf(this.value))
    is Either.Right -> Either.Right(Unit)
}
