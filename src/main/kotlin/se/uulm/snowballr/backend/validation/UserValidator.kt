package se.uulm.snowballr.backend.validation

import arrow.core.Either
import arrow.core.EitherNel
import arrow.core.raise.either
import arrow.core.raise.zipOrAccumulate
import se.uulm.snowballr.backend.model.ValidationIssue
import snowballr.UserOuterClass.User

/**
 * A validator for [User] related requests.
 *
 * @see AuthenticationValidator for more validation functions regarding the [User]
 */
object UserValidator {
    fun validateUpdateRequest(request: User.Update): EitherNel<ValidationIssue, Unit> = either {
        // Validate the field mask
        val fieldMaskResult = either {
            ensureFieldMaskIsValid(request.mask, User.Update.getDescriptor())
        }

        // If field mask validation fails, return early
        if (fieldMaskResult is Either.Left) {
            fieldMaskResult.toEitherNel().bind()
        }

        // Only proceed with field validation if the mask is valid
        val selectedFields = request.mask.pathsList.toSet()

        zipOrAccumulate(
            { ensureIdValidity("id", request.user.id) },
            {
                if ("user.email" in selectedFields) {
                    ensureEmailValidity(request.user.email)
                }
            },
            {
                if ("user.first_name" in selectedFields) {
                    ensureFirstNameValidity(request.user.firstName)
                }
            },
            {
                if ("user.last_name" in selectedFields) {
                    ensureLastNameValidity(request.user.lastName)
                }
            },
            {
                if ("user.role" in selectedFields) {
                    ensureEnumNotUnspecified("role", request.user.role)
                }
            },
        ) { _, _, _, _, _ -> }
    }
}
