package se.uulm.snowballr.backend.model.exception

import io.grpc.Status

/**
 * Represents an exception that occurs when a user attempts to review a project paper that has already been reviewed
 * by this user.
 *
 * @param projectPaperId The ID of the project paper that has already been reviewed.
 * @param userId The ID of the user who has already reviewed the project paper.
 */
class DuplicateReviewException(
    projectPaperId: String,
    userId: String,
) : SnowballRException(
    Status.ALREADY_EXISTS,
    "Project paper with ID '$projectPaperId' was already reviewed by user with ID '$userId'.",
)
