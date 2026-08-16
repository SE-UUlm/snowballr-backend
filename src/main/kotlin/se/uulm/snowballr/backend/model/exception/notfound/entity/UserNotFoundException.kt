package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a user could not be found by its ID.
 *
 * @param userId The ID of the missing user.
 */
class UserNotFoundException(
    userId: UUID,
) : EntityNotFoundException(EntityType.USER, userId)
