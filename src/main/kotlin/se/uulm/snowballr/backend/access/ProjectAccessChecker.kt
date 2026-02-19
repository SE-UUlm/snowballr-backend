package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.access.rules.andAlso
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.forTarget
import se.uulm.snowballr.backend.access.rules.isServerAdmin
import se.uulm.snowballr.backend.access.rules.isServerAdminOrSameUser
import se.uulm.snowballr.backend.access.rules.orElse
import se.uulm.snowballr.backend.access.rules.orElseThrow
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.isActive
import se.uulm.snowballr.backend.model.dto.isDeleted
import se.uulm.snowballr.backend.model.dto.isServerAdmin
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedExceptionFactory
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

interface IProjectAccessChecker {
    /**
     * Checks whether the current user is allowed to read a specific project.
     *
     * Conditions:
     * - The project exists
     * - The user is a member of the project **OR** a server admin.
     *
     * @throws ProjectNotFoundException if the project does not exist.
     * @throws UnauthorizedReadException if the user is not allowed to read the project.
     */
    suspend fun isAllowedToReadProject(currentUser: User, projectId: UUID)

    /**
     * Checks whether the current user is allowed to read user projects pf the specified user.
     *
     * A user project is defined as a project of which the user is a member.
     *
     * Conditions:
     * - The user is a server admin **OR** the same user
     *
     * @throws UnauthorizedReadException if the user is not allowed to read the user projects.
     */
    suspend fun isAllowedToReadUserProjects(currentUser: User, userId: UUID)

    /**
     * Checks whether the current user is allowed to read all projects.
     *
     * Even if the check fails, the user may still be allowed to read specific projects. This check is about reading all
     * stored projects.
     *
     * Conditions:
     * - The user is a server admin
     *
     * @throws UnauthorizedReadAllException if the user is not allowed to read all projects.
     */
    suspend fun isAllowedToReadAllProjects(currentUser: User)

    /**
     * Checks whether the target user is not the last project admin.
     *
     * Conditions:
     * - The number of project members is not one
     * - The last project member is not the target user
     *
     * @param targetUser The user for whom the access check is being performed.
     * @param projectId The ID of the project in which the user shouldn't be the last project admin.
     * @param action The action that is being performed and wherefore the user must not be the last project admin.
     * @throws FailedPreconditionException if the conditions are not met, i.e, the target user is the last project
     * admin.
     */
    suspend fun isNotLastProjectAdmin(targetUser: User, projectId: UUID, action: String)

    /**
     * Checks whether the current user is a project or server admin.
     *
     * Conditions:
     * - The user is a project admin **OR** the user is a server admin
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project in which the user might be a project admin.
     * @param accessType The type of the access check.
     * @throws UnauthorizedException if the user is neither a project nor a server admin.
     */
    suspend fun isProjectOrServerAdmin(currentUser: User, projectId: UUID, accessType: AccessType)

    /**
     * Check whether a project with the given ID exists; otherwise, throws a [ProjectNotFoundException].
     *
     * A project is considered existent if it exists in the database and is not deleted, unless the requesting user is a
     * server admin.
     */
    @CheckReturnValue
    fun isProjectExistent(): AccessRule<UUID>

    /**
     * Check whether a project with the given ID is active (or active, but settings are locked); otherwise, throws an
     * [EntityNotActiveException].
     */
    @CheckReturnValue
    fun isProjectActiveById(): AccessRule<UUID>

    /**
     * Check whether the current user is an admin of the specified project. If the user is not a project admin, the user
     * has to be a server admin; otherwise, an [UnauthorizedException] is thrown.
     *
     * @param accessType The type of access that is being checked.
     * @param entityType The type of the entity for which access is being checked, used for error reporting in case of
     * an unauthorized access attempt. Defaults to [EntityType.PROJECT].
     */
    @CheckReturnValue
    fun isProjectOrServerAdmin(accessType: AccessType, entityType: EntityType = EntityType.PROJECT): AccessRule<UUID>
}

class ProjectAccessChecker(
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IProjectAccessChecker {
    override suspend fun isAllowedToReadProject(currentUser: User, projectId: UUID) {
        isProjectExistent()
            .andAlso(isProjectMember())
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, targetId ->
                UnauthorizedReadException(user.id, targetId, EntityType.PROJECT)
            }
            .checkFor(currentUser, projectId)
    }

    override suspend fun isAllowedToReadUserProjects(currentUser: User, userId: UUID) {
        isServerAdminOrSameUser()
            .orElseThrow { requestingUser, targetId ->
                UnauthorizedReadException(requestingUser.id, targetId, EntityType.USER)
            }
            .checkFor(currentUser, userId)
    }

    override suspend fun isAllowedToReadAllProjects(currentUser: User) {
        isServerAdmin()
            .orElseThrow(UnauthorizedReadAllException(currentUser.id, EntityType.PROJECT))
            .checkFor(currentUser)
    }

    override suspend fun isNotLastProjectAdmin(targetUser: User, projectId: UUID, action: String) {
        AccessRule<UUID> { user, targetId ->
            val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
            !(projectAdmins.size == 1 && projectAdmins.first().userId == user.id)
        }.orElseThrow { user, id ->
            val userId = user.id
            FailedPreconditionException(
                "$action, because the user with the ID '$userId' is the last admin of the project with the ID '$id'.",
            )
        }
            .checkFor(targetUser, projectId)
    }

    override suspend fun isProjectOrServerAdmin(currentUser: User, projectId: UUID, accessType: AccessType) {
        isProjectOrServerAdmin(accessType, EntityType.PROJECT).checkFor(currentUser, projectId)
    }

    override fun isProjectExistent() = AccessRule<UUID> { user, projectId ->
        val project = projectRepo.getProjectById(projectId).getOrNull()
        project != null && (!project.isDeleted() || user.isServerAdmin())
    }.orElseThrow { _, projectId -> ProjectNotFoundException(projectId) }

    override fun isProjectActiveById() = AccessRule<UUID> { _, projectId ->
        val project = projectRepo.getProjectById(projectId).getOrNull()
        project != null && project.isActive()
    }.orElseThrow { _, projectId -> EntityNotActiveException(EntityType.PROJECT, projectId) }

    override fun isProjectOrServerAdmin(accessType: AccessType, entityType: EntityType) = isProjectAdmin()
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, targetId ->
            UnauthorizedExceptionFactory.createForAccessType(accessType, currentUser.id, targetId, entityType)
        }

    /**
     * Check whether the requesting user is a member of a specific project.
     */
    @CheckReturnValue
    private fun isProjectMember() = AccessRule<UUID> { requester, targetId ->
        projectMemberRepo.isProjectMember(targetId, requester.id)
    }

    /**
     * Check whether the requesting user is an admin of a specific project.
     */
    @CheckReturnValue
    private fun isProjectAdmin() = AccessRule<UUID> { requester, targetId ->
        val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
        projectAdmins.any { it.userId == requester.id }
    }
}
