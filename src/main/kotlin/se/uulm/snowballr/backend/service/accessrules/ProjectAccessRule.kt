@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

/**
 * Represents an [AccessRule] to a project entity.
 */
fun interface ProjectAccessRule : AccessRule<Project>

/**
 * Check whether the requesting user is a member of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve a project member list.
 */
@CheckReturnValue
private fun isProjectMember(projectMemberRepo: IProjectMemberTableRepo) = UUIDAccessRule { requester, targetId ->
    val projectMembers = projectMemberRepo.getProjectMembers(targetId)
    projectMembers.any { it.userId == requester.id }
}

/**
 * Check whether the requesting user is an admin of a specific project.
 *
 * @param projectMemberRepo The project member repository used to retrieve a project admins list.
 */
@CheckReturnValue
private fun isProjectAdmin(projectMemberRepo: IProjectMemberTableRepo) = UUIDAccessRule { requester, targetId ->
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
        .orElse(isServerAdmin.forTarget())
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
 */
@CheckReturnValue
fun isServerOrProjectAdmin(projectMemberRepo: IProjectMemberTableRepo, accessType: AccessType): AccessRule<UUID> {
    return isProjectAdmin(projectMemberRepo)
        .orElse(isServerAdmin.forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedException.Single(EntityType.PROJECT, targetId.toString(), accessType, currentUser.id.toString())
        }
}
