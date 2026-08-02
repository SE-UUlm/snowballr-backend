package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.auth.setAuthCookies
import se.uulm.snowballr.backend.context.RequestContext
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.exception.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.failedprecondition.EntityNotActiveException
import se.uulm.snowballr.backend.model.exception.invalidargument.IncorrectOldPasswordException
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.model.incoming.authentication.ChangePasswordRequest
import se.uulm.snowballr.backend.model.incoming.authentication.LoginRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import java.time.OffsetDateTime

private val logger = KotlinLogging.logger {}

interface IAuthenticationService {
    /**
     * Service implementation of [SnowballRService.verifyEmail].
     */
    suspend fun verifyEmail(token: String)

    /**
     * Service implementation of [SnowballRService.logout].
     */
    suspend fun logout()

    /**
     * Service implementation of [SnowballRService.login].
     */
    suspend fun login(request: LoginRequest)

    /**
     * Service implementation of [SnowballRService.changePassword].
     */
    suspend fun changePassword(request: ChangePasswordRequest)
}

/**
 * The [AuthenticationService] class handles operations related to the authentication by implementing the
 * [IAuthenticationService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of authentication CRUD operations,
 * delegating the actual persistence operations to the [IAuthenticationService] repository.
 *
 * @constructor Initializes the [AuthenticationService] with a user repository.
 * @param repo The repository responsible for managing persistence operations for normal papers.
 * @param verificationTokenRepo The repository responsible for managing persistence operations for verification tokens.
 * @param jwtManager The utility for handling JWT operations, such as token parsing and validation.
 */
class AuthenticationService(
    private val repo: IUserTableRepo,
    private val verificationTokenRepo: IVerificationTokenTableRepo,
    private val jwtManager: IJwtManager,
) : IAuthenticationService {
    override suspend fun verifyEmail(token: String) {
        val verificationToken = verificationTokenRepo.getVerificationTokenByValue(token).getOrThrow()

        // Check if the token has expired
        if (OffsetDateTime.now().isAfter(verificationToken.expiresAt)) {
            logger.debug { "Email verification failed: token expired for user ${verificationToken.userId}" }
            verificationTokenRepo.deleteVerificationToken(token)
            throw VerificationTokenNotFoundException()
        }

        // Check whether the user exists
        val user = repo.getUserById(verificationToken.userId).getOrThrow()

        // Update the user's status to active
        val updatedUser = user.copy(status = UserStatus.ACTIVE)
        val userUpdate = UpdateUserRequest(
            userId = updatedUser.id,
            firstName = updatedUser.firstName,
            lastName = updatedUser.lastName,
            email = updatedUser.email,
            role = updatedUser.role,
            status = updatedUser.status,
        )
        repo.updateUser(userUpdate, listOf("user.status"))

        // Remove the verification token after successful verification
        verificationTokenRepo.deleteVerificationToken(token)
        logger.info { "Email verified for user ${user.id}" }
    }

    override suspend fun logout() {
        val userId = RequestContext.current().userId
        RequestContext.current().setAuthCookies("", "")
        logger.info { "User ${userId ?: "<unknown>"} logged out" }
    }

    @Suppress("ThrowsCount")
    override suspend fun login(request: LoginRequest) {
        // Check whether a user with the given email exists
        val user =
            try {
                repo.getUserByEmail(request.email).getOrThrow()
            } catch (_: NotFoundException) {
                logger.debug { "Login failed: no account found for email '${request.email}'" }
                throw UnauthenticatedException()
            }

        if (!user.isActiveAndConfirmed) {
            logger.debug { "Login failed: user ${user.id} is not active or confirmed (status: ${user.status})" }
            throw UnauthenticatedException()
        }

        // Verify the password against the stored hash
        val storedPasswordHash = try {
            repo.getPasswordHashByEmail(request.email).getOrThrow()
        } catch (_: NotFoundException) {
            logger.error { "Login failed: no password hash found for user ${user.id}" }
            throw UnauthenticatedException()
        }

        if (!PasswordUtils.verifyPassword(request.password, storedPasswordHash)) {
            logger.debug { "Login failed: incorrect password for user ${user.id}" }
            throw UnauthenticatedException()
        }

        // Generate JWT tokens
        val (accessToken, refreshToken) = jwtManager.generateAuthTokens(user.id)
        RequestContext.current().setAuthCookies(accessToken, refreshToken)
        logger.info { "User ${user.id} logged in" }
    }

    override suspend fun changePassword(request: ChangePasswordRequest) = withUser(repo) { currentUser ->
        if (!currentUser.isActiveAndConfirmed) {
            throw EntityNotActiveException(EntityType.USER, currentUser.id)
        }

        val storedPasswordHash = repo.getPasswordHashByEmail(currentUser.email).getOrThrow()
        if (!PasswordUtils.verifyPassword(request.oldPassword, storedPasswordHash)) {
            logger.debug { "Password change failed: incorrect old password for user ${currentUser.id}" }
            throw IncorrectOldPasswordException()
        }

        val newPasswordHash = PasswordUtils.hashPassword(request.newPassword)
        repo.updatePasswordHash(currentUser.id, newPasswordHash)
        logger.info { "Password changed for user ${currentUser.id}" }
    }
}
