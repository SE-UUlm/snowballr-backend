package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Project
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
     * The user may still be allowed to read specific projects. This check is about reading all stored projects.
     *
     * Conditions:
     * - The user is a server admin
     *
     * @throws UnauthorizedReadAllException if the user is not allowed to read all projects.
     */
    suspend fun isAllowedToReadAllProjects(currentUser: User)

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
     * Check whether a project with the given ID is active (or active, but settings are locked); otherwise, throws an
     * [EntityNotActiveException].
     */
    @CheckReturnValue
    fun isProjectActiveById(): AccessRule<UUID>

    /**
     * Check whether the user is not the last project admin of a specific project; otherwise, throws a
     * [FailedPreconditionException].
     *
     * @param action The action that is being performed and wherefore the user must not be the last project admin.
     */
    @CheckReturnValue
    fun isNotLastProjectAdmin(action: String): AccessRule<UUID>

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
    private val userAccessChecker: IUserAccessChecker,
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
        userAccessChecker.isServerAdminOrSameUser()
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

    override fun isProjectExistent() = AccessRule<UUID> { user, projectId ->
        val project = projectRepo.getProjectById(projectId).getOrNull()
        project != null && (!project.isDeleted() || user.isServerAdmin())
    }.orElseThrow { _, projectId -> ProjectNotFoundException(projectId) }

    override fun isProjectActive() = AccessRule<Project> { _, project ->
        project.isActive()
    }.orElseThrow { _, project -> EntityNotActiveException(EntityType.PROJECT, project.id) }

    override fun isProjectActiveById() = AccessRule<UUID> { _, projectId ->
        val project = projectRepo.getProjectById(projectId).getOrNull()
        project != null && project.isActive()
    }.orElseThrow { _, projectId -> EntityNotActiveException(EntityType.PROJECT, projectId) }

    override fun isNotLastProjectAdmin(action: String) = AccessRule<UUID> { user, targetId ->
        val projectAdmins = projectMemberRepo.getAllProjectAdmins(targetId)
        !(projectAdmins.size == 1 && projectAdmins.first().userId == user.id)
    }.orElseThrow { user, projectId ->
        FailedPreconditionException(
            "$action, because the user with the ID '${user.id}' is the last admin of the project with the ID " +
                "'$projectId'.",
        )
    }

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
        val projectMembers = projectMemberRepo.getProjectMembers(targetId)
        projectMembers.any { it.userId == requester.id }
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
