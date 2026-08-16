package se.uulm.snowballr.backend.access

import se.uulm.snowballr.backend.access.rules.AccessRule
import se.uulm.snowballr.backend.access.rules.checkFor
import se.uulm.snowballr.backend.access.rules.forTarget
import se.uulm.snowballr.backend.access.rules.isProjectActive
import se.uulm.snowballr.backend.access.rules.isServerAdmin
import se.uulm.snowballr.backend.access.rules.orElse
import se.uulm.snowballr.backend.access.rules.orElseThrow
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.project.Project
import se.uulm.snowballr.backend.model.dto.review.Review
import se.uulm.snowballr.backend.model.dto.user.User
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
     * - The user is allowed to read the project ([IProjectAccessChecker.isAllowedToReadProject])
     * - The project is active (not archived or deleted)
     *
     * @param currentUser The user for whom the access check is being performed.
     * @param projectId The ID of the project in which the review is being created.
     * @param projectResult The result of fetching the project, used to check if the project is active.
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
     * @param currentUser The user for whom the access check is being performed.
     * @param review The review that is being accessed.
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
        isProjectActive().checkFor(currentUser, projectResult.getOrThrow())
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
        val projectPaperResult = projectPaperRepo.getProjectPaperById(target.projectPaperId)
        val projectPaper = projectPaperResult.getOrNull()

        if (projectPaper != null) {
            projectMemberRepo.isProjectMember(projectPaper.projectId, requester.id)
        } else {
            false
        }
    }
}
