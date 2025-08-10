package se.uulm.snowballr.backend.validation

import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.Authentication

/**
 * A validator for [Authentication] related requests.
 */
object AuthenticationValidator {
    fun validateRegisterRequest(request: Authentication.RegisterRequest): EitherNel<ValidationIssue, Unit> = either {
        val result1 = either {
            zipOrAccumulate(
                { ensureEmailValidity(request.email) },
                { ensureFirstNameValidity(request.firstName) },
                { ensureLastNameValidity(request.lastName) },
            ) { _, _, _ -> }
        }

        val result2 = ensurePasswordValidity(request.password)

        return EitherNel.zipOrAccumulate(result1, result2) { _, _ -> }
    }

    fun validateVerifyEmailRequest(request: Authentication.VerifyEmailRequest): EitherNel<ValidationIssue, Unit> =
        either {
            ensureFieldNonBlank("token", request.token)
        }.toEitherNel()

    fun validateLoginRequest(request: Authentication.LoginRequest): EitherNel<ValidationIssue, Unit> = either {
        ensureEmailValidity(request.email)
        ensureFieldNonBlank("password", request.password)
    }.toEitherNel()
}
