package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.ProjectNotFoundException
import se.uulm.snowballr.backend.model.exception.notfound.entity.UserNotFoundByEmailException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedActionException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.accessrules.AccessRuleCompoundUUID
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forProperty
import se.uulm.snowballr.backend.service.accessrules.forTarget
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isNotLastProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.isProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.isProjectExistent
import se.uulm.snowballr.backend.service.accessrules.isSameUserById
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.isServerOrProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.orElse
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import snowballr.Base
import snowballr.ProjectOuterClass.MemberRole
import java.util.UUID
import snowballr.ProjectOuterClass.Project.Member as GrpcProjectMember

interface IProjectMemberService {
    /**
     * Service implementation of [SnowballRService.getProjectMembers].
     */
    suspend fun getProjectMembers(request: Base.Id): GrpcProjectMember.List

    /**
     * Service implementation of [SnowballRService.updateProjectMemberRole].
     */
    suspend fun updateProjectMemberRole(request: GrpcProjectMember.Update): Base.Nothing

    /**
     * Service implementation of [SnowballRService.removeProjectMember].
     */
    suspend fun removeProjectMember(request: GrpcProjectMember.Remove): Base.Nothing
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
 */
class ProjectMemberService(
    private val repo: IProjectMemberTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
    private val invitationTokenRepo: IInvitationTokenTableRepo,
) : IProjectMemberService {
    override suspend fun getProjectMembers(request: Base.Id): GrpcProjectMember.List =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.id, EntityType.PROJECT)

            isAllowedToReadProject(repo)
                .andAlso(isProjectExistent(projectRepo))
                .checkFor(currentUser, projectId)

            val projectMembersWithUsers = repo.getProjectMembersWithUsers(projectId)
            projectMembersWithUsers.toGrpcProjectMembers()
        }

    override suspend fun updateProjectMemberRole(request: GrpcProjectMember.Update): Base.Nothing {
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val userId = parseUUID(request.userId, EntityType.USER)

            isServerOrProjectAdmin(repo, AccessType.UPDATE).checkFor(currentUser, projectId)

            projectRepo.getProjectById(projectId).getOrThrow()
            val user = userRepo.getUserById(userId).getOrThrow()

            val currentMember = try {
                repo.getProjectMemberByComposedId(projectId, userId).getOrThrow()
            } catch (_: NotFoundException) {
                throw FailedPreconditionException(
                    "User with ID '$userId' is not a member of project with ID '$projectId'.",
                )
            }

            if (currentMember.role == MemberRole.MEMBER_ROLE_ADMIN && request.newRole != MemberRole.MEMBER_ROLE_ADMIN) {
                isNotLastProjectAdmin(repo, "Cannot demote the user")
                    .checkFor(user, projectId)
            }

            repo.updateProjectMemberRole(projectId, userId, request.newRole)
        }

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun removeProjectMember(request: GrpcProjectMember.Remove): Base.Nothing =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val requestedUserResult = userRepo.getUserByEmail(request.userEmail)

            if (requestedUserResult.isSuccess) {
                val requestedUser = requestedUserResult.getOrThrow()
                removeProjectMemberUser(currentUser, requestedUser, projectId)
            } else {
                removeProjectMemberInvitation(currentUser, request.userEmail, projectId)
            }

            Base.Nothing.getDefaultInstance()
        }

    private suspend fun removeProjectMemberUser(currentUser: User, requestedUser: User, projectId: UUID) {
        val userProjectCompound = AccessRuleCompoundUUID(requestedUser.id, projectId)

        if (!repo.isProjectMember(projectId, requestedUser.id)) {
            return
        }

        isSameUserById()
            .forProperty(AccessRuleCompoundUUID::firstTarget)
            .orElse(
                isProjectAdmin(repo)
                    .orElse(isServerAdmin().forTarget())
                    .orElseThrow { user, targetId ->
                        UnauthorizedActionException(EntityType.PROJECT, targetId, AccessType.DELETE, user.id)
                    }
                    .forProperty(AccessRuleCompoundUUID::secondTarget),
            )
            .checkFor(currentUser, userProjectCompound)

        if (!projectRepo.doesProjectExistById(projectId)) {
            throw ProjectNotFoundException(projectId)
        }

        val projectMembers = repo.getProjectMembers(projectId)
        val isLastMember = projectMembers.size == 1 && projectMembers.first().userId == requestedUser.id
        if (isLastMember) {
            projectRepo.softDeleteProject(projectId)
        } else {
            isNotLastProjectAdmin(repo, "The user cannot be removed from the project")
                .checkFor(requestedUser, projectId)
        }

        repo.removeProjectMember(projectId, requestedUser.id)
    }

    private suspend fun removeProjectMemberInvitation(currentUser: User, userEmail: String, projectId: UUID) {
        isProjectAdmin(repo)
            .orElse(isServerAdmin().forTarget())
            .orElseThrow { user, targetId ->
                UnauthorizedActionException(EntityType.PROJECT, targetId, AccessType.DELETE, user.id)
            }
            .checkFor(currentUser, projectId)

        val invitationToken =
            invitationTokenRepo.getInvitationTokenByEmailAndProjectId(userEmail, projectId).getOrNull()
                ?: throw UserNotFoundByEmailException(userEmail)

        invitationTokenRepo.deleteInvitationToken(invitationToken.token)
    }
}
