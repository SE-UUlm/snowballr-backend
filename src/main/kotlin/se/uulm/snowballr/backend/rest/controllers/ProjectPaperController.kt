package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.projectpaper.AddPaperToProjectRequest
import se.uulm.snowballr.backend.model.outgoing.projectpaper.ProjectPaperResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IProjectPaperService
import java.util.UUID

/**
 * No single class-level base route. `GetNextPaper`/`GetNextPaperToReview`/`GetPreviousPaper`/`GetProjectPaperById`
 * only ever receive a project-paper ID in the proto (never a project ID), so they stay under the flat
 * [Routes.PROJECT_PAPERS_ROUTE]; everything else nests under [Routes.PROJECTS_ROUTE] as usual. See
 * GRPC_TO_REST_MAPPING.md for the full reasoning, including the more RESTful nested alternative left for later.
 */
@RestController
class ProjectPaperController(private val projectPaperService: IProjectPaperService) {
    @GetMapping("${Routes.PROJECTS_ROUTE}/{projectId}/papers-to-review")
    fun getPapersToReviewForProject(@PathVariable projectId: UUID): List<ProjectPaperResponse> = onRequest {
        projectPaperService.getPapersToReviewForProject(projectId)
    }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{projectId}/papers")
    fun getAllProjectPapersForProject(@PathVariable projectId: UUID): List<ProjectPaperResponse> = onRequest {
        projectPaperService.getAllProjectPapersForProject(projectId)
    }

    @PostMapping("${Routes.PROJECTS_ROUTE}/{projectId}/papers")
    @ResponseStatus(HttpStatus.CREATED)
    fun addPaperToProject(
        @PathVariable projectId: UUID,
        @RequestBody request: AddPaperToProjectRequest,
    ): ProjectPaperResponse = onRequest {
        projectPaperService.addPaperToProject(projectId, request.paperId, request.stage)
    }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{projectId}/papers/{relativeId}")
    fun getProjectPaperByRelativeId(
        @PathVariable projectId: UUID,
        @PathVariable relativeId: Int,
    ): ProjectPaperResponse = onRequest { projectPaperService.getProjectPaperByRelativeId(projectId, relativeId) }

    @GetMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}")
    fun getProjectPaperById(@PathVariable id: UUID): ProjectPaperResponse = onRequest {
        projectPaperService.getProjectPaperById(id)
    }

    @GetMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}/next")
    fun getNextPaper(@PathVariable id: UUID): ProjectPaperResponse = onRequest {
        projectPaperService.getNextPaper(id)
    }

    @GetMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}/next-to-review")
    fun getNextPaperToReview(@PathVariable id: UUID): ProjectPaperResponse = onRequest {
        projectPaperService.getNextPaperToReview(id)
    }

    @GetMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}/previous")
    fun getPreviousPaper(@PathVariable id: UUID): ProjectPaperResponse = onRequest {
        projectPaperService.getPreviousPaper(id)
    }
}
