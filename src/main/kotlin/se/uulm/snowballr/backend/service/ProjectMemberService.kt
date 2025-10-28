package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcProjectMembers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.accessrules.AccessRuleCompoundObject
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forProperty
import se.uulm.snowballr.backend.service.accessrules.forTarget
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadProject
import se.uulm.snowballr.backend.service.accessrules.isProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.isProjectExistent
import se.uulm.snowballr.backend.service.accessrules.isProjectMember
import se.uulm.snowballr.backend.service.accessrules.isSameUserById
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.isServerOrProjectAdmin
import se.uulm.snowballr.backend.service.accessrules.orElse
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import snowballr.Base
import snowballr.ProjectOuterClass.MemberRole
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
 */
class ProjectMemberService(
    private val repo: IProjectMemberTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val userRepo: IUserTableRepo,
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
            userRepo.getUserById(userId).getOrThrow()

            val currentMember = try {
                repo.getProjectMemberByComposedId(projectId, userId).getOrThrow()
            } catch (_: NotFoundException) {
                throw FailedPreconditionException(
                    "User with ID '$userId' is not a member of project with ID '$projectId'.",
                )
            }

            if (currentMember.role == MemberRole.MEMBER_ROLE_ADMIN && request.newRole != MemberRole.MEMBER_ROLE_ADMIN) {
                val projectAdmins = repo.getAllProjectAdmins(projectId)
                if (projectAdmins.size <= 1) {
                    throw FailedPreconditionException("Cannot demote the last admin of a project.")
                }
            }

            repo.updateProjectMemberRole(projectId, userId, request.newRole)
        }

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun removeProjectMember(request: GrpcProjectMember.Remove): Base.Nothing =
        withUser(userRepo) { currentUser ->
            val projectId = parseUUID(request.projectId, EntityType.PROJECT)
            val requestedUserId = parseUUID(request.userId, EntityType.USER)

            val userProjectCompound = AccessRuleCompoundObject(requestedUserId, projectId)

            isSameUserById()
                .forProperty(AccessRuleCompoundObject::firstTargetId)
                .andAlso(isProjectMember(repo).forProperty(AccessRuleCompoundObject::secondTargetId))
                .orElse(
                    isProjectAdmin(repo)
                        .orElse(isServerAdmin().forTarget())
                        .orElseThrow { user, targetId ->
                            UnauthorizedException.Action(
                                EntityType.PROJECT,
                                targetId.toString(),
                                AccessType.DELETE,
                                user.id.toString(),
                            )
                        }
                        .forProperty(AccessRuleCompoundObject::secondTargetId),
                )
                .checkFor(currentUser, userProjectCompound)

            when {
                !projectRepo.doesProjectExistById(projectId)
                -> throw NotFoundException(EntityType.PROJECT, projectId.toString())

                !userRepo.doesUserExistById(requestedUserId)
                -> throw NotFoundException(EntityType.USER, projectId.toString())
            }

            val projectMembers = repo.getProjectMembers(projectId)
            val projectAdmins = repo.getAllProjectAdmins(projectId)
            if (projectMembers.size == 1 && projectMembers.any { it.userId == requestedUserId }) {
                projectRepo.softDeleteProject(projectId)
            } else if (projectAdmins.size == 1 && projectAdmins.any { it.userId == requestedUserId }) {
                throw FailedPreconditionException(
                    "The user can not be removed from the project, because this user is the last " +
                        "project admin.",
                )
            }

            repo.removeProjectMember(projectId, requestedUserId)
            Base.Nothing.getDefaultInstance()
        }
}
