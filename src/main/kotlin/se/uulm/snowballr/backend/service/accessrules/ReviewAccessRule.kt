@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.Review
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectPaperTableRepo
import javax.annotation.CheckReturnValue

/**
 * Represents an [AccessRule] to a review entity.
 */
fun interface ReviewAccessRule : AccessRule<Review>

/**
 * Check whether the requester is a member of the project the review was created in.
 *
 * @param projectMemberRepo The repository used to access project membership data.
 * @param projectPaperRepo The repository used to access project-papers.
 */
private fun isUserInProjectOfReview(
    projectMemberRepo: IProjectMemberTableRepo,
    projectPaperRepo: IProjectPaperTableRepo,
) = ReviewAccessRule { requester, target ->
    val projectPaper = projectPaperRepo.getProjectPaperById(target.projectPaperId).getOrThrow()
    val projectMembers = projectMemberRepo.getProjectMembers(projectPaper.projectId)
    projectMembers.any { it.userId == requester.id }
}

/**
 * Check whether the current user is a member of the project. If the user is not a project member,
 * the user has to be a server admin; otherwise, throws an [UnauthorizedException.Single].
 *
 * Actually, this rule should be used to check whether the current user is allowed to read the review,
 * and this check includes that the current user must be a member of the project the review belongs to.
 *
 * @param projectMemberRepo The repository used to access project membership data.
 * @param projectPaperRepo The repository used to access project-papers.
 */
@CheckReturnValue
fun isAllowedToReadReview(
    projectMemberRepo: IProjectMemberTableRepo,
    projectPaperRepo: IProjectPaperTableRepo,
): AccessRule<Review> {
    return isUserInProjectOfReview(projectMemberRepo, projectPaperRepo)
        .orElse(isServerAdmin.forTarget())
        .orElseThrow { currentUser, target ->
            UnauthorizedException.Single(
                EntityType.REVIEW,
                target.id.toString(),
                AccessType.READ,
                currentUser.id.toString(),
            )
        }
}
