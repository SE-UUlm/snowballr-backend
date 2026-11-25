package se.uulm.snowballr.backend.model.exception.alreadyexists

import se.uulm.snowballr.backend.model.exception.AlreadyExistsException
import java.util.UUID

/**
 * Represents an exception that occurs when a user attempts to review a project paper that has already been reviewed by
 * this user.
 *
 * @param projectId The ID of the project the reviewed paper belongs to.
 * @param paperId The ID of the paper that has already been reviewed in the project.
 * @param userId The ID of the user who has already reviewed the project paper.
 */
class DuplicateReviewException(
    projectId: UUID,
    paperId: UUID,
    userId: UUID,
) : AlreadyExistsException(
    "Paper with ID '$paperId' in project with ID '$projectId' was already reviewed by user with ID '$userId'.",
)
