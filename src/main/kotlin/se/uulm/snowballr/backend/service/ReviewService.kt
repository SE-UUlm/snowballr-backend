package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IReviewAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.project.SnowballingType
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectRequest
import se.uulm.snowballr.backend.model.incoming.project.UpdateProjectSettingRequest
import se.uulm.snowballr.backend.model.incoming.review.CreateReviewRequest
import se.uulm.snowballr.backend.model.outgoing.review.ReviewResponse
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import java.util.UUID

interface IReviewService {
    /**
     * Service implementation of [SnowballRService.getReviewById].
     */
    suspend fun getReviewById(reviewId: UUID): ReviewResponse

    /**
     * Service implementation of [SnowballRService.getAllReviewsForProjectPaper].
     */
    suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<ReviewResponse>

    /**
     * Service implementation of [SnowballRService.createReview].
     */
    suspend fun createReview(request: CreateReviewRequest): ReviewResponse
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
 * @param criterionRepo Interface for persistence and retrieval operations related to criteria.
 * @param reviewHasCriterionRepo Interface for persistence and retrieval operations related to review-criteria relation.
 * @param accessChecker Interface for checking access permissions for reviews based on defined rules.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 * @param fetcherOrchestrator Interface for enqueuing fetcher jobs for fetching referenced papers.
 */
@Suppress("LongParameterList")
class ReviewService(
    private val repo: IReviewTableRepo,
    private val userRepo: IUserTableRepo,
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val reviewHasCriterionRepo: IReviewHasCriterionTableRepo,
    private val accessChecker: IReviewAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
    private val fetcherOrchestrator: IFetcherOrchestrator,
) : IReviewService {
    override suspend fun getReviewById(reviewId: UUID): ReviewResponse = withUser(userRepo) { currentUser ->
        val review = repo.getReviewById(reviewId).getOrThrow()

        accessChecker.isAllowedToReadReview(currentUser, review)

        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(reviewId)

        ReviewResponse.fromReviewAndIds(review, selectedCriteriaIds)
    }

    override suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): List<ReviewResponse> =
        withUser(userRepo) { currentUser ->
            val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

            projectAccessChecker.isAllowedToReadProject(currentUser, projectPaper.projectId)

            repo.getAllReviewsWithSelectedCriteriaIdsForProjectPaper(projectPaperId)
                .map { ReviewResponse.fromReviewWithSelectedCriteriaIds(it) }
        }

    override suspend fun createReview(request: CreateReviewRequest): ReviewResponse =
        withUser(userRepo) { currentUser ->
            val projectPaper = projectPaperRepo.getProjectPaperById(request.projectPaperId).getOrThrow()

            val projectResult = projectRepo.getProjectById(projectPaper.projectId)
            accessChecker.isAllowedToCreateReview(currentUser, projectPaper.projectId, projectResult)
            val project = projectResult.getOrThrow()

            val reviewsForProjectPaper = repo.getAllReviewsForProjectPaper(request.projectPaperId)
            val hasUserAlreadyReviewed = reviewsForProjectPaper.any { review -> review.userId == currentUser.id }
            if (hasUserAlreadyReviewed) {
                throw DuplicateReviewException(request.projectPaperId, currentUser.id)
            }

            if (projectPaper.hasFinalDecision) {
                throw FailedPreconditionException(
                    "The project paper must be either unreviewed or still in review. " +
                        "Finally decided project papers cannot be reviewed anymore.",
                )
            }

            val review = repo.createReview(request, currentUser.id)
            val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(review.id)

            val hasSelectedExclusionCriterion = hasSelectedHardExclusionCriterion(project.id, selectedCriteriaIds)

            val decision = if (hasSelectedExclusionCriterion && review.doesDeclinePaper) {
                PaperDecision.DECLINED
            } else {
                determinePaperDecision(reviewsForProjectPaper + review, project.reviewDecisionMatrix)
            }
            projectPaperRepo.updateProjectPaperDecision(request.projectPaperId, decision)

            if (project.status != ProjectStatus.ACTIVE_LOCKED) {
                setProjectStatusActiveLocked(project.id)
            }
            if (decision === PaperDecision.ACCEPTED) {
                fetcherOrchestrator.enqueue(FetcherEnqueueJob(projectPaper, currentUser.id))
            }

            ReviewResponse.fromReviewAndIds(review, selectedCriteriaIds)
        }

    private suspend fun hasSelectedHardExclusionCriterion(projectId: UUID, selectedCriteriaIds: List<UUID>): Boolean {
        val hardExclusionCriteria = criterionRepo.getAllProjectCriteria(projectId)
            .filter { criterion -> criterion.category == CriterionCategory.HARD_EXCLUSION }
            .map { criterion -> criterion.id }

        return selectedCriteriaIds.any { id -> hardExclusionCriteria.contains(id) }
    }

    /**
     * Determines the final paper decision based on the given list of reviews.
     *
     * This function follows the following decision process:
     * - If the number of reviews is below the required threshold (as defined in the decision matrix),
     *   the paper remains with [PaperDecision.IN_REVIEW].
     * - Once the expected number of reviews is reached, the function attempts to match the current review distribution
     *   against the configured decision matrix patterns. If a matching pattern is found (order-sensitive),
     *   its associated final decision is returned.
     * - If no matrix pattern matches, the default decision is [PaperDecision.IN_REVIEW]
     * - If the required number of reviews were not enough to determine a final decision [PaperDecision.ACCEPTED] or
     *   [PaperDecision.DECLINED]), the latest review (assumed to be the deciding one) determines final decision. The
     *   paper is then only set to [PaperDecision.ACCEPTED] in case the latest review was [ReviewDecision.ACCEPTED];
     *   otherwise, it is set to [PaperDecision.DECLINED].
     *
     * @param reviews List of reviews to be considered for the final paper decision. The latest review is assumed to be the last one in the list.
     * @param decisionMatrix Decision matrix of the project, where the project paper is in, that can be used to define
     * the final paper decision.
     * @return The computed [PaperDecision] based on the given reviews.
     */
    private fun determinePaperDecision(reviews: List<Review>, decisionMatrix: ReviewDecisionMatrix): PaperDecision {
        if (reviews.size < decisionMatrix.numberOfReviewers) {
            return PaperDecision.IN_REVIEW
        }
        if (reviews.size == decisionMatrix.numberOfReviewers) {
            val counts = reviews.groupingBy { it.decision }.eachCount()

            for (pattern in decisionMatrix.patterns) {
                val doesFoundMatch = pattern.entries.all { entry ->
                    (counts[entry.decision] ?: 0) >= entry.count
                }
                if (doesFoundMatch) {
                    return pattern.decision
                }
            }

            return PaperDecision.IN_REVIEW
        }

        val decidingReview = reviews.last()
        return if (decidingReview.doesAcceptPaper) {
            PaperDecision.ACCEPTED
        } else {
            PaperDecision.DECLINED
        }
    }

    private suspend fun setProjectStatusActiveLocked(projectId: UUID) {
        val request = UpdateProjectRequest(
            projectId = projectId,
            name = "",
            status = ProjectStatus.ACTIVE_LOCKED,
            settings = UpdateProjectSettingRequest(
                similarityThreshold = 0F,
                snowballingType = SnowballingType.BOTH,
                reviewMaybeAllowed = false,
                fetchers = emptyMap(),
                decisionMatrix = ReviewDecisionMatrix(
                    numberOfReviewers = 1,
                    patterns = emptyList(),
                ),
            ),
        )

        projectRepo.updateProject(request, setOf("project.status"))
    }
}
