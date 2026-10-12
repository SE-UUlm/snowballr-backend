package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IProjectService
import java.util.UUID

@RestController
@RequestMapping(Routes.PROJECTS_ROUTE)
class ProjectsController(private val projectService: IProjectService) {
    @GetMapping
    fun getAllProjects(): List<ProjectResponse> = onRequest { projectService.getAllProjects() }

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: UUID): ProjectResponse = onRequest { projectService.getProjectById(id) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(@RequestBody request: CreateProjectRequest): ProjectResponse = onRequest {
        projectService.createProject(request)
    }

    @DeleteMapping("/{id}")
    fun deleteProject(@PathVariable id: UUID) {
        onRequest {
            projectService.softDeleteProject(id)
        }
    }
}
