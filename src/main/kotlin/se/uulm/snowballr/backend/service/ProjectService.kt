package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcProject
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole

interface IProjectService {
    /**
     * Service implementation of [SnowballRService.createProject].
     */
    suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project

    /**
     * Service implementation of [SnowballRService.getAllProjects].
     */
    suspend fun getAllProjects(): ProjectOuterClass.Project.List
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
    override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project {
        // TODO: remove dummy user when user management is implemented
        val requestingUserId = parseUUID(dummyUserId!!, "user")
        return repo.createProject(request, requestingUserId).toGrpcProject()
    }

    override suspend fun getAllProjects(): ProjectOuterClass.Project.List {
        val requestingUserId = parseUUID(dummyUserId!!, "user")
        val currentUser = userRepo.getUserById(requestingUserId)
        // Check whether the current user has access to retrieve all projects
        // TODO: remove dummy user when user management is implemented
        if (currentUser.role != UserRole.USER_ROLE_ADMIN) {
            throw UnauthorizedException.All.Project(dummyUserId!!)
        }

        val projects = repo.getAllProjects()

        val builder = ProjectOuterClass.Project.List.newBuilder()
        projects.forEach { builder.addProjects(it.toGrpcProject()) }
        return builder.build()
    }
}
