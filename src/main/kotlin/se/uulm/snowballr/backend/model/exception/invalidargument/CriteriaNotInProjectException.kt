package se.uulm.snowballr.backend.model.exception.invalidargument

import se.uulm.snowballr.backend.model.exception.InvalidArgumentException
import java.util.UUID

/**
 * Represents an exception that occurs when criteria are referenced that do not belong to the given project, e.g. when
 * a review selects criteria of another project or a user criterion.
 *
 * @param criteriaIds The IDs of the criteria that do not belong to the project.
 * @param projectId The ID of the project.
 */
class CriteriaNotInProjectException(
    criteriaIds: Collection<UUID>,
    projectId: UUID,
) : InvalidArgumentException(
    "The criteria ${criteriaIds.joinToString()} do not belong to the project $projectId.",
)
