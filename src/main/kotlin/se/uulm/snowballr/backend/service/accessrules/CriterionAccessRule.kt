@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.Criterion.UserCriterion
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import javax.annotation.CheckReturnValue

/**
 * Represents an [AccessRule] to a criterion entity.
 */
fun interface CriterionAccessRule : AccessRule<Criterion>

/**
 * Check whether the current user created the target criterion.
 * Additionally, the criterion must be a user criterion.
 */
val isCreatorOfCriterion = CriterionAccessRule { currentUser, criterion ->
    criterion is UserCriterion && currentUser.id == criterion.createdBy
}

/**
 * Check whether the requester is a member of the project the criterion was created in.
 *
 * @param projectMemberRepo The repository used to access project membership data.
 */
@CheckReturnValue
fun isUserInProjectOfCriterion(projectMemberRepo: IProjectMemberTableRepo) = CriterionAccessRule { requester, target ->
    when (target) {
        is UserCriterion -> {
            false
        }
        is ProjectCriterion -> {
            val projectMembers = projectMemberRepo.getProjectMembers(target.projectId)
            projectMembers.any { it.userId == requester.id }
        }
    }
}

/**
 * Check whether the requester is a project admin of the project the criterion was created in.
 *
 * @param projectMemberRepo The repository used to access project membership data.
 */
@CheckReturnValue
fun isUserAdminInProjectOfCriterion(projectMemberRepo: IProjectMemberTableRepo) =
    CriterionAccessRule { requester, target ->
        when (target) {
            is UserCriterion -> {
                false
            }
            is ProjectCriterion -> {
                val projectMembers = projectMemberRepo.getAllProjectAdmins(target.projectId)
                projectMembers.any { it.userId == requester.id }
            }
        }
    }
