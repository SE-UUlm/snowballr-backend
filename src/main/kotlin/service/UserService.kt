package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
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
    suspend fun getUserByEmail(email: Base.Id): UserOuterClass.User

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
 * @param repo The repository responsible for managing persistence operations for users.
 */
class UserService(
    private val repo: IUserTableRepo,
) : IUserService {
    override suspend fun getUserById(id: Base.Id): UserOuterClass.User =
        // TODO: think about who can request each user by ID
        repo.getUserById(id.id).toGrpcUser()

    override suspend fun getUserByEmail(email: Base.Id): UserOuterClass.User {
        // TODO: move this validation to validation layer by using separate Email type
        EMAIL_REGEX.matches(email.id) || throw IllegalArgumentException("Invalid email: ${email.id}")
        return repo.getUserByEmail(email.id).toGrpcUser()
    }

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
}

/**
 * Email regex.
 *
 * See: https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression/201378#201378
 */
@Suppress("MaxLineLength", "StringShouldBeRawString")
val EMAIL_REGEX =
    Regex(
        "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)])",
    )
