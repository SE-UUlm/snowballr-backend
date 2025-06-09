package se.uulm.snowballr.backend.table

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import snowballr.UserOuterClass
import java.time.OffsetDateTime

/**
 * Represents the database table "user" and provides a mapping for managing user-related entities in the database.
 *
 * Columns:
 * - [email]: Represents the email address of the user as a text.
 * - [firstName]: Represents the first name of the user as a text.
 * - [lastName]: Represents the last name of the user as a text.
 * - [password]: Represents the password of the user as a text.
 * - [accessToken]: Represents the access token of the user as a text.
 * - [refreshToken]: Represents the refresh token of the user as a text.
 * - [role]: Represents the role of the user as an enumeration value from [UserOuterClass.UserRole].
 * - [status]: Represents the status of the user as an enumeration value from [UserOuterClass.UserStatus].
 * - [createdAt]: Represents the timestamp of when the user was created as a [OffsetDateTime].
 * - [modifiedAt]: Represents the timestamp of when the user was last modified as a [OffsetDateTime].
 * - [deletedAt]: Represents the timestamp of when the user was last deleted as a [OffsetDateTime].
 */
object UserTable : UUIDTable("user") {
    val email = text("email").uniqueIndex()
    val firstName = text("first_name")
    val lastName = text("last_name")

//    val password = use exposed-crypt
//    val accessToken = foo
//    val refreshToken = bar
    val role = enumeration("role", UserOuterClass.UserRole::class)
    val status = enumeration("status", UserOuterClass.UserStatus::class)
    val createdAt = timestampWithTimeZone("created_at").clientDefault { OffsetDateTime.now() }
    val modifiedAt = timestampWithTimeZone("modified_at").nullable()
    val deletedAt = timestampWithTimeZone("deleted_at").nullable()

    @Suppress("unused")
    fun ResultRow.toUser(): UserOuterClass.User =
        UserOuterClass.User
            .newBuilder()
            .setId(this[id].value.toString())
            .setEmail(this[email])
            .setFirstName(this[firstName])
            .setLastName(this[lastName])
            .setRole(this[role])
            .setStatus(this[status])
            .build()
}
