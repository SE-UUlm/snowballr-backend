package se.uulm.snowballr.backend.service

import com.google.protobuf.FieldMask
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtManager
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.exception.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.exception.SnowballRException.UnauthenticatedException
import se.uulm.snowballr.backend.model.exception.notfound.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import snowballr.Authentication
import snowballr.Base
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import snowballr.UserOuterClass.User as GrpcUser

interface IAuthenticationService {
    /**
     * Service implementation of [SnowballRService.verifyEmail].
     */
    suspend fun verifyEmail(request: Authentication.VerifyEmailRequest): Base.Nothing

    /**
     * Service implementation of [SnowballRService.logout].
     */
    suspend fun logout(): Base.Nothing

    /**
     * Service implementation of [SnowballRService.login].
     */
    suspend fun login(request: Authentication.LoginRequest): Base.Nothing
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
    override suspend fun verifyEmail(request: Authentication.VerifyEmailRequest): Base.Nothing {
        val verificationToken = verificationTokenRepo.getVerificationTokenByValue(request.token).getOrThrow()

        // Check if the token has expired
        if (OffsetDateTime.now().isAfter(verificationToken.expiresAt)) {
            verificationTokenRepo.deleteVerificationToken(request.token)
            throw VerificationTokenNotFoundException()
        }

        // Check whether the user exists
        val user = repo.getUserById(verificationToken.userId).getOrThrow()

        // Update the user's status to active
        val updatedUser = user.copy(status = UserStatus.USER_STATUS_ACTIVE)
        val userUpdate = GrpcUser.Update.newBuilder()
            .setUser(updatedUser.toGrpcUser())
            .setMask(FieldMask.newBuilder().addPaths("user.status").build())
            .build()
        repo.updateUser(userUpdate)

        // Remove the verification token after successful verification
        verificationTokenRepo.deleteVerificationToken(request.token)

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun logout(): Base.Nothing {
        GrpcContext.setAuthCookiesInContext("", "")

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun login(request: Authentication.LoginRequest): Base.Nothing {
        // Check whether a user with the given email exists
        val user =
            try {
                repo.getUserByEmail(request.email).getOrThrow()
            } catch (_: NotFoundException) {
                throw UnauthenticatedException()
            }

        // Check whether the user is active (verified email)
        if (user.status != UserStatus.USER_STATUS_ACTIVE) {
            throw UnauthenticatedException()
        }

        // Verify the password against the stored hash
        val storedPasswordHash = try {
            repo.getPasswordHashByEmail(request.email).getOrThrow()
        } catch (_: NotFoundException) {
            throw UnauthenticatedException()
        }

        if (!PasswordUtils.verifyPassword(request.password, storedPasswordHash)) {
            throw UnauthenticatedException()
        }

        // Generate JWT tokens
        val (accessToken, refreshToken) = jwtManager.generateAuthTokens(user.id)
        GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)

        return Base.Nothing.getDefaultInstance()
    }
}
