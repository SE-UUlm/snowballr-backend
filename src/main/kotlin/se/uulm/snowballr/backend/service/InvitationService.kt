package se.uulm.snowballr.backend.service

import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.SnowballRException.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcUsers
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.UserService.Companion.MINIMUM_LENGTH_OF_SEARCH_QUERY
import snowballr.Base
import snowballr.ProjectOuterClass.Project
import snowballr.ProjectOuterClass.ProjectStatus
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import snowballr.ProjectOuterClass.Project as GrpcProject
import snowballr.UserOuterClass.User as GrpcUser

interface IInvitationService {
    /**
     * Service implementation of [SnowballRService.getInviteCandidates]
     */
    suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): GrpcUser.List

    /**
     * Service implementation of [SnowballRService.inviteUserToProject].
     */
    suspend fun inviteUserToProject(request: GrpcProject.Member.Invite): Base.Nothing

    /**
     * Service implementation of [SnowballRService.acceptProjectInvitation].
     */
    suspend fun acceptProjectInvitation(request: GrpcProject.Member.Accept): Base.Nothing
}

private const val INVITATION_TOKEN_LENGTH = 48

/**
 * The [InvitationService] class handles operations related to normal papers by implementing the [IInvitationService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of invitations.
 *
 * @constructor Initializes the [InvitationService] with the necessary repositories.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param invitationTokenRepo The repository responsible for managing persistence operations for invitation tokens.
 * @param emailManager The manager responsible for sending emails.
 */
class InvitationService(
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val invitationTokenRepo: IInvitationTokenTableRepo,
    private val emailManager: IEmailManager,
) : IInvitationService {
    override suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): GrpcUser.List =
        withUser(userRepo) { currentUser ->
            val searchQuery = request.query.trim()

            // Check whether the search query is too short, i.e., 3 or fewer characters long
            if (searchQuery.length < MINIMUM_LENGTH_OF_SEARCH_QUERY) {
                return@withUser GrpcUser.List.getDefaultInstance()
            }

            val excludedUsersFromSearch = mutableSetOf(currentUser.id)
            try {
                val projectMembers =
                    projectMemberRepo.getProjectMembers(parseUUID(request.projectId, EntityType.PROJECT))
                excludedUsersFromSearch += projectMembers.map { it.userId }
            } catch (_: InvalidIdException.UUID) {
                Logger.warn { "Invalid project ID in invite candidates request: ${request.projectId}" }
            }

            val candidates = userRepo.getUsersMatchingSearchQuery(searchQuery, excludedUsersFromSearch)
            candidates.toGrpcUsers()
        }

    override suspend fun inviteUserToProject(request: GrpcProject.Member.Invite): Base.Nothing {
        // Check whether a project with the given id exists
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)
        val project = projectRepo.getProjectById(projectId).getOrThrow()

        // Check whether the project is active
        if (project.status != ProjectStatus.PROJECT_STATUS_ACTIVE) {
            throw FailedPreconditionException(
                "The project with the id $projectId is not active.",
            )
        }

        // Check whether the current user is a server admin or a project admin
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()

        val isProjectAdmin = projectMemberRepo.getAllProjectAdmins(projectId).any { it.userId == currentUser.id }
        if (!isProjectAdmin) {
            verifyServerAdminRole(currentUser) {
                throw UnauthorizedException.Single(
                    EntityType.PROJECT,
                    projectId.toString(),
                    AccessType.READ,
                    it,
                )
            }
        }

        // Generate and save invitation token
        val invitationToken = NanoId.generate(INVITATION_TOKEN_LENGTH)
        invitationTokenRepo.saveInvitationToken(request.userEmail, projectId, invitationToken)

        // Get first name of user if exists
        val userFirstName = try {
            userRepo.getUserByEmail(request.userEmail).getOrThrow().firstName
        } catch (_: NotFoundException) {
            "User"
        }

        // Send invitation email
        val invitationLink = emailManager.createAcceptProjectInvitationLink(invitationToken)
        emailManager.sendAcceptProjectInvitationEmail(
            request.userEmail,
            EmailData.AcceptProjectInvitation(
                userFirstName,
                project.name,
                invitationLink,
            ),
        )

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun acceptProjectInvitation(request: GrpcProject.Member.Accept): Base.Nothing {
        val invitationToken = invitationTokenRepo.getInvitationTokenByValue(request.token)
            ?: throw InvitationTokenNotFoundException()

        // Check if the token has expired
        if (OffsetDateTime.now().isAfter(invitationToken.expiresAt)) {
            invitationTokenRepo.deleteInvitationToken(invitationToken.token)
            throw InvitationTokenNotFoundException()
        }

        // Check if the user is registered and verified
        val user = try {
            userRepo.getUserByEmail(invitationToken.email)
        } catch (_: NotFoundException) {
            throw FailedPreconditionException(
                "The user with the email ${invitationToken.email} is not registered.",
            )
        }.getOrThrow()

        if (user.status != UserStatus.USER_STATUS_ACTIVE) {
            throw FailedPreconditionException(
                "The user with the email ${invitationToken.email} has not verified their email address.",
            )
        }

        // Add user to project
        projectMemberRepo.addUserToProject(user.id, invitationToken.projectId)

        // Remove the invitation token after successful acceptance
        invitationTokenRepo.deleteInvitationToken(invitationToken.token)

        return Base.Nothing.getDefaultInstance()
    }
}
