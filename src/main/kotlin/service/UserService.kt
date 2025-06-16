package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.repository.IUserTableRepo
import snowballr.Base
import snowballr.UserOuterClass

interface IUserService {
    suspend fun getUserById(id: Base.Id): UserOuterClass.User
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
}
