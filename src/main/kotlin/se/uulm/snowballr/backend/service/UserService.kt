package se.uulm.snowballr.backend.service

import com.google.protobuf.FieldMask
import io.github.oshai.kotlinlogging.KotlinLogging
import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.auth.GrpcContext
import se.uulm.snowballr.backend.auth.IJwtService
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthenticatedException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.SnowballRException.VerificationTokenNotFoundException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.dto.toGrpcUserSettings
import se.uulm.snowballr.backend.model.dto.toGrpcUsers
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.repository.association.IProjectMemberTableRepo
import snowballr.Authentication
import snowballr.Base
import snowballr.CriterionOuterClass
import snowballr.ProjectOuterClass
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import snowballr.UserSettingsOuterClass
import snowballr.nothing
import java.time.OffsetDateTime
import java.util.UUID
import snowballr.UserOuterClass.User as GrpcUser

val Logger = KotlinLogging.logger { }

@Suppress("ComplexInterface", "TooManyFunctions")
interface IUserService {
    /**
     * Service implementation of [SnowballRService.getUserById].
     */
    suspend fun getUserById(request: Base.Id): GrpcUser

    /**
     * Service implementation of [SnowballRService.getUserByEmail].
     */
    suspend fun getUserByEmail(request: Base.Email): GrpcUser

    /**
     * Service implementation of [SnowballRService.getAllUsers].
     */
    suspend fun getAllUsers(): GrpcUser.List

    /**
     * Service implementation of [SnowballRService.getInviteCandidates]
     */
    suspend fun getInviteCandidates(request: ProjectOuterClass.Project.InviteCandidatesRequest): GrpcUser.List

    /**
     * Service implementation of [SnowballRService.register].
     */
    suspend fun register(request: Authentication.RegisterRequest): Base.Nothing

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

    /**
     * Service implementation of [SnowballRService.updateUser].
     */
    suspend fun updateUser(request: GrpcUser.Update): GrpcUser

    /**
     * Service implementation of [SnowballRService.softDeleteUser].
     */
    suspend fun softDeleteUser(request: Base.Id): Base.Nothing

    /**
     * Service implementation of [SnowballRService.getUserSettings].
     */
    suspend fun getUserSettings(): UserSettingsOuterClass.UserSettings

    /**
     * Service implementation of [SnowballRService.getCurrentUser].
     */
    suspend fun getCurrentUser(): GrpcUser
}

private const val VERIFICATION_TOKEN_LENGTH = 48

/**
 * The [UserService] class handles operations related to users by implementing the [IUserService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IUserTableRepo] repository.
 *
 * @constructor Initializes the [UserService] with a user repository.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectMemberRepo The repository responsible for managing persistence operations for project members.
 * @param criterionRepo The repository responsible for managing persistence operations for criteria.
 * @param verificationTokenRepo The repository responsible for managing persistence operations for verification tokens.
 * @param jwtService The utility for handling JWT operations, such as token parsing and validation.
 * @param emailManager The manager responsible for sending emails.
 */
@Suppress("TooManyFunctions")
class UserService(
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val verificationTokenRepo: IVerificationTokenTableRepo,
    private val jwtService: IJwtService,
    private val emailManager: IEmailManager,
) : IUserService {
    companion object {
        const val MINIMUM_LENGTH_OF_SEARCH_QUERY = 3
    }

    private suspend fun verifyUserAccess(currentUser: User, targetUserId: UUID, identifierType: IdentifierType) {
        // Check whether the requesting user is server admin
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

    override suspend fun getUserById(request: Base.Id): GrpcUser = withUser(userRepo) { currentUser ->
        val targetUserId = parseUUID(request.id, EntityType.USER)

        verifyUserAccess(currentUser, targetUserId, IdentifierType.ID)

        val isRequestedUser = currentUser.id == targetUserId

        // Don't re-request the user if it is the current user itself
        val result =
            if (isRequestedUser) {
                currentUser
            } else {
                userRepo.getUserById(targetUserId).getOrThrow()
            }

        // Only active or active unconfirmed users can be retrieved
        if (result.status != UserStatus.USER_STATUS_ACTIVE &&
            result.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException(EntityType.USER, request.id)
        }

        result.toGrpcUser()
    }

    override suspend fun getUserByEmail(request: Base.Email): GrpcUser = withUser(userRepo) { currentUser ->
        // We have to request the user first to get the ID for the access checks
        val targetUser = userRepo.getUserByEmail(request.email).getOrThrow()

        verifyUserAccess(currentUser, targetUser.id, IdentifierType.EMAIL)

        // Only active or active unconfirmed users can be retrieved
        if (targetUser.status != UserStatus.USER_STATUS_ACTIVE &&
            targetUser.status != UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ) {
            throw NotFoundException(EntityType.USER, request.email, identifierType = IdentifierType.EMAIL)
        }

        targetUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): GrpcUser.List = withUser(userRepo) { currentUser ->
        verifyServerAdminRole(currentUser) { UnauthorizedException.All(EntityType.USER, AccessType.READ, it) }

        userRepo.getAllUsers().toGrpcUsers()
    }

    override suspend fun getInviteCandidates(
        request: ProjectOuterClass.Project.InviteCandidatesRequest,
    ): GrpcUser.List {
        val searchQuery = request.query.trim()

        // Check whether the search query is too short, i.e., 3 or fewer characters long
        if (searchQuery.length < MINIMUM_LENGTH_OF_SEARCH_QUERY) {
            return GrpcUser.List.getDefaultInstance()
        }

        val currentUser = userRepo.getUserById(GrpcContext.getUserIdFromContext()).getOrThrow()
        val excludedUsersFromSearch = mutableSetOf(currentUser.id)
        try {
            val projectMembers = projectMemberRepo.getProjectMembers(parseUUID(request.projectId, EntityType.PROJECT))
            excludedUsersFromSearch += projectMembers.map { it.userId }
        } catch (_: SnowballRException.InvalidIdException.UUID) {
            Logger.warn { "Invalid project ID in invite candidates request: ${request.projectId}" }
        }

        val candidates = userRepo.getUsersMatchingSearchQuery(searchQuery, excludedUsersFromSearch)

        return candidates.toGrpcUsers()
    }

    override suspend fun register(request: Authentication.RegisterRequest): Base.Nothing {
        // Check whether a user with the given email already exists
        if (userRepo.doesUserExistByEmail(request.email)) {
            throw DuplicateEntityException(EntityType.USER, request.email, identifierType = IdentifierType.EMAIL)
        }

        // Hash the password and create the user
        val passwordHash = PasswordUtils.hashPassword(request.password)
        val user = userRepo.createUser(request, passwordHash)

        // Generate and save verification token for the user
        val verificationToken = NanoId.generate(VERIFICATION_TOKEN_LENGTH)
        verificationTokenRepo.saveVerificationToken(user.id, verificationToken)

        // Send verification email
        val verificationLink = emailManager.createVerificationLink(verificationToken)
        emailManager.sendVerificationEmail(
            user.email,
            EmailData.EmailVerification(
                user.firstName,
                user.lastName,
                verificationLink,
            ),
        )

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun verifyEmail(request: Authentication.VerifyEmailRequest): Base.Nothing {
        val verificationToken = verificationTokenRepo.getVerificationTokenByValue(request.token)
            ?: throw VerificationTokenNotFoundException()

        // Check if the token has expired
        if (OffsetDateTime.now().isAfter(verificationToken.expiresAt)) {
            verificationTokenRepo.deleteVerificationToken(request.token)
            throw VerificationTokenNotFoundException()
        }

        // Check whether the user exists
        val user = userRepo.getUserById(verificationToken.userId).getOrThrow()

        // Update the user's status to active
        val updatedUser = user.copy(status = UserStatus.USER_STATUS_ACTIVE)
        val userUpdate = GrpcUser.Update.newBuilder()
            .setUser(updatedUser.toGrpcUser())
            .setMask(FieldMask.newBuilder().addPaths("user.status").build())
            .build()
        userRepo.updateUser(userUpdate)

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
                userRepo.getUserByEmail(request.email).getOrThrow()
            } catch (_: NotFoundException) {
                throw UnauthenticatedException()
            }

        // Check whether the user is active (verified email)
        if (user.status != UserStatus.USER_STATUS_ACTIVE) {
            throw UnauthenticatedException()
        }

        // Verify the password against the stored hash
        val storedPasswordHash = try {
            userRepo.getPasswordHashByEmail(request.email).getOrThrow()
        } catch (_: NotFoundException) {
            throw UnauthenticatedException()
        }

        if (!PasswordUtils.verifyPassword(request.password, storedPasswordHash)) {
            throw UnauthenticatedException()
        }

        // Generate JWT tokens
        val (accessToken, refreshToken) = jwtService.generateAuthTokens(user.id)
        GrpcContext.setAuthCookiesInContext(accessToken, refreshToken)

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun updateUser(request: GrpcUser.Update): GrpcUser = withUser(userRepo) { currentUser ->
        // Check that user to update exists in the database
        val targetUserId = parseUUID(request.user.id, EntityType.USER)
        val targetUser = userRepo.getUserById(targetUserId).getOrThrow()

        // Check whether the current user is a server admin if the role is changed or the requested user is different
        // from the current user
        if (request.mask.pathsList.contains("role") || currentUser.id != targetUser.id) {
            verifyServerAdminRole(currentUser) {
                UnauthorizedException.Single(EntityType.USER, targetUser.id.toString(), AccessType.UPDATE, it)
            }
        }

        // Check whether a user with the given email already exists if the email should be changed
        if (request.mask.pathsList.contains("email") && userRepo.doesUserExistByEmail(request.user.email)) {
            throw DuplicateEntityException(EntityType.USER, request.user.email, identifierType = IdentifierType.EMAIL)
        }

        userRepo.updateUser(request).toGrpcUser()
    }

    override suspend fun softDeleteUser(request: Base.Id): Base.Nothing = withUser(userRepo) { currentUser ->
        val targetUser = userRepo.getUserById(parseUUID(request.id, EntityType.USER)).getOrThrow()
        val isSameUser = currentUser.id == targetUser.id

        // Checks if the user tries to delete another user without being an admin
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

        nothing { }
    }

    override suspend fun getCurrentUser(): GrpcUser = withUser(userRepo, User::toGrpcUser)

    override suspend fun getUserSettings(): UserSettingsOuterClass.UserSettings = withUser(userRepo) { currentUser ->
        val userSettings = userRepo.getUserSettings(currentUser.id).getOrThrow()
        val defaultUserCriteria = criterionRepo.getCriteriaByIds(userSettings.criteriaIds)

        val criteria = mutableListOf<CriterionOuterClass.Criterion>()
        for (criterion in defaultUserCriteria) {
            criteria.add(
                CriterionOuterClass.Criterion
                    .newBuilder()
                    .setTag(criterion.tag)
                    .setName(criterion.name)
                    .setDescription(criterion.description)
                    .setCategory(criterion.category)
                    .build(),
            )
        }

        userSettings.toGrpcUserSettings(criteria)
    }
}
