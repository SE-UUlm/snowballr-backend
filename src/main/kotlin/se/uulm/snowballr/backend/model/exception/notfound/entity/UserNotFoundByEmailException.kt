package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException

/**
 * Represents an exception that occurs when a user could not be found by its email.
 *
 * @param email The email of the missing user.
 */
class UserNotFoundByEmailException(
    email: String,
) : EntityNotFoundException(EntityType.USER, email, identifierType = IdentifierType.EMAIL)
