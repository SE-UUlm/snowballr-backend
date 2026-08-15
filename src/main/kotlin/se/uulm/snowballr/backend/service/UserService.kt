package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import io.viascom.nanoid.NanoId
import se.uulm.snowballr.backend.access.IProjectAccessChecker
import se.uulm.snowballr.backend.access.IUserAccessChecker
import se.uulm.snowballr.backend.auth.PasswordUtils
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.formatting.daysToHumanReadable
import se.uulm.snowballr.backend.grpc.SnowballRServer.SnowballRService
import se.uulm.snowballr.backend.mail.IEmailManager
import se.uulm.snowballr.backend.model.UserIdentifierType
import se.uulm.snowballr.backend.model.dto.project.ProjectStatus
import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserSettingsWithCriteria
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.exception.alreadyexists.entity.DuplicateUserException
import se.uulm.snowballr.backend.model.incoming.user.RegisterRequest
import se.uulm.snowballr.backend.model.incoming.user.UpdateUserRequest
import se.uulm.snowballr.backend.repository.ICriterionTableRepo
import se.uulm.snowballr.backend.repository.IProjectTableRepo
import se.uulm.snowballr.backend.repository.IUserTableRepo
import se.uulm.snowballr.backend.repository.IVerificationTokenTableRepo
import java.util.UUID

private val logger = KotlinLogging.logger {}

interface IUserService {
    /**
     * Service implementation of [SnowballRService.getUserById].
     */
    suspend fun getUserById(userId: UUID): User

    /**
     * Service implementation of [SnowballRService.getUserByEmail].
     */
    suspend fun getUserByEmail(email: String): User

    /**
     * Service implementation of [SnowballRService.getAllUsers].
     */
    suspend fun getAllUsers(): List<User>

    /**
     * Service implementation of [SnowballRService.register].
     */
    suspend fun register(request: RegisterRequest)

    /**
     * Service implementation of [SnowballRService.updateUser].
     */
    suspend fun updateUser(request: UpdateUserRequest, paths: List<String>): User

    /**
     * Service implementation of [SnowballRService.softDeleteUser].
     */
    suspend fun softDeleteUser(userId: UUID)

    /**
     * Service implementation of [SnowballRService.getUserSettings].
     */
    suspend fun getUserSettings(): UserSettingsWithCriteria

    /**
     * Service implementation of [SnowballRService.getCurrentUser].
     */
    suspend fun getCurrentUser(): User
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

    override suspend fun getUserById(userId: UUID): User = withUser(userRepo) { currentUser ->
        if (currentUser.id == userId) return@withUser currentUser

        val targetUser = userRepo.getUserById(userId).getOrThrow()

        accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.ID)

        targetUser
    }

    override suspend fun getUserByEmail(email: String): User = withUser(userRepo) { currentUser ->
        if (currentUser.email == email) return@withUser currentUser

        // We have to request the user first to get the ID for the access checks
        val targetUser = userRepo.getUserByEmail(email).getOrThrow()

        accessChecker.isAllowedToReadUser(currentUser, targetUser, UserIdentifierType.EMAIL)

        targetUser
    }

    override suspend fun getAllUsers(): List<User> = withUser(userRepo) { currentUser ->
        accessChecker.isAllowedToReadAllUsers(currentUser)

        userRepo.getAllUsers()
    }

    override suspend fun register(request: RegisterRequest) {
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
        logger.info { "User ${user.id} registered (${user.email})" }
    }

    override suspend fun updateUser(request: UpdateUserRequest, paths: List<String>): User =
        withUser(userRepo) { currentUser ->
            val targetUser = userRepo.getUserById(request.userId).getOrThrow()

            accessChecker.isAllowedToUpdateUser(currentUser, targetUser)

            // If the role is changed, the requesting user must be a server admin.
            if (paths.contains("user.role")) {
                accessChecker.isAllowedToUpdateUserRole(currentUser, request.userId)
            }

            // If the email is changed, there must not yet exist an account with that email address.
            if (paths.contains("user.email") && userRepo.doesUserExistByEmail(request.email)) {
                throw DuplicateUserException(request.email)
            }

            val updatedUser = userRepo.updateUser(request, paths)
            logger.info { "User ${targetUser.id} updated: ${paths.joinToString()}" }
            updatedUser
        }

    override suspend fun softDeleteUser(userId: UUID) = withUser(userRepo) { currentUser ->
        val targetUser = userRepo.getUserById(userId).getOrThrow()

        accessChecker.isAllowedToDeleteUser(currentUser, targetUser)

        // Verify that the user to be deleted is no project admin in any active or archived project anymore.
        val projectsOfTargetUser = projectRepo.getUserProjects(
            targetUser.id,
            setOf(
                ProjectStatus.ACTIVE,
                ProjectStatus.ACTIVE_LOCKED,
                ProjectStatus.ARCHIVED,
            ),
        )
        projectsOfTargetUser.forEach { project ->
            projectAccessChecker.isNotLastProjectAdmin(targetUser, project.id, "The user cannot be (soft-)deleted")
        }

        userRepo.softDeleteUser(targetUser.id)
        logger.info { "User ${targetUser.id} soft-deleted" }
    }

    override suspend fun getCurrentUser(): User = withUser(userRepo) { it }

    override suspend fun getUserSettings(): UserSettingsWithCriteria = withUser(userRepo) { currentUser ->
        val userSettings = userRepo.getUserSettings(currentUser.id).getOrThrow()
        val defaultUserCriteria = criterionRepo.getCriteriaByIds(userSettings.criteriaIds)

        UserSettingsWithCriteria(userSettings, defaultUserCriteria)
    }
}
