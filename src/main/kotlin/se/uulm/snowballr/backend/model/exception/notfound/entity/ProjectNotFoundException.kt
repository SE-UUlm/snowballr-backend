package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a project could not be found by its ID.
 *
 * @param projectId The ID of the missing project.
 */
class ProjectNotFoundException(
    projectId: UUID,
) : EntityNotFoundException(EntityType.PROJECT, projectId)
