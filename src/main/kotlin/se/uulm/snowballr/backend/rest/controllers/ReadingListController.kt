package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.outgoing.paper.PaperResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IReadingListService
import java.util.UUID

@RestController
@RequestMapping(Routes.READING_LIST_ROUTE)
class ReadingListController(private val readingListService: IReadingListService) {
    @GetMapping
    fun getReadingList(): List<PaperResponse> = onRequest { readingListService.getReadingList() }

    @RequestMapping(value = ["/{paperId}"], method = [RequestMethod.HEAD])
    fun isPaperOnReadingList(@PathVariable paperId: UUID): ResponseEntity<Void> = onRequest {
        val onList = readingListService.isPaperOnReadingList(paperId)
        ResponseEntity.status(if (onList) HttpStatus.OK else HttpStatus.NOT_FOUND).build()
    }

    @PutMapping("/{paperId}")
    fun addPaperToReadingList(@PathVariable paperId: UUID) {
        onRequest { readingListService.addPaperToReadingList(paperId) }
    }

    @DeleteMapping("/{paperId}")
    fun removePaperFromReadingList(@PathVariable paperId: UUID) {
        onRequest { readingListService.removePaperFromReadingList(paperId) }
    }
}
