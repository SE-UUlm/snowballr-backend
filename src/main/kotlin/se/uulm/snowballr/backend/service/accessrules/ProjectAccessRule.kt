@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Project
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
        NotFoundException(EntityType.PROJECT, projectId.toString())
    }
}

/**
 * Check whether the project is active (or active, but settings are locked).
 */
@CheckReturnValue
fun isProjectActive() = AccessRule<Project> { _, project ->
    project.status == ProjectStatus.PROJECT_STATUS_ACTIVE ||
        project.status == ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED
}

/**
 * Check whether the requesting user is a member of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve a project member list.
 */
@CheckReturnValue
private fun isProjectMember(projectMemberRepo: IProjectMemberTableRepo) = AccessRule<UUID> { requester, targetId ->
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
 * Check whether the current user is a member of the specified project. If the user is not a project member,
 * the user has to be a server admin; otherwise, throws an [UnauthorizedException.Single].
 *
 * @param projectMemberRepo The repository used to access project membership data.
 */
@CheckReturnValue
fun isAllowedToReadProject(projectMemberRepo: IProjectMemberTableRepo): AccessRule<UUID> {
    return isProjectMember(projectMemberRepo)
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedException.Single(
                EntityType.PROJECT,
                targetId.toString(),
                AccessType.READ,
                currentUser.id.toString(),
            )
        }
}

/**
 * Check whether the current user is an admin of the specified project. If the user is not a project admin,
 * the user has to be a server admin; otherwise, throws an [UnauthorizedException.Single].
 *
 * @param projectMemberRepo The repository used to access project membership data.
 * @param accessType The type of access that is being checked.
 */
@CheckReturnValue
fun isServerOrProjectAdmin(projectMemberRepo: IProjectMemberTableRepo, accessType: AccessType): AccessRule<UUID> {
    return isProjectAdmin(projectMemberRepo)
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedException.Single(EntityType.PROJECT, targetId.toString(), accessType, currentUser.id.toString())
        }
}
