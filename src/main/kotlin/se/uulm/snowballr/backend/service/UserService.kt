package se.uulm.snowballr.backend.service

import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
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
    suspend fun register(request: Authentication.RegisterRequest): Base.Nothing

    /**
     * Service implementation of [SnowballRService.logout].
     */
    suspend fun logout(): Base.Nothing

    /**
     * Service implementation of [SnowballRService.updateUser].
     */
    suspend fun updateUser(request: UserOuterClass.User.Update): UserOuterClass.User

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
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param jwtService The utility for handling JWT operations, such as token parsing and validation.
 */
class UserService(
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val jwtService: IJwtService,
) : IUserService {
    private suspend fun verifyUserAccess(currentUser: User, targetUserId: UUID, identifierType: IdentifierType) {
        // Check whether requesting user is server admin
        if (currentUser.role == UserRole.USER_ROLE_ADMIN) return

        // Check whether requesting user is requested user
        if (targetUserId == currentUser.id) return

        // Check whether requesting user is in a same project as the requested user
        val isInSameProject =
            projectMemberRepo
                .getMembersInSameProjectsAsUser(targetUserId)
                .any { it.userId == currentUser.id }
        if (isInSameProject) return

        // Requesting user is not authorized
        throw UnauthorizedException.Single(
            EntityType.USER,
            targetUserId.toString(),
            AccessType.READ,
            currentUser.id.toString(),
            identifierType,
        )
    }

    override suspend fun getUserById(request: Base.Id): UserOuterClass.User {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val targetUserId = parseUUID(request.id, EntityType.USER)

        verifyUserAccess(currentUser, targetUserId, IdentifierType.ID)

        val isRequestedUser = currentUser.id == targetUserId

        // Don't re-request the user if it is the current user itself
        val result =
            if (isRequestedUser) {
                currentUser
            } else {
                userRepo.getUserById(targetUserId)
            }

        // Only active or active unconfirmed users can be retrieved
        if (result.status != UserStatus.USER_STATUS_ACTIVE &&
            result.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException(EntityType.USER, request.id)
        }

        return result.toGrpcUser()
    }

    override suspend fun getUserByEmail(request: Base.Email): UserOuterClass.User {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())

        // We have to request the user first to get the ID for the access checks
        val targetUser = userRepo.getUserByEmail(request.email)

        verifyUserAccess(currentUser, targetUser.id, IdentifierType.EMAIL)

        // Only active or active unconfirmed users can be retrieved
        if (targetUser.status != UserStatus.USER_STATUS_ACTIVE &&
            targetUser.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException(EntityType.USER, request.email, IdentifierType.EMAIL)
        }

        return targetUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): UserOuterClass.User.List {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())

        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.USER, AccessType.READ, it) }

        val users = userRepo.getAllUsers()

        val builder = UserOuterClass.User.List.newBuilder()
        users.forEach { builder.addUsers(it.toGrpcUser()) }
        return builder.build()
    }

    override suspend fun register(request: Authentication.RegisterRequest): Base.Nothing {
        // Check whether a user with the given email already exists
        if (userRepo.doesUserExistByEmail(request.email)) {
            throw DuplicateEntityException(EntityType.USER, request.email, IdentifierType.EMAIL)
        }

        // Hash the password and create the user
        val passwordHash = PasswordUtils.hashPassword(request.password)
        val user = userRepo.createUser(request, passwordHash)

        // Generate JWT tokens
        val (accessToken, refreshToken) = jwtService.generateTokens(user.id)
        GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun logout(): Base.Nothing {
        GrpcContext.setAuthCookiesInContext("", "")

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun updateUser(request: UserOuterClass.User.Update): UserOuterClass.User {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())

        // Check that user to update exists in the database
        val targetUserId = parseUUID(request.user.id, EntityType.USER)
        val targetUser = userRepo.getUserById(targetUserId)

        // Check whether the current user is a server admin if the role is changed or the requested user is different
        // from the current user
        if (request.mask.pathsList.contains("role") || currentUser.id != targetUser.id) {
            verifyServerAdminRole(currentUser) {
                UnauthorizedException.Single(EntityType.USER, targetUser.id.toString(), AccessType.UPDATE, it)
            }
        }

        // Check whether a user with the given email already exists if the email should be changed
        if (request.mask.pathsList.contains("email") && userRepo.doesUserExistByEmail(request.user.email)) {
            throw DuplicateEntityException(EntityType.USER, request.user.email, IdentifierType.EMAIL)
        }

        val updatedUser = userRepo.updateUser(request)
        return updatedUser.toGrpcUser()
    }

    override suspend fun softDeleteUser(request: Base.Id): Base.Nothing {
        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext())
        val targetUser = userRepo.getUserById(parseUUID(request.id, EntityType.USER))
        val isSameUser = currentUser.id == targetUser.id

        // Checks, if the user tries to delete another user without being an admin
        if (!isSameUser) {
            verifyServerAdminRole(currentUser) {
                UnauthorizedException.Single(EntityType.USER, targetUser.id.toString(), AccessType.DELETE, it)
            }
        }
        // Checks, if the user tries to delete another user that is an admin (not possible even if the current user is
        // an admin)
        if (targetUser.role == UserRole.USER_ROLE_ADMIN && !isSameUser) {
            throw FailedPreconditionException(
                "The user with the id ${targetUser.id} can not be deleted " +
                    "because he is an admin.",
            )
        }

        userRepo.softDeleteUser(targetUser.id)
        return Base.Nothing.getDefaultInstance()
    }
}
