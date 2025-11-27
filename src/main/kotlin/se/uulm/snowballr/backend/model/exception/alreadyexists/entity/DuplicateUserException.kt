package se.uulm.snowballr.backend.model.exception.alreadyexists.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateEntityException

/**
 * Represents an exception that occurs when a user with the given email already exists.
 *
 * @param userEmail The email of the duplicated user.
 */
class DuplicateUserException(
    userEmail: String,
) : DuplicateEntityException(
    EntityType.USER,
    userEmail,
    identifierType = IdentifierType.EMAIL,
)
