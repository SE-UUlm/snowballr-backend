package se.uulm.snowballr.backend.service

import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.formatting.daysToHumanReadable
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.getFullName
import se.uulm.snowballr.backend.model.dto.isActiveAndConfirmed
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.dto.toGrpcUsers
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.accessrules.IProjectAccessChecker
import se.uulm.snowballr.backend.service.accessrules.checkFor
import snowballr.ProjectOuterClass.Project
import snowballr.UserOuterClass.User
import java.time.OffsetDateTime
import java.util.UUID
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
    suspend fun inviteUserToProject(request: GrpcProject.Member.Invite)

    /**
     * Service implementation of [SnowballRService.acceptProjectInvitation].
     */
    suspend fun acceptProjectInvitation(request: GrpcProject.Member.Accept)

    /**
     * Service implementation of [SnowballRService.getPendingInvitationsForProject].
     */
    suspend fun getPendingInvitationsForProject(projectId: UUID): GrpcUser.List
}

/**
 * The [InvitationService] class handles operations related to normal papers by implementing the [IInvitationService]
 * interface.
 *
 * This class serves as a layer that abstracts the responsibility of invitations.
 *
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param invitationTokenRepo The repository responsible for managing persistence operations for invitation tokens.
 * @param emailManager The manager responsible for sending emails.
 * @param envReader The environment reader that provides access to configuration values.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
@Suppress("LongParameterList")
class InvitationService(
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val invitationTokenRepo: IInvitationTokenTableRepo,
    private val emailManager: IEmailManager,
    private val envReader: EnvReader,
    private val projectAccessChecker: IProjectAccessChecker,
) : IInvitationService {
    companion object {
        private const val INVITATION_TOKEN_LENGTH = 48
        private const val MINIMUM_LENGTH_OF_SEARCH_QUERY = 3
    }

    override suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): GrpcUser.List =
        withUser(userRepo) { currentUser ->
            val searchQuery = request.query.trim()

            // Check whether the search query is too short, i.e., 3 or fewer characters long
            if (searchQuery.length < MINIMUM_LENGTH_OF_SEARCH_QUERY) {
                return@withUser GrpcUser.List.getDefaultInstance()
            }

            val excludedUsersFromSearch = mutableSetOf(currentUser.email)
            try {
                val projectId = parseUUID(request.projectId, EntityType.PROJECT)
                val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
                excludedUsersFromSearch += projectMembers.map { it.user.email }

                val invitedMembers = invitationTokenRepo.getActiveInvitationTokensForProject(projectId)
                excludedUsersFromSearch += invitedMembers.map { it.email }
            } catch (_: InvalidUUIDException) {
                Logger.warn { "Invalid project ID in invite candidates request: ${request.projectId}" }
            }

            val candidates = userRepo.getUsersMatchingSearchQuery(searchQuery, excludedUsersFromSearch)
            candidates.toGrpcUsers()
        }

    override suspend fun inviteUserToProject(request: GrpcProject.Member.Invite) = withUser(userRepo) { currentUser ->
        val projectId = parseUUID(request.projectId, EntityType.PROJECT)

        projectAccessChecker.isProjectOrServerAdmin(AccessType.READ)
            .checkFor(currentUser, projectId)

        val project = projectRepo.getProjectById(projectId).getOrThrow()

        projectAccessChecker.isProjectActive().checkFor(currentUser, project)

        // Check if the user is already a member
        val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
        val doesAlreadyExists = projectMembers.any { it.user.email == request.userEmail }
        if (doesAlreadyExists) {
            return@withUser
        }

        // Check if the user is already invited
        val isAlreadyInvited =
            invitationTokenRepo.getInvitationTokenByEmailAndProjectId(request.userEmail, projectId).isSuccess
        if (isAlreadyInvited) {
            return@withUser
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
        val inviterName = currentUser.getFullName()
        val invitationLink = emailManager.createAcceptProjectInvitationLink(invitationToken)
        val expirationTimeInDays = envReader.env.lifetime.invitationTokenLifeTimeInDays
        val data = EmailData.AcceptProjectInvitation(
            userFirstName,
            inviterName,
            project.name,
            invitationLink,
            daysToHumanReadable(expirationTimeInDays),
        )
        emailManager.sendAcceptProjectInvitationEmail(request.userEmail, data)
    }

    override suspend fun acceptProjectInvitation(request: GrpcProject.Member.Accept) {
        val invitationToken = invitationTokenRepo.getInvitationTokenByValue(request.token).getOrThrow()

        // Check if the token has expired
        if (OffsetDateTime.now().isAfter(invitationToken.expiresAt)) {
            invitationTokenRepo.deleteInvitationToken(invitationToken.token)
            throw InvitationTokenNotFoundException()
        }

        // Check if the user is registered and verified
        val user = try {
            userRepo.getUserByEmail(invitationToken.email).getOrThrow()
        } catch (_: NotFoundException) {
            throw FailedPreconditionException("The user with the email ${invitationToken.email} is not registered.")
        }

        if (!user.isActiveAndConfirmed()) {
            throw FailedPreconditionException(
                "The user with the email ${invitationToken.email} has not verified their email address.",
            )
        }

        // Add user to project
        projectMemberRepo.addUserToProject(user.id, invitationToken.projectId)

        // Remove the invitation token after successful acceptance
        invitationTokenRepo.deleteInvitationToken(invitationToken.token)
    }

    override suspend fun getPendingInvitationsForProject(projectId: UUID): GrpcUser.List =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val tokens = invitationTokenRepo.getActiveInvitationTokensForProject(projectId)

            val invitees = tokens.map { token ->
                try {
                    userRepo.getUserByEmail(token.email).getOrThrow().toGrpcUser()
                } catch (_: NotFoundException) {
                    User.newBuilder().setEmail(token.email).build()
                }
            }

            User.List.newBuilder().addAllUsers(invitees).build()
        }
}
