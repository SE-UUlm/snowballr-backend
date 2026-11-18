package se.uulm.snowballr.backend.rest.controllers

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import se.uulm.snowballr.backend.rest.onRequest
import se.uulm.snowballr.backend.service.IReviewService
import java.util.UUID

/**
 * No single class-level base route: [GetReviewById][IReviewService.getReviewById] is keyed only by the review's own
 * ID (case (a) exception, see GRPC_TO_REST_MAPPING.md), while the rest nest under a project paper's reviews.
 */
@RestController
class ReviewController(private val reviewService: IReviewService) {
    @GetMapping("${Routes.REVIEWS_ROUTE}/{id}")
    fun getReviewById(@PathVariable id: UUID): ReviewResponse = onRequest { reviewService.getReviewById(id) }

    @GetMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}/reviews")
    fun getAllReviewsForProjectPaper(@PathVariable id: UUID): List<ReviewResponse> = onRequest {
        reviewService.getAllReviewsForProjectPaper(id)
    }

    @PostMapping("${Routes.PROJECT_PAPERS_ROUTE}/{id}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    fun createReview(@PathVariable id: UUID, @RequestBody request: CreateReviewRequest): ReviewResponse = onRequest {
        reviewService.createReview(request.copy(projectPaperId = id))
    }
}
