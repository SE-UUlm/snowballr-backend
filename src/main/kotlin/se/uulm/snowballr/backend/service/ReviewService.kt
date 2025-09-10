package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcReview
import se.uulm.snowballr.backend.model.dto.toGrpcReviews
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IReviewTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import se.uulm.snowballr.backend.repository.association.IReviewHasCriterionTableRepo
import snowballr.Base
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
    override suspend fun getReviewById(request: Base.Id): GrpcReview {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val reviewId = parseUUID(request.id, EntityType.REVIEW)
        val review = repo.getReviewById(reviewId).getOrThrow()
        val projectPaper = projectPaperRepo.getProjectPaperById(review.projectPaperId).getOrThrow()

        ensureCurrentUserIsProjectMember(projectPaper.projectId, currentUser)

        val selectedCriteriaIds = reviewHasCriterionRepo.getSelectedCriteriaIdsForReviewById(reviewId)
        return review.toGrpcReview(selectedCriteriaIds.map { it.toString() })
    }

    override suspend fun getAllReviewsForProjectPaper(request: Base.Id): GrpcReview.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val projectPaperId = parseUUID(request.id, EntityType.PROJECT_PAPER)
        val projectPaper = projectPaperRepo.getProjectPaperById(projectPaperId).getOrThrow()

        ensureCurrentUserIsProjectMember(projectPaper.projectId, currentUser)

        val reviews = repo.getAllReviewsForProjectPaper(projectPaperId)
        val reviewSelectedCriteriaMap = mutableMapOf<Review, List<String>>()
        for (review in reviews) {
            reviewSelectedCriteriaMap[review] = reviewHasCriterionRepo
                .getSelectedCriteriaIdsForReviewById(review.id).map {
                    it.toString()
                }
        }
        return reviews.toGrpcReviews(reviewSelectedCriteriaMap)
    }

    private suspend fun ensureCurrentUserIsProjectMember(projectId: UUID, currentUser: User) {
        val project = projectRepo.getProjectById(projectId)
        val projectMembers = projectMemberRepo.getProjectMembers(project.id)
        val isProjectMember = projectMembers.any { it.userId == currentUser.id }

        if (!isProjectMember) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(EntityType.PROJECT, project.id.toString(), AccessType.READ, it)
            }
        }
    }
}
