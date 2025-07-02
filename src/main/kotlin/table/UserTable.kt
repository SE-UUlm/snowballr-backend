package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import se.uulm.snowballr.backend.model.dto.User
import snowballr.UserOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "user" and provides a mapping for managing user-related entities in the database.
 *
 * Columns:
 * - [email]: Represents the email address of the user as a [String].
 * - [firstName]: Represents the first name of the user as a [String].
 * - [lastName]: Represents the last name of the user as a [String].
 * - [passwordHash]: Represents the hashed password of the user as a [String].
 * - [role]: Represents the role of the user as an enumeration value from [UserOuterClass.UserRole].
 * - [status]: Represents the status of the user as an enumeration value from [UserOuterClass.UserStatus].
 * - [createdAt]: Represents the timestamp of when the user was created as an [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the user was last modified as an [OffsetDateTime].
 * - [deletedAt]: Represents the timestamp of when the user was last deleted as an [OffsetDateTime].
 */
object UserTable : UUIDTable("user") {
    val email = text("email").uniqueIndex()
    val firstName = text("first_name")
    val lastName = text("last_name")
    val passwordHash = obfuscatedText("password_hash")
    val role = enumeration<UserOuterClass.UserRole>("role")
    val status = enumeration<UserOuterClass.UserStatus>("status")

    // Metadata

    val createdAt = createdAt()
    val modifiedAt = modifiedAt()
    val deletedAt = deletedAt()

    // Methods

    /**
     * Creates a [User] from this [ResultRow].
     */
    fun ResultRow.toUser() = User(
        id = this[id].value,
        email = this[email],
        firstName = this[firstName],
        lastName = this[lastName],
        role = this[role],
        status = this[status],
        createdAt = this[createdAt],
        modifiedAt = this[modifiedAt],
        deletedAt = this[deletedAt],
    )
}
