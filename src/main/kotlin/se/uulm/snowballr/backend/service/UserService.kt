package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.JwtUtils
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.db.dummyUserId
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.jwt.JwtTokens
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Authentication
import snowballr.Base
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.util.UUID

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

    /**
     * Service implementation of [SnowballRService.register].
     */
    suspend fun register(request: Authentication.RegisterRequest): JwtTokens
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
    private suspend fun verifyUserAccess(currentUser: User, requestedUserId: UUID, identifier: String) {
        // Check whether requesting user is server admin
        if (currentUser.role == UserRole.USER_ROLE_ADMIN) return

        // Check whether requesting user is requested user
        if (requestedUserId == currentUser.id) return

        // Check whether requesting user is in a same project as the requested user
        val isInSameProject =
            projectMemberRepo
                .getMembersInSameProjectsAsUser(requestedUserId)
                .any { it.userId == currentUser.id }
        if (isInSameProject) return

        // Requesting user is not authorized
        throw UnauthorizedException.Single.User(currentUser.id.toString(), identifier, requestedUserId.toString())
    }

    override suspend fun getUserById(request: Base.Id): UserOuterClass.User {
        val requestingUserId = parseUUID(dummyUserId!!, EntityType.USER)
        val requestedUserId = parseUUID(request.id, EntityType.USER)
        val currentUser = userRepo.getUserById(requestingUserId)

        verifyUserAccess(currentUser, requestingUserId, "ID")

        val isRequestedUser = requestingUserId == currentUser.id

        // Don't re-request the user if it is the current user itself
        val result =
            if (isRequestedUser) {
                currentUser
            } else {
                userRepo.getUserById(requestedUserId)
            }

        // Only active or active unconfirmed users can be retrieved
        if (result.status != UserStatus.USER_STATUS_ACTIVE &&
            result.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException.User(request.id)
        }

        return result.toGrpcUser()
    }

    override suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User {
        val requestingUserId = parseUUID(dummyUserId!!, EntityType.USER)
        val currentUser = userRepo.getUserById(requestingUserId)

        // We have to request the user first to get the ID for the access checks
        val requestedUser = userRepo.getUserByEmail(request.email)

        verifyUserAccess(currentUser, requestingUserId, "email")

        // Only active or active unconfirmed users can be retrieved
        if (requestedUser.status != UserStatus.USER_STATUS_ACTIVE &&
            requestedUser.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException.User(request.email, "email")
        }

        return requestedUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): UserOuterClass.User.List {
        val requestingUserId = parseUUID(dummyUserId!!, EntityType.USER)
        val currentUser = userRepo.getUserById(requestingUserId)

        verifyServerAdminRole(currentUser) { UnauthorizedException.All.User(it) }

        val users = userRepo.getAllUsers()

        val builder = UserOuterClass.User.List.newBuilder()
        users.forEach { builder.addUsers(it.toGrpcUser()) }
        return builder.build()
    }

    override suspend fun register(request: Authentication.RegisterRequest): JwtTokens {
        // Check whether a user with the given email already exists
        if (userRepo.doesUserExistByEmail(request.email)) {
            throw DuplicateEntityException.UserEmail(request.email)
        }

        // Hash the password and create the user
        val passwordHash = PasswordUtils.hashPassword(request.password)
        val user = userRepo.createUser(request, passwordHash)

        // Generate JWT tokens
        val tokens = JwtUtils.generateTokens(user.id)

        return tokens
    }
}
