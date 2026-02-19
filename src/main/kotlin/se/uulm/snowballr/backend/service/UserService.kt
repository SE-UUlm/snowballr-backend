package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.formatting.daysToHumanReadable
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.toGrpcUser
import se.uulm.snowballr.backend.model.dto.toGrpcUserSettings
import se.uulm.snowballr.backend.model.dto.toGrpcUsers
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.model.exception.unauthorized.UnauthorizedReadAllException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import se.uulm.snowballr.backend.service.accessrules.IProjectAccessChecker
import se.uulm.snowballr.backend.service.accessrules.IUserAccessChecker
import se.uulm.snowballr.backend.service.accessrules.checkFor
import se.uulm.snowballr.backend.service.accessrules.isServerAdmin
import se.uulm.snowballr.backend.service.accessrules.orElseThrow
import snowballr.Authentication
import snowballr.ProjectOuterClass.ProjectStatus
import java.util.UUID
import snowballr.CriterionOuterClass.Criterion as GrpcCriterion
import snowballr.UserOuterClass.User as GrpcUser
import snowballr.UserSettingsOuterClass.UserSettings as GrpcUserSettings

val Logger = KotlinLogging.logger { }

interface IUserService {
    /**
     * Service implementation of [SnowballRService.getUserById].
     */
    suspend fun getUserById(userId: UUID): GrpcUser

    /**
     * Service implementation of [SnowballRService.getUserByEmail].
     */
    suspend fun getUserByEmail(email: String): GrpcUser

    /**
     * Service implementation of [SnowballRService.getAllUsers].
     */
    suspend fun getAllUsers(): GrpcUser.List

    /**
     * Service implementation of [SnowballRService.register].
     */
    suspend fun register(request: Authentication.RegisterRequest)

    /**
     * Service implementation of [SnowballRService.updateUser].
     */
    suspend fun updateUser(request: GrpcUser.Update): GrpcUser

    /**
     * Service implementation of [SnowballRService.softDeleteUser].
     */
    suspend fun softDeleteUser(userId: UUID)

    /**
     * Service implementation of [SnowballRService.getUserSettings].
     */
    suspend fun getUserSettings(): GrpcUserSettings

    /**
     * Service implementation of [SnowballRService.getCurrentUser].
     */
    suspend fun getCurrentUser(): GrpcUser
}

/**
 * The [UserService] class handles operations related to users by implementing the [IUserService] interface.
 *
 * This class serves as a layer that abstracts the responsibility of user CRUD operations,
 * delegating the actual persistence operations to the [IUserTableRepo] repository.
 *
 * @constructor Initializes the [UserService] with a user repository.
 * @param userRepo The repository responsible for managing persistence operations for users.
 * @param projectRepo The repository responsible for managing persistence operations for projects.
 * @param criterionRepo The repository responsible for managing persistence operations for criteria.
 * @param verificationTokenRepo The repository responsible for managing persistence operations for verification tokens.
 * @param emailManager The manager responsible for sending emails.
 * @param envReader The environment reader that provides access to configuration values.
 * @param accessChecker Interface for checking access permissions for users based on defined rules.
 * @param projectAccessChecker Interface for checking access permissions for projects based on defined rules.
 */
@Suppress("LongParameterList")
class UserService(
    private val userRepo: IUserTableRepo,
    private val projectRepo: IProjectTableRepo,
    private val criterionRepo: ICriterionTableRepo,
    private val verificationTokenRepo: IVerificationTokenTableRepo,
    private val emailManager: IEmailManager,
    private val envReader: EnvReader,
    private val accessChecker: IUserAccessChecker,
    private val projectAccessChecker: IProjectAccessChecker,
) : IUserService {
    companion object {
        private const val VERIFICATION_TOKEN_LENGTH = 48
    }

    override suspend fun getUserById(userId: UUID): GrpcUser = withUser(userRepo) { currentUser ->
        if (currentUser.id == userId) return@withUser currentUser.toGrpcUser()

        val targetUser = userRepo.getUserById(userId).getOrThrow()

        accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)

        targetUser.toGrpcUser()
    }

    override suspend fun getUserByEmail(email: String): GrpcUser = withUser(userRepo) { currentUser ->
        if (currentUser.email == email) return@withUser currentUser.toGrpcUser()

        // We have to request the user first to get the ID for the access checks
        val targetUser = userRepo.getUserByEmail(email).getOrThrow()

        accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.EMAIL)

        targetUser.toGrpcUser()
    }

    override suspend fun getAllUsers(): GrpcUser.List = withUser(userRepo) { currentUser ->
        isServerAdmin()
            .orElseThrow(UnauthorizedReadAllException(currentUser.id, EntityType.USER))
            .checkFor(currentUser)

        userRepo.getAllUsers().toGrpcUsers()
    }

    override suspend fun register(request: Authentication.RegisterRequest) {
        // Check whether a user with the given email already exists
        if (userRepo.doesUserExistByEmail(request.email)) {
            throw DuplicateUserException(request.email)
        }

        // Hash the password and create the user
        val passwordHash = PasswordUtils.hashPassword(request.password)
        val user = userRepo.createUser(request, passwordHash)

        // Generate and save verification token for the user
        val verificationToken = NanoId.generate(VERIFICATION_TOKEN_LENGTH)
        verificationTokenRepo.saveVerificationToken(user.id, verificationToken)

        // Send verification email
        val verificationLink = emailManager.createVerificationLink(verificationToken)
        val expirationTimeInDays = envReader.env.lifetime.verificationTokenLifeTimeInDays
        val data = EmailData.EmailVerification(
            user.firstName,
            verificationLink,
            daysToHumanReadable(expirationTimeInDays),
        )
        emailManager.sendVerificationEmail(user.email, data)
    }

    override suspend fun updateUser(request: GrpcUser.Update): GrpcUser = withUser(userRepo) { currentUser ->
        val targetUserId = parseUUID(request.user.id, EntityType.USER)
        val targetUser = userRepo.getUserById(targetUserId).getOrThrow()

        accessChecker.isAllowedToUpdateUser(currentUser, targetUser)

        // If the role is changed, the requesting user must be a server admin.
        if (request.mask.pathsList.contains("role")) {
            accessChecker.isAllowedToUpdateUserRole(currentUser, targetUserId)
        }

        // If the email is changed, there must not yet exist an account with that email address.
        if (request.mask.pathsList.contains("email") && userRepo.doesUserExistByEmail(request.user.email)) {
            throw DuplicateUserException(request.user.email)
        }

        userRepo.updateUser(request).toGrpcUser()
    }

    override suspend fun softDeleteUser(userId: UUID) = withUser(userRepo) { currentUser ->
        val targetUser = userRepo.getUserById(userId).getOrThrow()

        accessChecker.isAllowedToDeleteUser(currentUser, targetUser)

        // Verify that the user to be deleted is no project admin in any active or archived project anymore.
        val projectsOfTargetUser = projectRepo.getUserProjects(
            targetUser.id,
            setOf(
                ProjectStatus.PROJECT_STATUS_ACTIVE,
                ProjectStatus.PROJECT_STATUS_ACTIVE_LOCKED,
                ProjectStatus.PROJECT_STATUS_ARCHIVED,
            ),
        )
        projectsOfTargetUser.forEach { project ->
            projectAccessChecker.isNotLastProjectAdmin("The user cannot be (soft-)deleted")
                .checkFor(targetUser, project.id)
        }

        userRepo.softDeleteUser(targetUser.id)
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
