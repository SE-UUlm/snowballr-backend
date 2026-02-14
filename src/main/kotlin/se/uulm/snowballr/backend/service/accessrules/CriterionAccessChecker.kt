@file:Suppress("NonBooleanPropertyPrefixedWithIs")

package se.uulm.snowballr.backend.service.accessrules

import se.uulm.snowballr.backend.model.dto.Criterion
import se.uulm.snowballr.backend.model.dto.Criterion.ProjectCriterion
import se.uulm.snowballr.backend.model.dto.Criterion.UserCriterion
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID
import javax.annotation.CheckReturnValue

interface ICriterionAccessChecker {
    /**
     * Check whether the current user created the target criterion.
     * Additionally, the criterion must be a user criterion.
     */
    @CheckReturnValue
    fun isCreatorOfCriterion(): AccessRule<Criterion>

    /**
     * Check whether the requester is a member of the project the criterion was created in.
     */
    @CheckReturnValue
    fun isUserInProjectOfCriterion(): AccessRule<Criterion>

    /**
     * Check whether the requester is a project admin of the project the criterion was created in.
     */
    @CheckReturnValue
    fun isUserAdminInProjectOfCriterion(): AccessRule<Criterion>
}

class CriterionAccessChecker(
    private val projectMemberRepo: IProjectMemberTableRepo,
) : ICriterionAccessChecker {
    override fun isCreatorOfCriterion() = AccessRule<Criterion> { currentUser, criterion ->
        criterion is UserCriterion && currentUser.id == criterion.createdBy
    }

    override fun isUserInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> isProjectMember(target.projectId, requester.id)
        }
    }

    override fun isUserAdminInProjectOfCriterion() = AccessRule<Criterion> { requester, target ->
        when (target) {
            is UserCriterion -> false
            is ProjectCriterion -> isProjectAdmin(target.projectId, requester.id)
        }
    }

    private suspend fun isProjectMember(projectId: UUID, userId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getProjectMembers(projectId)
        return projectMembers.any { it.userId == userId }
    }

    private suspend fun isProjectAdmin(projectId: UUID, userId: UUID): Boolean {
        val projectMembers = projectMemberRepo.getAllProjectAdmins(projectId)
        return projectMembers.any { it.userId == userId }
    }
}
