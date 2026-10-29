package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.dto.criterion.Criterion
import se.uulm.snowballr.backend.model.dto.criterion.CriterionField
import se.uulm.snowballr.backend.model.incoming.criterion.CreateCriterionRequest
import se.uulm.snowballr.backend.model.incoming.criterion.UpdateCriterionRequest
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.ICriterionService
import java.util.UUID

/**
 * No single class-level base route: `CreateCriterion` isn't nested under `/projects` since `projectId` is optional
 * (global vs. project-specific criteria), and `GetCriterionById`/`UpdateCriterion` are case (a) exceptions (no
 * project ID in the request) - see GRPC_TO_REST_MAPPING.md.
 */
@RestController
class CriterionController(private val criterionService: ICriterionService) {
    @GetMapping("${Routes.CRITERIA_ROUTE}/{id}")
    fun getCriterionById(@PathVariable id: UUID): Criterion = onRequest { criterionService.getCriterionById(id) }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{projectId}/criteria")
    fun getAllCriteriaForProject(@PathVariable projectId: UUID): List<Criterion.ProjectCriterion> = onRequest {
        criterionService.getAllCriteriaForProject(projectId)
    }

    @PostMapping(Routes.CRITERIA_ROUTE)
    @ResponseStatus(HttpStatus.CREATED)
    fun createCriterion(@RequestBody request: CreateCriterionRequest): Criterion = onRequest {
        criterionService.createCriterion(request)
    }

    @PutMapping("${Routes.CRITERIA_ROUTE}/{id}")
    fun updateCriterion(@PathVariable id: UUID, @RequestBody request: UpdateCriterionRequest): Criterion = onRequest {
        criterionService.updateCriterion(request.copy(criterionId = id), CriterionField.entries.toSet())
    }
}
