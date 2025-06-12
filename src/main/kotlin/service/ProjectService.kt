package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import snowballr.ProjectOuterClass

interface IProjectService {
    suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project
}

/**
 * The [ProjectService] class handles operations related to projects by implementing the [IProjectService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of project CRUD operations,
 * delegating the actual persistence operations to the [IProjectTableRepo] repository.
 *
 * @constructor Initializes the [ProjectService] with a project repository.
 * @param repo The repository responsible for managing persistence operations for projects.
 */
class ProjectService(
    private val repo: IProjectTableRepo,
) : IProjectService {
    override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project =
        // TODO: remove dummy user when user management is implemented
        repo.createProject(request, dummyUserId!!)
}
