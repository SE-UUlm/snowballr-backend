@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.isActive
import se.uulm.snowballr.backend.model.dto.isDeleted
import se.uulm.snowballr.backend.model.dto.isServerAdmin
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedExceptionFactory
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

interface IProjectAccessChecker {
    /**
     * Check whether a project with the given ID exists; otherwise, throws a [ProjectNotFoundException].
     *
     * A project is considered existent if it exists in the database and is not deleted, unless the requesting user is a
     * server admin.
     */
    @CheckReturnValue
    fun isProjectExistent(): AccessRule<UUID>

    /**
     * Check whether the project is active (or active, but settings are locked); otherwise, throws an
     * [EntityNotActiveException].
     */
    @CheckReturnValue
    fun isProjectActive(): AccessRule<Project>

    /**
     * Check whether the requesting user is a member of a specific project.
     */
    @CheckReturnValue
    fun isProjectMember(): AccessRule<UUID>

    /**
     * Check whether the requesting user is an admin of a specific project.
     */
    @CheckReturnValue
    fun isProjectAdmin(): AccessRule<UUID>

    /**
     * Check whether the user is not the last project admin of a specific project; otherwise, throws a
     * [FailedPreconditionException].
     *
     * @param action The action that is being performed and wherefore the user must not be the last project admin.
     */
    @CheckReturnValue
    fun isNotLastProjectAdmin(action: String): AccessRule<UUID>

    /**
     * Check whether the current user is allowed to read the specified project.
     *
     * The user can read the project if the following conditions are met:
     * 1. The project exists according to [isProjectExistent]; otherwise a [ProjectNotFoundException] is thrown.
     * 2. The user is a member of the project according to [isProjectMember] or a server admin according to
     * [isServerAdmin].
     *
     * If neither condition is met, an [UnauthorizedReadException] is thrown.
     */
    @CheckReturnValue
    fun isAllowedToReadProject(): AccessRule<UUID>

    /**
     * Check whether the current user is an admin of the specified project. If the user is not a project admin, the user
     * has to be a server admin; otherwise, an [UnauthorizedException] is thrown.
     *
     * @param accessType The type of access that is being checked.
     */
    @CheckReturnValue
    fun isServerOrProjectAdmin(accessType: AccessType): AccessRule<UUID>
}

class ProjectAccessChecker(
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IProjectAccessChecker {
    override fun isProjectExistent() = AccessRule<UUID> { user, projectId ->
        val project = projectRepo.getProjectById(projectId).getOrNull()
        project != null && (!project.isDeleted() || user.isServerAdmin())
    }.orElseThrow { _, projectId -> ProjectNotFoundException(projectId) }

    override fun isProjectActive() = AccessRule<Project> { _, project ->
        project.isActive()
    }.orElseThrow { _, project -> EntityNotActiveException(EntityType.PROJECT, project.id) }

    override fun isProjectMember() = AccessRule<UUID> { requester, targetId ->
        val projectMembers = projectMemberRepo.getProjectMembers(targetId)
        projectMembers.any { it.userId == requester.id }
    }

    override fun isProjectAdmin() = AccessRule<UUID> { requester, targetId ->
        val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
        projectAdmins.any { it.userId == requester.id }
    }

    override fun isNotLastProjectAdmin(action: String) = AccessRule<UUID> { user, targetId ->
        val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
        !(projectAdmins.size == 1 && projectAdmins.first().userId == user.id)
    }.orElseThrow { user, projectId ->
        FailedPreconditionException(
            "$action, because the user with the ID '${user.id}' is the last admin of the project with the ID " +
                "'$projectId'.",
        )
    }

    override fun isAllowedToReadProject() = isProjectExistent()
        .andAlso(isProjectMember())
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedReadException(currentUser.id, targetId, EntityType.PROJECT)
        }

    override fun isServerOrProjectAdmin(accessType: AccessType) = isProjectAdmin()
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedExceptionFactory.createForAccessType(
                accessType,
                currentUser.id,
                targetId,
                EntityType.PROJECT,
            )
        }
}
