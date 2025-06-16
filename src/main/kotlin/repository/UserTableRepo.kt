package se.uulm.snowballr.backend.repository

import org.jetbrains.exposed.sql.selectAll
import se.uulm.snowballr.backend.db.IDatabase
import se.uulm.snowballr.backend.model.SnowballRException.NotFoundException
import se.uulm.snowballr.backend.model.dto.User
import se.uulm.snowballr.backend.table.UserTable
import se.uulm.snowballr.backend.table.UserTable.toUser
import java.util.UUID

/**
 * Defines an interface for repository operations related to the [UserTable].
 *
 * This interface is used to handle persistence and retrieval operations for users by providing
 * abstraction over the underlying database implementation. By using this interface, the logic
 * for creating and managing users can remain decoupled from the specifics of the database layer.
 */
interface IUserTableRepo {
    suspend fun getUserById(id: String): User

    suspend fun getUserByEmail(email: String): User

    suspend fun getAllUsers(): List<User>
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
    override suspend fun getUserById(id: String): User =
        db.dbQuery {
            UserTable
                .selectAll()
                .where { UserTable.id eq UUID.fromString(id) }
                .map { it.toUser() }
                .singleOrNull()
                ?: throw NotFoundException.User(id)
        }

    override suspend fun getUserByEmail(email: String): User =
        db.dbQuery {
            UserTable
                .selectAll()
                .where { UserTable.email eq email }
                .map { it.toUser() }
                .singleOrNull()
                ?: throw NotFoundException.User(email, "email")
        }

    override suspend fun getAllUsers(): List<User> =
        db.dbQuery {
            UserTable
                .selectAll()
                .map { it.toUser() }
        }
}
