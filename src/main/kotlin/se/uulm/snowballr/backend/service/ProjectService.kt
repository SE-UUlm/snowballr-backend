package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import snowballr.Base
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.ProjectOuterClass.Project as GrpcProject

interface IProjectService {
    /**
     * Service implementation of [SnowballRService.createProject].
     */
    suspend fun createProject(request: GrpcProject.Create): GrpcProject

    /**
     * Service implementation of [SnowballRService.getAllProjects].
     */
    suspend fun getAllProjects(): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllProjectsForUser]
     */
    suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllArchivedProjectsForUser]
     */
    suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List

    /**
     * Service implementation of [SnowballRService.getAllDeletedProjectsForUser]
     */
    suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List
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
 */
class ProjectService(
    private val repo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
) : IProjectService {
    private fun toGrpcProjects(projects: List<Project>): GrpcProject.List {
        val builder = GrpcProject.List.newBuilder()
        projects.forEach { builder.addProjects(it.toGrpcProject()) }
        return builder.build()
    }

    override suspend fun createProject(request: GrpcProject.Create): GrpcProject {
        // TODO: remove dummy user when user management is implemented
        val requestingUserId = parseUUID(dummyUserId!!, EntityType.USER)
        return repo.createProject(request, requestingUserId).toGrpcProject()
    }

    override suspend fun getAllProjects(): GrpcProject.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())

        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.PROJECT, AccessType.READ, it) }

        val projects = repo.getAllProjects()
        return toGrpcProjects(projects)
    }

    override suspend fun getAllProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val userProjects = repo.getUserProjects(requestedUserId)
        return toGrpcProjects(userProjects)
    }

    override suspend fun getAllArchivedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val archivedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_ARCHIVED))
        return toGrpcProjects(archivedUserProjects)
    }

    override suspend fun getAllDeletedProjectsForUser(request: Base.Id): GrpcProject.List {
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        authorizeAccessTo(requestedUserId, userRepo, AccessType.READ)

        val deletedUserProjects = repo.getUserProjects(requestedUserId, setOf(ProjectStatus.PROJECT_STATUS_DELETED))
        return toGrpcProjects(deletedUserProjects)
    }
}
