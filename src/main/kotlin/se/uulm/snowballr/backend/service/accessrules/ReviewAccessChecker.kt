package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.model.exception.UnauthorizedException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadException
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import javax.annotation.CheckReturnValue

interface IReviewAccessChecker {
    /**
     * Check whether the current user is allowed to read the review.
     *
     * For the check to succeed, the user must be a member of the project the review belongs to. If the user is not a
     * project member, the user has to be a server admin; otherwise, an [UnauthorizedException] is thrown.
     */
    @CheckReturnValue
    fun isAllowedToReadReview(): AccessRule<Review>
}

class ReviewAccessChecker(
    private val projectPaperRepo: IProjectPaperTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IReviewAccessChecker {
    override fun isAllowedToReadReview() = isUserInProjectOfReview()
        .orElse(isServerAdmin().forTarget())
        .orElseThrow { currentUser, target ->
            UnauthorizedReadException(currentUser.id, target.id, EntityType.REVIEW)
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
