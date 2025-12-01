package se.uulm.snowballr.backend.model.exception.alreadyexists.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateEntityException
import java.util.UUID

/**
 * Represents an exception that occurs when a project-paper with the given project and paper ID already exists.
 *
 * @param projectId The ID of the project.
 * @param paperId The ID of the paper.
 */
class DuplicateProjectPaperException(
    projectId: UUID,
    paperId: UUID,
) : DuplicateEntityException(
    EntityType.PROJECT_PAPER,
    projectId,
    paperId,
)
