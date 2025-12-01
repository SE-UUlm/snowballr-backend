package se.uulm.snowballr.backend.model.exception.alreadyexists

import se.uulm.snowballr.backend.model.exception.AlreadyExistsException
import java.util.UUID

/**
 * Represents an exception that occurs when a user attempts to review a project paper that has already been reviewed by
 * this user.
 *
 * @param projectPaperId The ID of the project paper that has already been reviewed.
 * @param userId The ID of the user who has already reviewed the project paper.
 */
class DuplicateReviewException(
    projectPaperId: UUID,
    userId: UUID,
) : AlreadyExistsException(
    "Project paper with ID '$projectPaperId' was already reviewed by user with ID '$userId'.",
)
