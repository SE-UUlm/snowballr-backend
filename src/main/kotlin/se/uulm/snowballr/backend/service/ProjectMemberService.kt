package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IProjectMemberAccessChecker
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.projectmember.InvitationToken
import se.uulm.snowballr.backend.model.dto.projectmember.MemberRole
import se.uulm.snowballr.backend.model.dto.projectmember.ProjectMemberWithUser
import se.uulm.snowballr.backend.model.dto.projectmember.isProjectAdmin
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.incoming.projectmember.UpdateProjectMemberRoleRequest
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import java.util.UUID

interface IProjectMemberService {
    /**
     * Service implementation of [SnowballRService.getProjectMembers].
     */
    suspend fun getProjectMembers(projectId: UUID): List<ProjectMemberWithUser>

    /**
     * Service implementation of [SnowballRService.updateProjectMemberRole].
     */
    suspend fun updateProjectMemberRole(request: UpdateProjectMemberRoleRequest)

    /**
     * Service implementation of [SnowballRService.removeProjectMember].
     */
    suspend fun removeProjectMember(projectId: UUID, userEmail: String)
}

/**
 * The [ProjectMemberService] class handles operations related to project members by implementing the [IProjectMemberService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of project members CRUD operations,
 * delegating the actual persistence operations to the [IProjectMemberTableRepo] repository.
 *
 * @constructor Initializes the [ProjectMemberService] with a project member repository.
 * @param repo The repository responsible for managing persistence operations for project members.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param invitationTokenRepo The repository responsible for managing persistence operations for invitation tokens.
 * @param accessChecker Interface for checking access permissions for project members based on defined rules.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
class ProjectMemberService(
    private val repo: IProjectMemberTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val invitationTokenRepo: IInvitationTokenTableRepo,
    private val accessChecker: IProjectMemberAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
) : IProjectMemberService {
    override suspend fun getProjectMembers(projectId: UUID): List<ProjectMemberWithUser> =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            repo.getProjectMembersWithUsers(projectId)
        }

    override suspend fun updateProjectMemberRole(request: UpdateProjectMemberRoleRequest) {
        withUser(userRepo) { currentUser ->
            accessChecker.isAllowedToUpdateMemberRole(currentUser, request.projectId)

            val user = userRepo.getUserById(request.userId).getOrThrow()

            val member = try {
                repo.getProjectMemberByComposedId(request.projectId, request.userId).getOrThrow()
            } catch (_: NotFoundException) {
                throw FailedPreconditionException(
                    "User with ID '${request.userId}' is not a member of project with ID '${request.projectId}'.",
                )
            }

            if (member.isProjectAdmin() && request.newRole != MemberRole.ADMIN) {
                projectAccessChecker.isNotLastProjectAdmin(user, request.projectId, "Cannot demote the user")
            }

            repo.updateProjectMemberRole(request.projectId, request.userId, request.newRole)
        }
    }

    override suspend fun removeProjectMember(projectId: UUID, userEmail: String) = withUser(userRepo) { currentUser ->
        val invitationToken =
            invitationTokenRepo.getInvitationTokenByEmailAndProjectId(userEmail, projectId).getOrNull()

        if (invitationToken != null) {
            removeProjectMemberInvitation(currentUser, projectId, invitationToken)
        } else {
            val requestedUser = userRepo.getUserByEmail(userEmail).getOrThrow()
            removeProjectMemberUser(currentUser, requestedUser, projectId)
        }
    }

    private suspend fun removeProjectMemberUser(currentUser: User, requestedUser: User, projectId: UUID) {
        if (!repo.isProjectMember(projectId, requestedUser.id)) {
            return
        }

        accessChecker.isAllowedToRemoveMember(currentUser, requestedUser.id, projectId)

        if (!projectRepo.doesProjectExistById(projectId)) {
            throw ProjectNotFoundException(projectId)
        }

        val projectMembers = repo.getProjectMembers(projectId)
        val isLastMember = projectMembers.size == 1 && projectMembers.first().userId == requestedUser.id
        if (isLastMember) {
            projectRepo.softDeleteProject(projectId)
        } else {
            projectAccessChecker
                .isNotLastProjectAdmin(requestedUser, projectId, "The user cannot be removed from the project")
        }

        repo.removeProjectMember(projectId, requestedUser.id)
    }

    private suspend fun removeProjectMemberInvitation(currentUser: User, projectId: UUID, token: InvitationToken) {
        accessChecker.isAllowedToRemoveInvitation(currentUser, projectId)

        invitationTokenRepo.deleteInvitationToken(token.token)
    }
}
