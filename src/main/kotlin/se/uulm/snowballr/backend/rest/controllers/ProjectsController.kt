package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.incoming.project.CreateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.outgoing.project.ProjectDecisionStatistics
import se.uulm.snowballr.backend.model.outgoing.project.ProjectInformation
import se.uulm.snowballr.backend.model.outgoing.project.ProjectResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IProjectService
import java.util.UUID

@RestController
@RequestMapping(Routes.PROJECTS_ROUTE)
class ProjectsController(private val projectService: IProjectService) {
    @GetMapping
    fun getAllProjects(
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) status: ProjectStatus?,
    ): List<ProjectResponse> = onRequest {
        when {
            userId == null -> projectService.getAllProjects()
            status == ProjectStatus.ARCHIVED -> projectService.getAllArchivedProjectsForUser(userId)
            status == ProjectStatus.DELETED -> projectService.getAllDeletedProjectsForUser(userId)
            else -> projectService.getAllProjectsForUser(userId)
        }
    }

    @GetMapping("/{id}")
    fun getProject(@PathVariable id: UUID): ProjectResponse = onRequest { projectService.getProjectById(id) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createProject(@RequestBody request: CreateProjectRequest): ProjectResponse = onRequest {
        projectService.createProject(request)
    }

    @PutMapping("/{id}")
    fun updateProject(@PathVariable id: UUID, @RequestBody request: UpdateProjectRequest): ProjectResponse =
        onRequest {
            projectService.updateProject(request.copy(projectId = id), FULL_UPDATE_PATHS)
        }

    @DeleteMapping("/{id}")
    fun deleteProject(@PathVariable id: UUID) {
        onRequest {
            projectService.softDeleteProject(id)
        }
    }

    @GetMapping("/{id}/information")
    fun getProjectInformation(@PathVariable id: UUID): ProjectInformation = onRequest {
        // Empty paths means "no mask", i.e. every field is included - see ProjectService.getProjectInformation.
        projectService.getProjectInformation(id, emptyList())
    }

    @GetMapping("/{id}/stages/{stage}/decision-statistics")
    fun getDecisionStatisticsForStage(@PathVariable id: UUID, @PathVariable stage: Int): ProjectDecisionStatistics =
        onRequest { projectService.getDecisionStatisticsForStage(id, stage) }

    private companion object {
        // Every field of UpdateProjectRequest, so a REST PUT always behaves as a full replace
        // instead of the partial field-mask updates the underlying service also supports.
        val FULL_UPDATE_PATHS = setOf(
            "project.name",
            "project.status",
            "project.settings.similarity_threshold",
            "project.settings.snowballing_type",
            "project.settings.review_maybe_allowed",
            "project.settings.fetchers",
            "project.settings.decision_matrix.number_of_reviewers",
            "project.settings.decision_matrix.patterns",
        )
    }
}
