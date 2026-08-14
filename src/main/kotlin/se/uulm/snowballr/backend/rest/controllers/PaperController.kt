package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.paper.CreatePaperRequest
import se.uulm.snowballr.backend.model.incoming.paper.UpdatePaperRequest
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IPaperService
import java.util.UUID

/**
 * Flat throughout, and not a nesting-rule exception: `Paper` has no owning resource in the domain model - papers
 * exist independently of any project - see GRPC_TO_REST_MAPPING.md.
 */
@RestController
@RequestMapping(Routes.PAPERS_ROUTE)
class PaperController(private val paperService: IPaperService) {
    @GetMapping("/{id}")
    fun getPaperById(@PathVariable id: UUID): PaperResponse = onRequest { paperService.getPaperById(id) }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createPaper(@RequestBody request: CreatePaperRequest): PaperResponse = onRequest {
        paperService.createPaper(request)
    }

    @PutMapping("/{id}")
    fun updatePaper(@PathVariable id: UUID, @RequestBody request: UpdatePaperRequest): PaperResponse = onRequest {
        paperService.updatePaper(request.copy(paperId = id), FULL_UPDATE_PATHS)
    }

    @GetMapping("/{id}/forward-references")
    fun getForwardReferencedPapers(@PathVariable id: UUID): List<PaperResponse> = onRequest {
        paperService.getForwardReferencedPapers(id)
    }

    @GetMapping("/{id}/backward-references")
    fun getBackwardReferencedPapers(@PathVariable id: UUID): List<PaperResponse> = onRequest {
        paperService.getBackwardReferencedPapers(id)
    }

    private companion object {
        // Every field of UpdatePaperRequest, so a REST PUT always behaves as a full replace
        // instead of the partial field-mask updates the underlying service also supports.
        val FULL_UPDATE_PATHS = listOf(
            "paper.title",
            "paper.abstrakt",
            "paper.year",
            "paper.publisher",
            "paper.publication_name",
            "paper.publication_type",
            "paper.authors",
            "paper.external_ids",
        )
    }
}
