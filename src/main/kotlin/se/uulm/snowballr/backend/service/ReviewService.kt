package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateReviewException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.dto.toGrpcReviews
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadReview
import se.uulm.snowballr.backend.service.accessrules.isProjectActive
import snowballr.Base
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ReviewOuterClass
import java.util.UUID
import snowballr.ReviewOuterClass.Review as GrpcReview

interface IReviewService {
    /**
     * Service implementation of [SnowballRService.getReviewById].
     */
    suspend fun getReviewById(request: Base.Id): GrpcReview

    /**
     * Service implementation of [SnowballRService.getAllReviewsForProjectPaper].
     */
    suspend fun getAllReviewsForProjectPaper(request: Base.Id): GrpcReview.List

    /**
     * Service implementation of [SnowballRService.createReview].
     */
    suspend fun createReview(request: GrpcReview.Create): GrpcReview
}

/**
 * The [ReviewService] class handles operations related to projects by implementing the [IReviewService] interface.
 *
 * The `Review` class provides functionality for managing reviews, including
 * creating, retrieving, and updating them. It also handles validation of access permissions,
 * preconditions, and interactions with underlying repositories.
 *
 * This service ensures that all operations related to reviews are performed
 * in accordance with the project and user access rules.
 *
 * @param repo Interface for persistence and retrieval operations related to reviews.
 * @param userRepo Interface for persistence and retrieval operations related to users.
 * @param projectPaperRepo Interface for persistence and retrieval operations related to project papers.
 * @param projectRepo Interface for persistence and retrieval operations related to projects.
 * @param projectMemberRepo Interface for persistence and retrieval operations related to project members.
 * @param reviewHasCriterionRepo Interface for persistence and retrieval operations related to review criteria.
 */
class ReviewService(
    private val repo: IReviewTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val reviewHasCriterionRepo: IReviewHasCriterionTableRepo,
) : IReviewService {
    override suspend fun getReviewById(request: Base.Id): GrpcReview = withUser(userRepo) { currentUser ->
        val reviewId = parseUUID(request.id, EntityType.REVIEW)

        val review = repo.getReviewById(reviewId).getOrThrow()

        isAllowedToReadReview(projectMemberRepo, projectPaperRepo).checkFor(currentUser, review)

        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(reviewId)
        review.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
    }

    override suspend fun getAllReviewsForProjectPaper(request: Base.Id): GrpcReview.List =
        withUser(userRepo) { currentUser ->
            val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
            val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

            isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

            val reviews = repo.getAllReviewsForProjectPaper(projectPaperId)
            val reviewSelectedCriteriaMap = mutableMapOf<Review, List<String>>()
            for (review in reviews) {
                reviewSelectedCriteriaMap[review] = reviewHasCriterionRepo
                    .getSelectedCriteriaIdsForReviewById(review.id).map(UUID::toString)
            }

            reviews.toGrpcReviews(reviewSelectedCriteriaMap)
        }

    /**
     * Determines the final paper decision based on the given list of reviews.
     *
     * At the moment, this is a simple sum function that adds one for each accepted review, subtracts one for each rejected,
     * and leaves the sum unchanged for each maybe review. If the entire sum is above 0, the paper is accepted,
     * if it is below 0, the paper is declined, if it is 0, the paper is in review. If the list is empty, the paper
     * is considered to be unreviewed. At least two reviews are required to make a final decision.
     * Exchange this by loading the decision matrix and calculating the final decision based on the matrix (see #345)
     *
     * @param projectPaperId ID of the project paper for which the final paper decision is to be determined.
     * @param reviews List of reviews to be considered for the final paper decision.
     */
    @Suppress("MagicNumber")
    private suspend fun determinePaperDecision(projectPaperId: UUID, reviews: List<Review>) {
        val requiredReviews = 2
        if (reviews.isEmpty()) {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_UNREVIEWED)
            return
        } else if (reviews.size < requiredReviews) {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_IN_REVIEW)
            return
        }

        val sum = reviews.sumOf { review ->
            when (review.decision) {
                ReviewOuterClass.ReviewDecision.REVIEW_DECISION_UNSPECIFIED -> 0
                ReviewOuterClass.ReviewDecision.REVIEW_DECISION_DECLINED -> -1
                ReviewOuterClass.ReviewDecision.REVIEW_DECISION_MAYBE -> 0
                ReviewOuterClass.ReviewDecision.REVIEW_DECISION_ACCEPTED -> 1
                ReviewOuterClass.ReviewDecision.UNRECOGNIZED -> 0
            }
        }

        if (sum > 0) {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_ACCEPTED)
        } else if (sum < 0) {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
        } else {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_IN_REVIEW)
        }
    }

    override suspend fun createReview(request: GrpcReview.Create): GrpcReview = withUser(userRepo) { currentUser ->
        val projectPaperId = parseUUID(request.projectPaperId, EntityType.PROJECT_PAPER)
        val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

        isAllowedToReadProject(projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

        val project = projectRepo.getProjectById(projectPaper.projectId).getOrThrow()
        isProjectActive().checkFor(currentUser, project)

        val reviewsForProjectPaper = repo.getAllReviewsForProjectPaper(projectPaperId)
        val hasUserAlreadyReviewed = reviewsForProjectPaper.any { review -> review.userId == currentUser.id }
        if (hasUserAlreadyReviewed) {
            throw DuplicateReviewException(projectPaperId.toString(), currentUser.id.toString())
        }

        val isPaperNotFinallyDecided = projectPaper.decision == PaperDecision.PAPER_DECISION_IN_REVIEW ||
            projectPaper.decision == PaperDecision.PAPER_DECISION_UNREVIEWED
        if (!isPaperNotFinallyDecided) {
            throw FailedPreconditionException(
                "The project paper must be either unreviewed or still in review." +
                    "Finally decided project papers cannot be reviewed anymore.",
            )
        }

        val review = repo.createReview(request, currentUser.id)
        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(review.id)

        determinePaperDecision(projectPaper.id, reviewsForProjectPaper + review)

        // TODO: (question for reviewer): Think about directly injecting the selected criteria ids instead of requesting
        // them from the repo.
        review.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
    }
}
