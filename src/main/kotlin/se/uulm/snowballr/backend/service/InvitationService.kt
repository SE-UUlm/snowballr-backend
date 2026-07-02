package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.access.IInvitationAccessChecker
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.formatting.daysToHumanReadable
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.getFullName
import se.uulm.snowballr.backend.model.dto.user.isActiveAndConfirmed
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.FailedPreconditionException
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.invalidargument.InvalidUUIDException
import se.uulm.snowballr.backend.model.exception.notfound.InvitationTokenNotFoundException
import se.uulm.snowballr.backend.model.outgoing.invitation.InvitationResponse
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IInvitationTokenTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.ProjectOuterClass.Project
import java.time.OffsetDateTime
import java.util.UUID

val Logger = KotlinLogging.logger { }

interface IInvitationService {
    /**
     * Service implementation of [SnowballRService.getInviteCandidates].
     */
    suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): List<User>

    /**
     * Service implementation of [SnowballRService.inviteUserToProject].
     */
    suspend fun inviteUserToProject(projectId: UUID, userEmail: String)

    /**
     * Service implementation of [SnowballRService.acceptProjectInvitation].
     */
    suspend fun acceptProjectInvitation(token: String)

    /**
     * Service implementation of [SnowballRService.getPendingInvitationsForProject].
     */
    suspend fun getPendingInvitationsForProject(projectId: UUID): List<InvitationResponse>
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
 * @param accessChecker Interface for checking access permissions for invitations based on defined rules.
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
    private val accessChecker: IInvitationAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
) : IInvitationService {
    companion object {
        private const val INVITATION_TOKEN_LENGTH = 48
        private const val MINIMUM_LENGTH_OF_SEARCH_QUERY = 3
    }

    override suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): List<User> =
        withUser(userRepo) { currentUser ->
            val searchQuery = request.query.trim()

            // Check whether the search query is too short, i.e., 3 or fewer characters long
            if (searchQuery.length < MINIMUM_LENGTH_OF_SEARCH_QUERY) {
                return@withUser emptyList()
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

            userRepo.getUsersMatchingSearchQuery(searchQuery, excludedUsersFromSearch)
        }

    override suspend fun inviteUserToProject(projectId: UUID, userEmail: String) = withUser(userRepo) { currentUser ->
        val projectResult = projectRepo.getProjectById(projectId)
        accessChecker.isAllowedToInviteUserToProject(currentUser, projectId, projectResult)
        val project = projectResult.getOrThrow()

        // Check if the user is already a member
        val projectMembers = projectMemberRepo.getProjectMembersWithUsers(projectId)
        val doesAlreadyExists = projectMembers.any { it.user.email == userEmail }
        if (doesAlreadyExists) {
            return@withUser
        }

        // Check if the user is already invited
        val isAlreadyInvited =
            invitationTokenRepo.getInvitationTokenByEmailAndProjectId(userEmail, projectId).isSuccess
        if (isAlreadyInvited) {
            return@withUser
        }

        // Generate and save invitation token
        val invitationToken = NanoId.generate(INVITATION_TOKEN_LENGTH)
        invitationTokenRepo.saveInvitationToken(userEmail, projectId, invitationToken)

        // Get first name of user if exists
        val userFirstName = try {
            userRepo.getUserByEmail(userEmail).getOrThrow().firstName
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
        emailManager.sendAcceptProjectInvitationEmail(userEmail, data)
    }

    override suspend fun acceptProjectInvitation(token: String) {
        val invitationToken = invitationTokenRepo.getInvitationTokenByValue(token).getOrThrow()

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

    override suspend fun getPendingInvitationsForProject(projectId: UUID): List<InvitationResponse> =
        withUser(userRepo) { currentUser ->
            projectAccessChecker.isAllowedToReadProject(currentUser, projectId)

            val tokens = invitationTokenRepo.getActiveInvitationTokensForProject(projectId)

            val invitees = tokens.map { token ->
                try {
                    val user = userRepo.getUserByEmail(token.email).getOrThrow()
                    InvitationResponse.fromUser(user)
                } catch (_: NotFoundException) {
                    InvitationResponse.fromEmail(token.email)
                }
            }

            invitees
        }
}
