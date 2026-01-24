package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.dto.toGrpcReviews
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
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
import snowballr.CriterionOuterClass.CriterionCategory
import snowballr.ProjectOuterClass.PaperDecision
import snowballr.ProjectOuterClass.ReviewDecisionMatrix
import snowballr.ReviewOuterClass.ReviewDecision
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
 * @param criteriaRepo Interface for persistence and retrieval operations related to criteria.
 * @param reviewHasCriterionRepo Interface for persistence and retrieval operations related to review-criteria relation.
 */
@Suppress("LongParameterList")
class ReviewService(
    private val repo: IReviewTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val criteriaRepo: ICriterionTableRepo,
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

            isAllowedToReadProject(projectRepo, projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

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
     * This function follows the following decision process:
     * - If there are no reviews, the paper is marked as [PaperDecision.PAPER_DECISION_UNREVIEWED].
     * - If the number of reviews is below the required threshold (as defined in the decision matrix),
     *   the paper remains with [PaperDecision.PAPER_DECISION_IN_REVIEW].
     * - Once the expected number of reviews is reached, the function attempts to match the current review distribution
     *   against the configured decision matrix patterns. If a matching pattern is found (order-sensitive),
     *   its associated final decision is returned.
     * - If no matrix pattern matches, the default decision is [PaperDecision.PAPER_DECISION_IN_REVIEW]
     * - If the required number of reviews were not enough to determine a final decision
     *   [PaperDecision.PAPER_DECISION_ACCEPTED] or [PaperDecision.PAPER_DECISION_DECLINED]), the latest review
     *   (assumed to be the deciding one) determines final decision. The paper is then only set to
     *   [PaperDecision.PAPER_DECISION_ACCEPTED] in case the latest review was [ReviewDecision.REVIEW_DECISION_ACCEPTED];
     *   otherwise, it is set to [PaperDecision.PAPER_DECISION_DECLINED].
     *
     * @param reviews List of reviews to be considered for the final paper decision. The latest review is assumed to be the last one in the list.
     * @param decisionMatrix Decision matrix of the project, where the project paper is in, that can be used to define
     * the final paper decision.
     * @return The computed [PaperDecision] based on the given reviews.
     */
    private fun determinePaperDecision(reviews: List<Review>, decisionMatrix: ReviewDecisionMatrix): PaperDecision {
        if (reviews.isEmpty()) {
            return PaperDecision.PAPER_DECISION_UNREVIEWED
        }
        if (reviews.size < decisionMatrix.numberOfReviewers) {
            return PaperDecision.PAPER_DECISION_IN_REVIEW
        }
        if (reviews.size == decisionMatrix.numberOfReviewers) {
            val counts = reviews.groupingBy { it.decision }.eachCount()

            for (pattern in decisionMatrix.patternsList) {
                val doesFoundMatch = pattern.entriesList.all { entry ->
                    (counts[entry.reviewDecision] ?: 0) >= entry.count.toInt()
                }
                if (doesFoundMatch) {
                    return pattern.decision
                }
            }

            return PaperDecision.PAPER_DECISION_IN_REVIEW
        }

        val decidingReview = reviews.last()
        return if (decidingReview.decision == ReviewDecision.REVIEW_DECISION_ACCEPTED) {
            PaperDecision.PAPER_DECISION_ACCEPTED
        } else {
            PaperDecision.PAPER_DECISION_DECLINED
        }
    }

    override suspend fun createReview(request: GrpcReview.Create): GrpcReview = withUser(userRepo) { currentUser ->
        val projectPaperId = parseUUID(request.projectPaperId, EntityType.PROJECT_PAPER)
        val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

        isAllowedToReadProject(projectRepo, projectMemberRepo).checkFor(currentUser, projectPaper.projectId)

        val project = projectRepo.getProjectById(projectPaper.projectId).getOrThrow()
        isProjectActive().checkFor(currentUser, project)

        val reviewsForProjectPaper = repo.getAllReviewsForProjectPaper(projectPaperId)
        val hasUserAlreadyReviewed = reviewsForProjectPaper.any { review -> review.userId == currentUser.id }
        if (hasUserAlreadyReviewed) {
            throw DuplicateReviewException(projectPaperId, currentUser.id)
        }

        val isPaperNotFinallyDecided = projectPaper.decision == PaperDecision.PAPER_DECISION_IN_REVIEW ||
            projectPaper.decision == PaperDecision.PAPER_DECISION_UNREVIEWED
        if (!isPaperNotFinallyDecided) {
            throw FailedPreconditionException(
                "The project paper must be either unreviewed or still in review. " +
                    "Finally decided project papers cannot be reviewed anymore.",
            )
        }

        val review = repo.createReview(request, currentUser.id)
        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(review.id)

        val hardExclusionCriteria = criteriaRepo.getAllProjectCriteria(project.id)
            .filter { criterion -> criterion.category == CriterionCategory.CRITERION_CATEGORY_HARD_EXCLUSION }
            .map { criterion -> criterion.id }
        val isAnySelectedCriteriaHardExclusion = selectedCriteriaIds.any { id -> hardExclusionCriteria.contains(id) }
        if (isAnySelectedCriteriaHardExclusion && review.decision == ReviewDecision.REVIEW_DECISION_DECLINED) {
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, PaperDecision.PAPER_DECISION_DECLINED)
        } else {
            val updatedDecision = determinePaperDecision(reviewsForProjectPaper + review, project.reviewDecisionMatrix)
            projectPaperRepo.updateProjectPaperDecision(projectPaperId, updatedDecision)
        }

        review.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
    }
}
