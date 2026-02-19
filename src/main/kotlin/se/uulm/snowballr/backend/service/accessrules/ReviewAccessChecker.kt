package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Project
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

interface IReviewAccessChecker {
    /**
     * Checks whether the current user is allowed to create a review in the specified project.
     *
     * Conditions:
     * - The user is allowed to read to project ([IProjectAccessChecker.isAllowedToReadProject])
     * - The project is active (not archived or deleted)
     *
     * @throws ProjectNotFoundException if the project does not exist.
     * @throws UnauthorizedReadException if the user is not allowed to read the project.
     * @throws EntityNotActiveException if the project is not active.
     */
    suspend fun isAllowedToCreateReview(currentUser: User, projectId: UUID, projectResult: Result<Project>)

    /**
     * Checks whether the current user is allowed to read the target review.
     *
     * Conditions:
     * - The user is a member of the project to which the review belongs to, **OR** the user is a server admin
     *
     * @throws UnauthorizedReadException if the user is not allowed to read the review.
     */
    suspend fun isAllowedToReadReview(currentUser: User, review: Review)
}

class ReviewAccessChecker(
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val projectAccessChecker: IProjectAccessChecker,
) : IReviewAccessChecker {
    override suspend fun isAllowedToCreateReview(currentUser: User, projectId: UUID, projectResult: Result<Project>) {
        projectAccessChecker.isAllowedToReadProject(currentUser, projectId)
        projectAccessChecker.isProjectActive().checkFor(currentUser, projectResult.getOrThrow())
    }

    override suspend fun isAllowedToReadReview(currentUser: User, review: Review) {
        isUserInProjectOfReview()
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, target ->
                UnauthorizedReadException(user.id, target.id, EntityType.REVIEW)
            }
            .checkFor(currentUser, review)
    }

    /**
     * Check whether the requester is a member of the project the review was created in.
     */
    @CheckReturnValue
    private fun isUserInProjectOfReview() = AccessRule<Review> { requester, target ->
        val projectPaper = projectPaperRepo.getProjectPaperById(target.projectPaperId).getOrThrow()
        val projectMembers = projectMemberRepo.getProjectMembers(projectPaper.projectId)
        projectMembers.any { it.userId == requester.id }
    }
}
