package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Base
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole

interface IUserService {
    /**
     * Service implementation of [SnowballRService.getUserById].
     */
    suspend fun getUserById(request: Base.Id): UserOuterClass.User

    /**
     * Service implementation of [SnowballRService.getUserByEmail].
     */
    suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User

    /**
     * Service implementation of [SnowballRService.getAllUsers].
     */
    suspend fun getAllUsers(): UserOuterClass.User.List
}

/**
 * The [UserService] class handles operations related to users by implementing the [IUserService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IUserTableRepo] repository.
 *
 * @constructor Initializes the [UserService] with a user repository.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 */
class UserService(
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
) : IUserService {
    private suspend fun verifyUserAccess(
        currentUser: User,
        requestedUserId: String,
        identifier: String,
    ) {
        // Check whether requesting user is server admin
        if (currentUser.role == UserRole.USER_ROLE_ADMIN) return

        // Check whether requesting user is requested user
        if (requestedUserId == currentUser.id.toString()) return

        // Check whether requesting user is in a same project as the requested user
        val isInSameProject =
            projectMemberRepo
                .getProjectMembersInSameProjectsAsUser(currentUser.id.toString())
                .any { it.userId == currentUser.id }
        if (isInSameProject) return

        // Requesting user is not authorized
        throw UnauthorizedException.Single.User(currentUser.id.toString(), identifier, requestedUserId)
    }

    override suspend fun getUserById(request: Base.Id): UserOuterClass.User {
        val currentUser = userRepo.getUserById(dummyUserId!!)

        verifyUserAccess(currentUser, request.id, "ID")

        val isRequestedUser = request.id == currentUser.id.toString()

        // Don't rerequest the user if it is the current user itself
        return if (isRequestedUser) {
            currentUser.toGrpcUser()
        } else {
            userRepo.getUserById(request.id).toGrpcUser()
        }
    }

    override suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User {
        val currentUser = userRepo.getUserById(dummyUserId!!)

        // We have to request the user first to get the ID for the access checks
        val requestedUser = userRepo.getUserByEmail(request.email)

        verifyUserAccess(currentUser, requestedUser.id.toString(), "email")

        return requestedUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): UserOuterClass.User.List {
        val currentUser = userRepo.getUserById(dummyUserId!!)
        // Check whether the current user has access to retrieve all users
        // TODO: remove dummy user when user management is implemented
        if (currentUser.role != UserRole.USER_ROLE_ADMIN) {
            throw UnauthorizedException.All.User(dummyUserId!!)
        }

        val users = userRepo.getAllUsers()

        val builder = UserOuterClass.User.List.newBuilder()
        users.forEach { builder.addUsers(it.toGrpcUser()) }
        return builder.build()
    }
}
