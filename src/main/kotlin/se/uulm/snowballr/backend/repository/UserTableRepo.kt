package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.TextColumnType
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.statements.StatementType
import org.jetbrains.exposed.sql.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.dto.UserSettings
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.toUser
import se.uulm.snowballr.backend.table.toUserSettings
import snowballr.Authentication
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.sql.ResultSet
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [UserTable].
 *
 * This interface is used to handle persistence and retrieval operations for users by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing users can remain decoupled from the specifics of the database layer.
 */
@Suppress("ComplexInterface")
interface IUserTableRepo {
    /**
     * Returns a [Result] containing the user by its id or a [NotFoundException] if the user with the passed [id]
     * doesn't exist.
     */
    suspend fun getUserById(id: UUID): Result<User>

    /**
     * Returns a [Result] containing the user by its email or a [NotFoundException] if the user with the passed [email]
     * doesn't exist.
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
     * @param excludedUsers A list of user ids to be excluded from the results.
     * @return A list of up to 10 matching users.
     */
    suspend fun getUsersMatchingSearchQuery(searchQuery: String, excludedUsers: Set<UUID>): List<User>

    /**
     * Creates a new user in the database with the provided registration request and password hash.
     *
     * @param request The registration request containing user details such as email, first name, and last name.
     * @param passwordHash The hashed password for the user.
     * @return The created [User] object representing the newly registered user.
     */
    suspend fun createUser(request: Authentication.RegisterRequest, passwordHash: String): User

    /**
     * Updates an existing user in the database with the provided new information.
     * The following fields can be updated:
     * - first name
     * - last name
     * - email
     * - role
     *
     * @param request The update request containing the new user details, such as the new first name.
     * @return The updated [User] object reflecting the changes from the [request].
     */
    suspend fun updateUser(request: UserOuterClass.User.Update): User

    /**
     * Performs a soft delete meaning the user with the given [id] is not removed from the database, but only the
     * status is set to [UserOuterClass.UserStatus.USER_STATUS_DELETED].
     */
    suspend fun softDeleteUser(id: UUID)

    /**
     * Retrieves the password hash for a user by their email address.
     *
     * @param email The email address of the user whose password hash is to be retrieved.
     * @return The password hash as a [String] for the user with the specified email.
     */
    suspend fun getPasswordHashByEmail(email: String): Result<String>

    /**
     * Retrieves the user settings associated with a specific user ID.
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
        const val MAXIMUM_NUMBER_OF_INVITE_CANDIDATES = 10
    }

    private fun extractUserRows(result: ResultSet): List<User> {
        return generateSequence {
            if (result.next()) {
                ResultRow.create(
                    result,
                    UserTable.fields.withIndex().associate { it.value to it.index },
                )
            } else {
                null
            }
        }.map { it.toUser() }.toList()
    }

    /**
     * Requesting a user from the database.
     *
     * @param id The id of the requested user.
     * @return The [User] object or null, if no user with the given [id] was found.
     */
    private fun getUserByIdOrNull(id: UUID): User? = UserTable.getEntityByIdOrNull(id, ResultRow::toUser)

    override suspend fun getUserById(id: UUID): Result<User> = db.query {
        getEntityByIdAsResult(::getUserByIdOrNull, EntityType.USER, id)
    }

    override suspend fun getUserByEmail(email: String): Result<User> = db.query {
        val user = UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .map { it.toUser() }
            .singleOrNull()

        if (user != null) {
            Result.success(user)
        } else {
            Result.failure(NotFoundException(EntityType.USER, email, identifierType = IdentifierType.EMAIL))
        }
    }

    override suspend fun doesUserExistByEmail(email: String): Boolean = db.query {
        UserTable.doesEntityExist { UserTable.email eq email }
    }

    override suspend fun getAllUsers(): List<User> = db.query {
        UserTable
            .selectAll()
            .where(UserTable.email neq "")
            .map { it.toUser() }
    }

    @Suppress("MagicNumber")
    override suspend fun getUsersMatchingSearchQuery(searchQuery: String, excludedUsers: Set<UUID>): List<User> =
        db.query {
            val userTable = "\"${UserTable.tableName}\""
            val idCol = "$userTable.${UserTable.id.name}"
            val firstNameCol = "$userTable.${UserTable.firstName.name}"
            val lastNameCol = "$userTable.${UserTable.lastName.name}"
            val emailCol = "$userTable.${UserTable.email.name}"
            val statusCol = "$userTable.${UserTable.status.name}"

            val excludeUsersClause = if (excludedUsers.isNotEmpty()) {
                "AND $idCol NOT IN (${excludedUsers.joinToString(",") { "'$it'" }})"
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
                    WHERE $statusCol IN (${UserStatus.USER_STATUS_ACTIVE.ordinal}, ${UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED.ordinal})
                      $excludeUsersClause
                )
                SELECT *
                FROM users_with_similarity_scores
                WHERE GREATEST(sim_first_name, sim_last_name, sim_email) > 0.2
                ORDER BY GREATEST(sim_first_name, sim_last_name, sim_email) DESC
                LIMIT $MAXIMUM_NUMBER_OF_INVITE_CANDIDATES
                """.trimIndent()

            val matchingUsers = exec(
                stmt = rawSqlQuery,
                args = List(3) { TextColumnType() to searchQuery },
                explicitStatementType = StatementType.SELECT,
                transform = ::extractUserRows,
            )

            matchingUsers.orEmpty()
        }

    override suspend fun createUser(request: Authentication.RegisterRequest, passwordHash: String): User = db.query {
        UserTable.insertAndGet(ResultRow::toUser, EntityType.USER) {
            it[email] = request.email
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[UserTable.passwordHash] = passwordHash
            it[role] = UserRole.USER_ROLE_DEFAULT
            it[status] = UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        }
    }

    override suspend fun updateUser(request: UserOuterClass.User.Update): User = db.query {
        val userId = parseUUID(request.user.id, EntityType.USER)
        val fieldMask = FieldMaskUtil.normalize(request.mask)

        UserTable.updateByIdAndGet(userId, ResultRow::toUser, EntityType.USER) {
            for (field in fieldMask.pathsList) {
                when (field) {
                    "user.email" -> it[email] = request.user.email
                    "user.first_name" -> it[firstName] = request.user.firstName
                    "user.last_name" -> it[lastName] = request.user.lastName
                    "user.role" -> it[role] = request.user.role
                    "user.status" -> it[status] = request.user.status
                }
            }

            it[modifiedAt] = OffsetDateTime.now()
        }
    }

    override suspend fun softDeleteUser(id: UUID) {
        db.query {
            UserTable.update({ UserTable.id eq id }) {
                it[status] = UserStatus.USER_STATUS_DELETED
                it[deletedAt] = OffsetDateTime.now()
            }
        }
    }

    override suspend fun getPasswordHashByEmail(email: String): Result<String> = db.query {
        val passwordHash = UserTable
            .select(UserTable.passwordHash)
            .where { UserTable.email eq email }
            .map { it[UserTable.passwordHash] }
            .singleOrNull()

        if (passwordHash != null) {
            Result.success(passwordHash)
        } else {
            Result.failure(NotFoundException(EntityType.USER, email, identifierType = IdentifierType.EMAIL))
        }
    }

    override suspend fun getUserSettings(id: UUID): Result<UserSettings> = db.query {
        val settings = UserTable.selectAll()
            .where { UserTable.id eq id }
            .map { it.toUserSettings() }
            .singleOrNull()

        if (settings != null) {
            Result.success(settings)
        } else {
            Result.failure(NotFoundException(EntityType.USER, id.toString(), identifierType = IdentifierType.ID))
        }
    }
}
