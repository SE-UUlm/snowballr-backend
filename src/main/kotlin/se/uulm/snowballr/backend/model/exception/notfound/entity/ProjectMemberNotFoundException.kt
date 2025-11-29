package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a project member could not be found by the combination of their user ID and
 * the ID of the project they belong to.
 *
 * @param userId The user ID of the missing project member.
 * @param projectId The ID of the project the member was supposed to belong to.
 */
class ProjectMemberNotFoundException(
    userId: UUID,
    projectId: UUID,
) : EntityNotFoundException(
    EntityType.PROJECT_MEMBER,
    userId,
    location = " in the project with ID '$projectId'",
)
