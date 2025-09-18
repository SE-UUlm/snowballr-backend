package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.InvalidIdException
import se.uulm.snowballr.backend.model.dto.toGrpcUsers
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import se.uulm.snowballr.backend.service.UserService.Companion.MINIMUM_LENGTH_OF_SEARCH_QUERY
import snowballr.ProjectOuterClass.Project
import snowballr.UserOuterClass.User as GrpcUser

interface IInvitationService {
    /**
     * Service implementation of [SnowballRService.getInviteCandidates]
     */
    suspend fun getInviteCandidates(request: Project.InviteCandidatesRequest): GrpcUser.List
}

/**
 * The [InvitationService] class handles operations related to normal papers by implementing the [IInvitationService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of invitations.
 *
 * @constructor Initializes the [InvitationService] with the necessary repositories.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 */
class InvitationService(
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
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
}
