package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.isNotNull
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.core.neq
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.statements.jdbc.JdbcResult
import org.jetbrains.exposed.v1.jdbc.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.UserSettings
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.model.exception.NotFoundException
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.toUser
import se.uulm.snowballr.backend.table.toUserSettings
import snowballr.Authentication
import java.time.OffsetDateTime
import java.util.UUID
import snowballr.UserOuterClass.User as GrpcUser

private val logger = KotlinLogging.logger { }

/**
 * Defines an interface for repository operations related to the [UserTable].
 *
 * This interface is used to handle persistence and retrieval operations for users by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing users can remain decoupled from the specifics of the database layer.
 */
@Suppress("ComplexInterface", "TooManyFunctions")
interface IUserTableRepo {
    /**
     * Returns a [Result] containing the user by their id or a [NotFoundException] if the user with the passed [id]
     * doesn't exist.
     */
    suspend fun getUserById(id: UUID): Result<User>

    /**
     * Returns a [Result] containing the user by their email or a [NotFoundException] if the user with the passed
     * [email] doesn't exist.
     */
    suspend fun getUserByEmail(email: String): Result<User>

    /**
     * Checks if a user exists in the database by their email address.
     *
     * @param email The email address to check for existence.
     * @return True if a user with the given email exists, false otherwise.
     */
    suspend fun doesUserExistByEmail(email: String): Boolean

    /**
     * Checks if a user exists in the database by their id.
     *
     * @param id The unique identifier to check for existence.
     * @return True if a user with the given id exists, false otherwise.
     */
    suspend fun doesUserExistById(id: UUID): Boolean

    /**
     * Returns all users stored on the server.
     */
    suspend fun getAllUsers(): List<User>

    /**
     * Retrieves a list of users whose name or email partially or fully match the [searchQuery].
     *
     * Only users who are active (not deleted or marked for deletion) and not present in the list of [excludedUsers]
     * are included in the results. The number of returned users is limited to a maximum of 10.
     * The search string is matched against the firstname, lastname, and email address of the user using
     * the `similarity()` function from the `pg_trgm` extension. Furthermore, the matching users are sorted by
     * their similarity to the search query.
     *
     * @param searchQuery The query against which the firstnames, lastnames, and emails of the users are checked.
     * @param excludedUsers A list of user emails to be excluded from the results.
     * @return A list of up to 10 matching users.
     */
    suspend fun getUsersMatchingSearchQuery(searchQuery: String, excludedUsers: Set<String>): List<User>

    /**
     * Creates a new user in the database with the provided registration request and password hash.
     *
     * @param request The registration request containing user details such as email, first name, and last name.
     * @param passwordHash The hashed password for the user.
     * @return The created [User] object representing the newly registered user.
     */
    suspend fun createUser(request: Authentication.RegisterRequest, passwordHash: String): User

    /**
     * Updates an existent user in the database with the provided new information.
     * The following fields can be updated:
     * - first name
     * - last name
     * - email
     * - role
     *
     * @param request The update request containing the new user details, such as the new first name.
     * @return The updated [User] object reflecting the changes from the [request].
     */
    suspend fun updateUser(request: GrpcUser.Update): User

    /**
     * Performs a soft-delete meaning the user with the given [id] is not removed from the database, but only the
     * status is set to [UserStatus.DELETED].
     */
    suspend fun softDeleteUser(id: UUID)

    /**
     * Clears all soft-deleted users whose deletion date is older than the given [thresholdDate].
     *
     * @param thresholdDate The date up to which soft-deleted users are to be cleared.
     */
    suspend fun clearSoftDeletedUsers(thresholdDate: OffsetDateTime)

    /**
     * Retrieves a list of user IDs that are eligible for hard deletion.
     *
     * @return A list of user IDs that are eligible for hard deletion.
     */
    suspend fun getUserIdsToDelete(): List<UUID>

    /**
     * Tries to hard-delete the users in the given [userIdsToDelete] list.
     *
     * @param userIdsToDelete The list of user IDs to be hard-deleted.
     */
    suspend fun hardDeleteClearedUsers(userIdsToDelete: List<UUID>)

    /**
     * Returns a [Result] containing the password hash for a user by their email address or a [NotFoundException] if the
     * user with the passed [email] doesn't exist.
     *
     * @param email The email address of the user whose password hash is to be retrieved.
     * @return The password hash as a [String] for the user with the specified email.
     */
    suspend fun getPasswordHashByEmail(email: String): Result<String>

    /**
     * Updates the password hash of the user with the given [userId].
     */
    suspend fun updatePasswordHash(userId: UUID, passwordHash: String)

    /**
     * Returns a [Result] containing the settings of the user with the passed [id] or a [NotFoundException] if the user
     * with the passed [id] doesn't exist.
     *
     * @param id The unique identifier of the user whose settings are to be fetched.
     * @return The [UserSettings] object containing the settings for the specified user.
     */
    suspend fun getUserSettings(id: UUID): Result<UserSettings>
}

/**
 * Repository implementation for managing the [UserTable] in the database.
 *
 * This class handles the persistence and retrieval of user data by integrating
 * with the underlying database through the [IDatabase] interface. It provides
 * concrete methods for CRUD operations on user records within the database.
 *
 * @param db The database abstraction used for executing queries within a transaction.
 */
@Suppress("TooManyFunctions")
class UserTableRepo(
    private val db: IDatabase,
) : IUserTableRepo {
    companion object {
        private const val MAXIMUM_NUMBER_OF_INVITE_CANDIDATES = 10
        private const val MINIMUM_SIMILARITY_SCORE = 0.2
    }

    private fun getUserByIdOrNull(id: UUID): User? = UserTable.getEntityByIdOrNull(id, ResultRow::toUser)

    private fun getUserByEmailOrNull(email: String): User? =
        UserTable.getEntityOrNull(ResultRow::toUser) { UserTable.email eq email }

    private fun getPasswordHashByUserMailOrNull(email: String): String? = UserTable.select(UserTable.passwordHash)
        .where { UserTable.email eq email }
        .map { it[UserTable.passwordHash] }
        .singleOrNull()

    private fun getUserSettingsByUserIdOrNull(userId: UUID): UserSettings? =
        UserTable.getEntityByIdOrNull(userId, ResultRow::toUserSettings)

    /**
     * Retrieves a list of user IDs that are eligible for clearing sensitive data.
     *
     * @param thresholdDate The date up to which users are to be cleared.
     * @return A list of user IDs that are eligible for clearing sensitive data.
     */
    private suspend fun getUserIdsToClear(thresholdDate: OffsetDateTime): List<UUID> = db.query {
        UserTable.selectAll()
            .where {
                (UserTable.status eq UserStatus.DELETED).and(UserTable.deletedAt lessEq thresholdDate)
            }
            .map { it[UserTable.id].value }
    }

    /**
     * Attempts to delete a single user by their ID.
     *
     * @param userId The ID of the user to be deleted.
     * @return `true` if the user was successfully deleted, `false` otherwise.
     */
    private suspend fun attemptToDeleteUser(userId: UUID): Boolean = db.query {
        try {
            val deletedRows = UserTable.deleteWhere { UserTable.id eq userId }
            deletedRows > 0
        } catch (e: ExposedSQLException) {
            logger.debug(e) { "Failed to hard-delete user $userId, likely due to existing references." }
            false
        }
    }

    override suspend fun getUserById(id: UUID): Result<User> = db.query {
        getEntityByKeyAsResult(::getUserByIdOrNull, EntityType.USER, id)
    }

    override suspend fun getUserByEmail(email: String): Result<User> = db.query {
        getEntityByKeyAsResult(::getUserByEmailOrNull, EntityType.USER, email, IdentifierType.EMAIL)
    }

    override suspend fun doesUserExistById(id: UUID): Boolean = db.query {
        UserTable.doesEntityExistById(id)
    }

    override suspend fun doesUserExistByEmail(email: String): Boolean = db.query {
        UserTable.doesEntityExist { UserTable.email eq email }
    }

    override suspend fun getAllUsers(): List<User> = db.query {
        UserTable.getEntities(ResultRow::toUser) { UserTable.email neq "" }
    }

    @Suppress("MagicNumber")
    override suspend fun getUsersMatchingSearchQuery(searchQuery: String, excludedUsers: Set<String>): List<User> =
        db.query {
            val userTable = "\"${UserTable.tableName}\""
            val firstNameCol = "$userTable.${UserTable.firstName.name}"
            val lastNameCol = "$userTable.${UserTable.lastName.name}"
            val emailCol = "$userTable.${UserTable.email.name}"
            val statusCol = "$userTable.${UserTable.status.name}"

            val excludeUsersClause = if (excludedUsers.isNotEmpty()) {
                "AND $emailCol NOT IN (${excludedUsers.joinToString(",") { "'$it'" }})"
            } else {
                ""
            }

            val rawSqlQuery =
                """
                WITH users_with_similarity_scores AS (
                    SELECT *,
                        similarity($firstNameCol, ?) AS sim_first_name,
                        similarity($lastNameCol, ?) AS sim_last_name,
                        similarity($emailCol, ?) AS sim_email
                    FROM $userTable
                    WHERE $statusCol IN (${UserStatus.ACTIVE.ordinal}, ${UserStatus.ACTIVE_UNCONFIRMED.ordinal})
                      $excludeUsersClause
                )
                SELECT *
                FROM users_with_similarity_scores
                WHERE GREATEST(sim_first_name, sim_last_name, sim_email) > $MINIMUM_SIMILARITY_SCORE
                ORDER BY GREATEST(sim_first_name, sim_last_name, sim_email) DESC
                LIMIT $MAXIMUM_NUMBER_OF_INVITE_CANDIDATES
                """.trimIndent()

            val matchingUsers = exec(
                stmt = rawSqlQuery,
                args = List(3) { TextColumnType() to searchQuery },
                explicitStatementType = StatementType.SELECT,
                transform = { extractUserRows(JdbcResult(it)) },
            )

            matchingUsers.orEmpty()
        }

    override suspend fun createUser(request: Authentication.RegisterRequest, passwordHash: String): User = db.query {
        UserTable.insertAndGet(ResultRow::toUser) {
            it[email] = request.email
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[UserTable.passwordHash] = passwordHash
            it[role] = UserRole.DEFAULT
            it[status] = UserStatus.ACTIVE_UNCONFIRMED
        }
    }

    override suspend fun updateUser(request: GrpcUser.Update): User = db.query {
        val userId = parseUUID(request.user.id, EntityType.USER)
        val fieldMask = FieldMaskUtil.normalize(request.mask)

        UserTable.updateByIdAndGet(userId, ResultRow::toUser) {
            for (field in fieldMask.pathsList) {
                when (field) {
                    "user.email" -> it[email] = request.user.email
                    "user.first_name" -> it[firstName] = request.user.firstName
                    "user.last_name" -> it[lastName] = request.user.lastName
                    "user.role" -> it[role] = UserRole.fromGrpc(request.user.role)
                    "user.status" -> it[status] = UserStatus.fromGrpc(request.user.status)
                }
            }

            it[modifiedAt] = OffsetDateTime.now()
        }
    }

    override suspend fun softDeleteUser(id: UUID) {
        db.query {
            UserTable.update({ UserTable.id eq id }) {
                it[status] = UserStatus.DELETED
                it[deletedAt] = OffsetDateTime.now()
            }
        }
    }

    override suspend fun clearSoftDeletedUsers(thresholdDate: OffsetDateTime) = db.query {
        val usersToBeCleared = getUserIdsToClear(thresholdDate)

        val clearedUsers = UserTable.update(
            {
                UserTable.id inList usersToBeCleared
            },
        ) {
            it[email] = ""
            it[firstName] = ""
            it[lastName] = ""
            it[passwordHash] = ""
            it[role] = UserRole.DEFAULT
            it[status] = UserStatus.CLEARED
            it[criteriaIds] = emptyList()
            it[fetchers] = emptyMap()
            it[modifiedAt] = OffsetDateTime.now()
        }

        logger.info { "Cleared $clearedUsers soft-deleted users older than $thresholdDate." }
    }

    override suspend fun getUserIdsToDelete(): List<UUID> = db.query {
        UserTable
            .selectAll()
            .where {
                (UserTable.status eq UserStatus.CLEARED).and(UserTable.deletedAt.isNotNull())
            }
            .map { it[UserTable.id].value }
    }

    override suspend fun hardDeleteClearedUsers(userIdsToDelete: List<UUID>) {
        if (userIdsToDelete.isEmpty()) {
            logger.info { "No users to hard-delete." }
            return
        }

        val (successfulDeletedIds, failedToDeleteIds) = userIdsToDelete.partition { userId ->
            attemptToDeleteUser(userId)
        }

        logger.info {
            "Hard-deleted ${successfulDeletedIds.size} users, failed to delete ${failedToDeleteIds.size} users."
        }
    }

    override suspend fun getPasswordHashByEmail(email: String): Result<String> = db.query {
        getEntityByKeyAsResult(::getPasswordHashByUserMailOrNull, EntityType.USER, email, IdentifierType.EMAIL)
    }

    override suspend fun updatePasswordHash(userId: UUID, passwordHash: String) {
        db.query {
            UserTable.update({ UserTable.id eq userId }) {
                it[UserTable.passwordHash] = passwordHash
                it[modifiedAt] = OffsetDateTime.now()
            }
        }
    }

    override suspend fun getUserSettings(id: UUID): Result<UserSettings> = db.query {
        getEntityByKeyAsResult(::getUserSettingsByUserIdOrNull, EntityType.USER, id)
    }

    /**
     * Extracts and converts rows from a [JdbcResult] to a list of [User] objects.
     *
     * @param result The [JdbcResult] containing user data.
     * @return A list of [User] objects extracted from the result set.
     */
    private fun extractUserRows(result: JdbcResult): List<User> = extractTableRows(result, UserTable, ResultRow::toUser)
}
