package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.AccessType
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.DuplicateEntityException
import se.uulm.snowballr.backend.model.SnowballRException.EntityNotActiveException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import se.uulm.snowballr.backend.model.SnowballRException.UnauthorizedException
import se.uulm.snowballr.backend.model.SnowballRException.UserNotFoundException
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
import se.uulm.snowballr.backend.service.accessrules.andAlso
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.forProperty
import se.uulm.snowballr.backend.service.accessrules.forTarget
import se.uulm.snowballr.backend.service.accessrules.isAllowedToReadUser
import se.uulm.snowballr.backend.service.accessrules.isSameUserById
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.isServerAdminOrSameUser
import se.uulm.snowballr.backend.service.accessrules.isTargetUserActive
import se.uulm.snowballr.backend.service.accessrules.orElse
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import se.uulm.snowballr.backend.service.accessrules.targetUserIsNotAdmin
import snowballr.Authentication
import snowballr.Base
import snowballr.nothing
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion
import snowballr.UserOuterClass.User as GrpcUser
import snowballr.UserSettingsOuterClass.UserSettings as GrpcUserSettings

val Logger = KotlinLogging.logger { }

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
     * Service implementation of [SnowballRService.register].
     */
    suspend fun register(request: Authentication.RegisterRequest): Base.Nothing

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
    suspend fun getUserSettings(): GrpcUserSettings

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
 * @param emailManager The manager responsible for sending emails.
 */
class UserService(
    private val userRepo: IUserTableRepo,
    private val projectMemberRepo: IProjectMemberTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val verificationTokenRepo: IVerificationTokenTableRepo,
    private val emailManager: IEmailManager,
) : IUserService {
    override suspend fun getUserById(request: Base.Id): GrpcUser = withUser(userRepo) { currentUser ->
        val targetUserId = parseUUID(request.id, EntityType.USER)

        isAllowedToReadUser(projectMemberRepo).checkFor(currentUser, targetUserId)

        val isRequestedUser = currentUser.id == targetUserId

        // Don't re-request the user if it is the current user itself
        val targetUser =
            if (isRequestedUser) {
                currentUser
            } else {
                userRepo.getUserById(targetUserId).getOrThrow()
            }

        // Only active or active unconfirmed users can be retrieved if the requester is not a server admin
        isServerAdmin.forTarget<User>()
            .orElse(isTargetUserActive)
            .orElseThrow(UserNotFoundException(request.id))
            .checkFor(currentUser, targetUser)

        targetUser.toGrpcUser()
    }

    override suspend fun getUserByEmail(request: Base.Email): GrpcUser = withUser(userRepo) { currentUser ->
        // We have to request the user first to get the ID for the access checks
        val targetUser = userRepo.getUserByEmail(request.email).getOrThrow()

        isAllowedToReadUser(projectMemberRepo, IdentifierType.EMAIL)
            .forProperty(User::id)
            // Only active or active unconfirmed users can be retrieved if the requester is not a server admin
            .andAlso(
                isServerAdmin.forTarget<User>()
                    .orElse(isTargetUserActive)
                    .orElseThrow(UserNotFoundException(request.email, IdentifierType.EMAIL)),
            )
            .checkFor(currentUser, targetUser)

        targetUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): GrpcUser.List = withUser(userRepo) { currentUser ->
        isServerAdmin
            .orElseThrow(UnauthorizedException.All(EntityType.USER, AccessType.READ, currentUser.id.toString()))
            .checkFor(currentUser, Unit)

        userRepo.getAllUsers().toGrpcUsers()
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
                verificationLink,
            ),
        )

        return Base.Nothing.getDefaultInstance()
    }

    override suspend fun updateUser(request: GrpcUser.Update): GrpcUser = withUser(userRepo) { currentUser ->
        val targetUserId = parseUUID(request.user.id, EntityType.USER)
        val targetUser = userRepo.getUserById(targetUserId).getOrThrow()

        val notAllowedToUpdateException = UnauthorizedException.Single(
            EntityType.USER,
            targetUser.id.toString(),
            AccessType.UPDATE,
            currentUser.id.toString(),
        )

        isSameUserById
            .forProperty(User::id)
            .orElse(
                isServerAdmin.forTarget<User>()
                    .andAlso(
                        isTargetUserActive
                            .orElseThrow(EntityNotActiveException(EntityType.USER, targetUserId.toString())),
                    ),
            )
            .orElseThrow(notAllowedToUpdateException)
            .checkFor(currentUser, targetUser)

        // If the role is changed, the requesting user must be a server admin.
        if (request.mask.pathsList.contains("role")) {
            isServerAdmin.forTarget<UUID>()
                .orElseThrow(notAllowedToUpdateException)
                .checkFor(currentUser, targetUserId)
        }

        // If the email is changed, there must not yet exist an account with that email address.
        if (request.mask.pathsList.contains("email") && userRepo.doesUserExistByEmail(request.user.email)) {
            throw DuplicateEntityException(EntityType.USER, request.user.email, identifierType = IdentifierType.EMAIL)
        }

        userRepo.updateUser(request).toGrpcUser()
    }

    override suspend fun softDeleteUser(request: Base.Id): Base.Nothing = withUser(userRepo) { currentUser ->
        val targetUser = userRepo.getUserById(parseUUID(request.id, EntityType.USER)).getOrThrow()

        isServerAdminOrSameUser
            .orElseThrow(
                UnauthorizedException.Single(
                    EntityType.USER,
                    targetUser.id.toString(),
                    AccessType.DELETE,
                    currentUser.id.toString(),
                ),
            )
            .forProperty(User::id)
            .andAlso(
                targetUserIsNotAdmin
                    .orElse(isSameUserById.forProperty(User::id))
                    .orElseThrow(
                        FailedPreconditionException(
                            "The user with the id ${targetUser.id} can not be deleted because the user is an admin.",
                        ),
                    ),
            )
            .checkFor(currentUser, targetUser)

        userRepo.softDeleteUser(targetUser.id)

        nothing { }
    }

    override suspend fun getCurrentUser(): GrpcUser = withUser(userRepo, User::toGrpcUser)

    override suspend fun getUserSettings(): GrpcUserSettings = withUser(userRepo) { currentUser ->
        val userSettings = userRepo.getUserSettings(currentUser.id).getOrThrow()
        val defaultUserCriteria = criterionRepo.getCriteriaByIds(userSettings.criteriaIds)

        val criteria = mutableListOf<GrpcCriterion>()
        for (criterion in defaultUserCriteria) {
            criteria.add(
                GrpcCriterion
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
