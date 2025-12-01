@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedExceptionFactory
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.ProjectOuterClass.ProjectStatus
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Check whether a project with the given ID exists; otherwise, throws a [NotFoundException].
 *
 * @param projectRepo The repository used to verify project existence.
 */
@CheckReturnValue
fun isProjectExistent(projectRepo: IProjectTableRepo): AccessRule<UUID> {
    return AccessRule<UUID> { _, projectId ->
        projectRepo.doesProjectExistById(projectId)
    }.orElseThrow { _, projectId ->
        ProjectNotFoundException(projectId)
    }
}

/**
 * Check whether the project is active (or active, but settings are locked);
 * otherwise, throws an [EntityNotActiveException].
 */
@CheckReturnValue
fun isProjectActive(): AccessRule<Project> {
    return AccessRule<Project> { _, project ->
        project.status == ProjectStatus.PROJECT_STATUS_ACTIVE ||
            project.status == ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
    }.orElseThrow { _, project -> EntityNotActiveException(EntityType.PROJECT, project.id) }
}

/**
 * Check whether the requesting user is a member of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve a project member list.
 */
@CheckReturnValue
fun isProjectMember(projectMemberRepo: IProjectMemberTableRepo) = AccessRule<UUID> { requester, targetId ->
    val projectMembers = projectMemberRepo.getProjectMembers(targetId)
    projectMembers.any { it.userId == requester.id }
}

/**
 * Check whether the requesting user is an admin of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve a project admins list.
 */
@CheckReturnValue
fun isProjectAdmin(projectMemberRepo: IProjectMemberTableRepo) = AccessRule<UUID> { requester, targetId ->
    val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
    projectAdmins.any { it.userId == requester.id }
}

/**
 * Check whether the user is not the last project admin of a specific project; otherwise, throws a [FailedPreconditionException].
 *
 * @param projectMemberRepo The project member repository used to retrieve a project admins list.
 * @param action The action that is being performed and wherefore the user must not be the last project admin.
 */
@CheckReturnValue
fun isNotLastProjectAdmin(projectMemberRepo: IProjectMemberTableRepo, action: String): AccessRule<UUID> {
    return AccessRule<UUID> { user, targetId ->
        val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
        !(projectAdmins.size == 1 && projectAdmins.first().userId == user.id)
    }.orElseThrow { user, projectId ->
        FailedPreconditionException(
            "$action, because the user with the ID '${user.id}' " +
                "is the last admin of the project with the ID '$projectId'.",
        )
    }
}

/**
 * Check whether the current user is a member of the specified project. If the user is not a project member,
 * the user has to be a server admin; otherwise, throws an [UnauthorizedException].
 *
 * @param projectMemberRepo The repository used to access project membership data.
 */
@CheckReturnValue
fun isAllowedToReadProject(projectMemberRepo: IProjectMemberTableRepo): AccessRule<UUID> {
    return isProjectMember(projectMemberRepo)
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedReadException(currentUser.id, targetId, EntityType.PROJECT)
        }
}

/**
 * Check whether the current user is an admin of the specified project. If the user is not a project admin,
 * the user has to be a server admin; otherwise, throws an [UnauthorizedException].
 *
 * @param projectMemberRepo The repository used to access project membership data.
 * @param accessType The type of access that is being checked.
 */
@CheckReturnValue
fun isServerOrProjectAdmin(projectMemberRepo: IProjectMemberTableRepo, accessType: AccessType): AccessRule<UUID> {
    return isProjectAdmin(projectMemberRepo)
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedExceptionFactory.createForAccessType(accessType, currentUser.id, targetId, EntityType.PROJECT)
        }
}
