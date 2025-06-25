package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.repository.IUserTableRepo
import snowballr.Base
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole

interface IUserService {
    /**
     * Service implementation of [SnowballRService.getUserById].
     */
    suspend fun getUserById(id: Base.Id): UserOuterClass.User

    /**
     * Service implementation of [SnowballRService.getUserByEmail].
     */
    suspend fun getUserByEmail(email: Base.Email): UserOuterClass.User

    /**
     * Service implementation of [SnowballRService.getAllUsers].
     */
    suspend fun getAllUsers(): UserOuterClass.User.List

    /**
     * Service implementation of [SnowballRService.softDeleteUser].
     */
    suspend fun softDeleteUser(request: Base.Id): Base.Nothing
}

/**
 * The [UserService] class handles operations related to users by implementing the [IUserService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IUserTableRepo] repository.
 *
 * @constructor Initializes the [UserService] with a user repository.
 * @param repo The repository responsible for managing persistence operations for users.
 */
class UserService(
    private val repo: IUserTableRepo,
) : IUserService {
    override suspend fun getUserById(id: Base.Id): UserOuterClass.User =
        // TODO: think about who can request each user by ID
        repo.getUserById(id.id).toGrpcUser()

    override suspend fun getUserByEmail(email: Base.Email): UserOuterClass.User =
        // TODO: think about who can request each user by email
        repo.getUserByEmail(email.email).toGrpcUser()

    override suspend fun getAllUsers(): UserOuterClass.User.List {
        val currentUser = repo.getUserById(dummyUserId!!)
        // Check whether the current user has access to retrieve all users
        // TODO: remove dummy user when user management is implemented
        if (currentUser.role != UserRole.USER_ROLE_ADMIN) {
            throw UnauthorizedException.All.User(dummyUserId!!)
        }

        val users = repo.getAllUsers()

        val builder = UserOuterClass.User.List.newBuilder()
        users.forEach { builder.addUsers(it.toGrpcUser()) }
        return builder.build()
    }

    override suspend fun softDeleteUser(request: Base.Id): Base.Nothing {
        val currentUser = repo.getUserById(dummyUserId!!)
        val userToDelete = repo.getUserById(request.id)
        val isSameUser = currentUser.id == userToDelete.id

        // Checks, if the user tries to delete another user without being an admin
        if (currentUser.role != UserRole.USER_ROLE_ADMIN && !isSameUser) {
            throw UnauthorizedException.All.User(dummyUserId!!)
        }
        // Checks, if the user tries to delete another user that is an admin (not possible even if the current user is
        // an admin)
        if (userToDelete.role == UserRole.USER_ROLE_ADMIN && !isSameUser) {
            throw FailedPreconditionException(
                "The user with the id ${userToDelete.id} can not be deleted " +
                    "because he is an admin.",
            )
        }

        repo.softDeleteUser(userToDelete.id)
        return Base.Nothing.getDefaultInstance()
    }
}
