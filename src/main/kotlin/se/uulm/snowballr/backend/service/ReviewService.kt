package se.uulm.snowballr.backend.service

import com.google.protobuf.util.FieldMaskUtil
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IReviewAccessChecker
import se.uulm.snowballr.backend.fetcher.IFetcherOrchestrator
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.criterion.CriterionCategory
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.project.ReviewDecisionMatrix
import se.uulm.snowballr.backend.model.dto.projectpaper.PaperDecision
import se.uulm.snowballr.backend.model.dto.projectpaper.hasFinalDecision
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.review.ReviewDecision
import se.uulm.snowballr.backend.model.dto.review.doesAcceptPaper
import se.uulm.snowballr.backend.model.dto.review.doesDeclinePaper
import se.uulm.snowballr.backend.model.dto.review.toGrpcReview
import se.uulm.snowballr.backend.model.dto.review.toGrpcReviews
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.alreadyexists.DuplicateReviewException
import se.uulm.snowballr.backend.model.fetcher.FetcherEnqueueJob
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import snowballr.ProjectOuterClass
import java.util.UUID
import snowballr.ReviewOuterClass.Review as GrpcReview

interface IReviewService {
    /**
     * Service implementation of [SnowballRService.getReviewById].
     */
    suspend fun getReviewById(reviewId: UUID): GrpcReview

    /**
     * Service implementation of [SnowballRService.getAllReviewsForProjectPaper].
     */
    suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): GrpcReview.List

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
    override suspend fun getReviewById(reviewId: UUID): GrpcReview = withUser(userRepo) { currentUser ->
        val review = repo.getReviewById(reviewId).getOrThrow()

        accessChecker.isAllowedToReadReview(currentUser, review)

        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(reviewId)
        review.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
    }

    override suspend fun getAllReviewsForProjectPaper(projectPaperId: UUID): GrpcReview.List =
        withUser(userRepo) { currentUser ->
            val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

            projectAccessChecker.isAllowedToReadProject(currentUser, projectPaper.projectId)

            val reviews = repo.getAllReviewsForProjectPaper(projectPaperId)
            val reviewSelectedCriteriaMap = mutableMapOf<Review, List<String>>()
            for (review in reviews) {
                reviewSelectedCriteriaMap[review] = reviewHasCriterionRepo
                    .getSelectedCriteriaIdsForReviewById(review.id).map(UUID::toString)
            }

            reviews.toGrpcReviews(reviewSelectedCriteriaMap)
        }

    override suspend fun createReview(request: GrpcReview.Create): GrpcReview = withUser(userRepo) { currentUser ->
        val projectPaperId = parseUUID(request.projectPaperId, EntityType.PROJECT_PAPER)
        val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

        val projectResult = projectRepo.getProjectById(projectPaper.projectId)
        accessChecker.isAllowedToCreateReview(currentUser, projectPaper.projectId, projectResult)
        val project = projectResult.getOrThrow()

        val reviewsForProjectPaper = repo.getAllReviewsForProjectPaper(projectPaperId)
        val hasUserAlreadyReviewed = reviewsForProjectPaper.any { review -> review.userId == currentUser.id }
        if (hasUserAlreadyReviewed) {
            throw DuplicateReviewException(projectPaperId, currentUser.id)
        }

        if (projectPaper.hasFinalDecision()) {
            throw FailedPreconditionException(
                "The project paper must be either unreviewed or still in review. " +
                    "Finally decided project papers cannot be reviewed anymore.",
            )
        }

        val review = repo.createReview(request, currentUser.id)
        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(review.id)

        val hasSelectedExclusionCriterion = hasSelectedHardExclusionCriterion(project.id, selectedCriteriaIds)

        val decision = if (hasSelectedExclusionCriterion && review.doesDeclinePaper()) {
            PaperDecision.DECLINED
        } else {
            determinePaperDecision(reviewsForProjectPaper + review, project.reviewDecisionMatrix)
        }
        projectPaperRepo.updateProjectPaperDecision(projectPaperId, decision)

        if (project.status != ProjectStatus.ACTIVE_LOCKED) {
            setProjectStatusActiveLocked(project.id)
        }
        if (decision === PaperDecision.ACCEPTED) {
            fetcherOrchestrator.enqueue(FetcherEnqueueJob(projectPaper, currentUser.id))
        }

        review.toGrpcReview(selectedCriteriaIds.map(UUID::toString))
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
        return if (decidingReview.doesAcceptPaper()) {
            PaperDecision.ACCEPTED
        } else {
            PaperDecision.DECLINED
        }
    }

    private suspend fun setProjectStatusActiveLocked(projectId: UUID) {
        val request = ProjectOuterClass.Project.Update.newBuilder()
            .setProject(
                ProjectOuterClass.Project.newBuilder()
                    .setId(projectId.toString())
                    .setStatus(ProjectStatus.ACTIVE_LOCKED.toGrpc())
                    .build(),
            )
            .setMask(FieldMaskUtil.fromString("project.status"))
            .build()
        projectRepo.updateProject(request)
    }
}
