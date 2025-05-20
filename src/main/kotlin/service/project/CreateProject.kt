package se.uulm.snowballr.backend.service.project

import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.service.ICreateProject
import snowballr.ProjectOuterClass

/**
 * The [CreateProject] class provides an implementation for the [ICreateProject] interface.
 *
 * It serves as a wrapper around the [IProjectTableRepo] repository, delegating the responsibility
 * of creating a project to the underlying repository implementation.
 *
 * @constructor Initializes the [CreateProject] with a given project repository.
 * @param repo The repository responsible for handling persistence operations related to projects.
 */
class CreateProject(
    private val repo: IProjectTableRepo,
) : ICreateProject {
    override suspend fun createProject(request: ProjectOuterClass.Project.Create): ProjectOuterClass.Project =
        repo.createProject(request)
}
