package se.uulm.snowballr.backend.repository

import com.google.protobuf.util.FieldMaskUtil
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.like
import org.jetbrains.exposed.sql.SqlExpressionBuilder.neq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.or
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.EntityType
import se.uulm.snowballr.backend.model.IdentifierType
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.model.parseUUID
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.toUser
import snowballr.Authentication
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
import java.time.OffsetDateTime
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [UserTable].
 *
 * This interface is used to handle persistence and retrieval operations for users by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing users can remain decoupled from the specifics of the database layer.
 */
interface IUserTableRepo {
    /**
     * Returns a user by its id or throws a [NotFoundException] if the user with the passed [id] doesn't exist.
     */
    suspend fun getUserById(id: UUID): User

    /**
     * Returns a user by its email or throws a [NotFoundException] if the user with the passed [email] doesn't
     * exist.
     */
    suspend fun getUserByEmail(email: String): User

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
    suspend fun getPasswordHashByEmail(email: String): String
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
class UserTableRepo(
    private val db: IDatabase,
) : IUserTableRepo {
    companion object {
        const val MAXIMUM_NUMBER_OF_INVITE_CANDIDATES = 10
    }

    /**
     * Requesting a user from the database.
     *
     * @param id The id of the requested user.
     * @return The [User] object or null, if no user with the given [id] was found.
     */
    private fun getUserByIdOrNull(id: UUID): User? = UserTable.getEntityByIdOrNull(id, ResultRow::toUser)

    override suspend fun getUserById(id: UUID): User = db.query {
        getUserByIdOrNull(id) ?: throw NotFoundException(EntityType.USER, id.toString())
    }

    override suspend fun getUserByEmail(email: String): User = db.query {
        UserTable
            .selectAll()
            .where { UserTable.email eq email }
            .map { it.toUser() }
            .singleOrNull()
            ?: throw NotFoundException(EntityType.USER, email, identifierType = IdentifierType.EMAIL)
    }

    override suspend fun doesUserExistByEmail(email: String): Boolean = db.query {
        UserTable
            .select(UserTable.email)
            .where { UserTable.email eq email }
            .empty()
            .not()
    }

    override suspend fun getAllUsers(): List<User> = db.query {
        UserTable
            .selectAll()
            .where(UserTable.email neq "")
            .map { it.toUser() }
    }

    override suspend fun getUsersMatchingSearchQuery(searchQuery: String, excludedUsers: Set<UUID>): List<User> =
        db.dbQuery {
            val query = "%$searchQuery%"

            UserTable
                .selectAll()
                .where {
                    (UserTable.firstName like query) or (UserTable.lastName like query) or (UserTable.email like query)
                }
                .andWhere {
                    (UserTable.status eq UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED) or
                        (UserTable.status eq UserStatus.USER_STATUS_ACTIVE)
                }
                .andWhere { UserTable.id notInList excludedUsers.map { it } }
                .limit(MAXIMUM_NUMBER_OF_INVITE_CANDIDATES)
                .map { it.toUser() }
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

    override suspend fun getPasswordHashByEmail(email: String): String = db.query {
        UserTable
            .select(UserTable.passwordHash)
            .where { UserTable.email eq email }
            .map { it[UserTable.passwordHash] }
            .singleOrNull()
            ?: throw NotFoundException(EntityType.USER, email, identifierType = IdentifierType.EMAIL)
    }
}
