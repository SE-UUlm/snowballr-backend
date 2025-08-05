package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.dto.toGrpcProjects
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.Project as GrpcProject

interface IProjectService {
    /**
     * Service implementation of [SnowballRService.getProjectById].
     */
    suspend fun getProjectById(request: Base.Id): GrpcProject

    /**
     * Service implementation of [SnowballRService.createProject].
     */
    suspend fun createProject(request: GrpcProject.Create): GrpcProject

    /**
     * Service implementation of [SnowballRService.getAllProjects].
     */
    suspend fun getAllProjects(): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllProjectsForUser].
     */
    suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllArchivedProjectsForUser].
     */
    suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllDeletedProjectsForUser].
     */
    suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.updateProject].
     *
     * @param request The update request containing the project details to be modified.
     * @return The updated project after the changes have been applied.
     */
    suspend fun updateProject(request: GrpcProject.Update): GrpcProject

    /**
     * Service implementation of [SnowballRService.getProjectMembers].
     */
    suspend fun getProjectMembers(request: Base.Id): GrpcProject.Member.List
}

/**
 * The [ProjectService] class handles operations related to projects by implementing the [IProjectService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of project CRUD operations,
 * delegating the actual persistence operations to the [IProjectTableRepo] repository.
 *
 * @constructor Initializes the [ProjectService] with a project repository.
 * @param repo The repository responsible for managing persistence operations for projects.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 */
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IProjectService {
    override suspend fun getProjectById(request: Base.Id): GrpcProject {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectId = parseUUID(request.id, EntityType.PROJECT)
        val isInProject = projectMemberRepo.getProjectMembers(projectId)
            .any { it.userId == currentUser.id }

        if (!isInProject) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        return repo.getProjectById(projectId).toGrpcProject()
    }

    override suspend fun createProject(request: GrpcProject.Create): GrpcProject =
        repo.createProject(request, GrpcContext.getUserIdFromContext()).toGrpcProject()

    override suspend fun getAllProjects(): GrpcProject.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())

        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.PROJECT, AccessType.READ, it) }

        val projects = repo.getAllProjects()
        return projects.toGrpcProjects()
    }

    override suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val userProjects = repo.getUserProjects(requestedUserId)
        return userProjects.toGrpcProjects()
    }

    override suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val archivedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))
        return archivedUserProjects.toGrpcProjects()
    }

    override suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val deletedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))
        return deletedUserProjects.toGrpcProjects()
    }

    override suspend fun updateProject(request: GrpcProject.Update): GrpcProject {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectId = parseUUID(request.project.id, EntityType.PROJECT)
        val project = repo.getProjectById(projectId)
        val isProjectAdmin = projectMemberRepo.getAllProjectAdmins(projectId).any { it.userId == currentUser.id }
        val projectStatus = project.status

        if (!isProjectAdmin) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    request.project.id,
                    AccessType.UPDATE,
                    it,
                )
            }
        }
        if (project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw SnowballRException.FailedPreconditionException(
                "The project with the id ${request.project.id} is deleted.",
            )
        }
        if (request.project.status == ProjectStatus.PROJECT_STATUS_DELETED) {
            throw SnowballRException.FailedPreconditionException(
                "The project status can not be set to deleted by an update call.",
            )
        }

        return repo.updateProject(request, projectStatus).toGrpcProject()
    }

    override suspend fun getProjectMembers(request: Base.Id): GrpcProject.Member.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectId = parseUUID(request.id, EntityType.PROJECT)
        repo.getProjectById(projectId)
        val projectMembersWithUsers = projectMemberRepo.getProjectMembersWithUsers(projectId)

        if (!projectMembersWithUsers.any { it.user.id == currentUser.id }) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }
        return projectMembersWithUsers.toGrpcProjectMembers()
    }
}
