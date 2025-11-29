package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a project paper could not be found within the context of a specific project.
 *
 * @param localProjectPaperId The project-local ID of the missing project paper.
 * @param projectId The ID of the project in which the project paper could not be found.
 */
open class ProjectPaperNotFoundException(
    localProjectPaperId: Long,
    projectId: UUID,
) : EntityNotFoundException(
    EntityType.PROJECT_PAPER,
    localProjectPaperId,
    identifierType = IdentifierType.LOCAL_ID,
    location = " in the project with ID '$projectId'",
)
