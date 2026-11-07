package se.uulm.snowballr.backend.rest.controllers

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.fetcher.FetcherInformationWithId
import se.uulm.snowballr.backend.model.outgoing.paper.FetcherPaperResponse
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IFetcherService
import java.util.UUID

/**
 * No single class-level base route: `GetAvailableFetchers` is a global, non-nesting registry, while the paper
 * candidate searches nest under a project - see GRPC_TO_REST_MAPPING.md.
 */
@RestController
class FetcherController(private val fetcherService: IFetcherService) {
    @GetMapping(Routes.FETCHERS_ROUTE)
    fun getAvailableFetchers(): Set<FetcherInformationWithId> = onRequest { fetcherService.getAvailableFetchers() }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{id}/paper-candidates/local")
    fun searchLocalProjectPaperCandidates(@PathVariable id: UUID, @RequestParam query: String): List<PaperResponse> =
        onRequest { fetcherService.searchLocalProjectPaperCandidates(id, query) }

    @GetMapping("${Routes.PROJECTS_ROUTE}/{id}/paper-candidates/fetcher")
    fun searchFetcherProjectPaperCandidates(
        @PathVariable id: UUID,
        @RequestParam query: String,
    ): List<FetcherPaperResponse> = onRequest { fetcherService.searchFetcherProjectPaperCandidates(id, query) }
}
