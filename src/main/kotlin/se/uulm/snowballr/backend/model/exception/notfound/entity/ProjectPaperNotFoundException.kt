package se.uulm.snowballr.backend.model.exception.notfound.entity

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.exception.notfound.EntityNotFoundException
import java.util.UUID

/**
 * Represents an exception that occurs when a project paper could not be found within the context of a specific project.
 */
open class ProjectPaperNotFoundException : EntityNotFoundException {
    /**
     * @param localProjectPaperId The project-local ID of the missing project paper.
     * @param projectId The ID of the project in which the project paper could not be found.
     */
    constructor(localProjectPaperId: Int, projectId: UUID) : super(
        EntityType.PROJECT_PAPER,
        localProjectPaperId,
        identifierType = IdentifierType.LOCAL_ID,
        location = " in the project with ID '$projectId'",
    )

    /**
     * @param paperId The paper ID of the missing project paper.
     * @param projectId The ID of the project in which the project paper could not be found.
     */
    constructor(paperId: UUID, projectId: UUID) : super(
        EntityType.PROJECT_PAPER,
        paperId,
        identifierType = IdentifierType.LOCAL_ID,
        location = " in the project with ID '$projectId'",
    )
}
