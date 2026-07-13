package se.uulm.snowballr.backend.access.rules

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import javax.annotation.CheckReturnValue

/**
 * Check whether the project is active (according to [Project.isActive]).
 *
 * @throws EntityNotActiveException if the project is not active.
 */
@CheckReturnValue
fun isProjectActive() = AccessRule<Project> { _, project -> project.isActive }
    .orElseThrow { _, project -> EntityNotActiveException(EntityType.PROJECT, project.id) }
