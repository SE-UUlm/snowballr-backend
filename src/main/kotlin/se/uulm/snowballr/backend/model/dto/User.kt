package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import se.uulm.snowballr.backend.table.UserTable
import snowballr.UserOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * DTO of [UserTable].
 */
data class User(
    val id: UUID,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val status: UserStatus,
    val createdAt: OffsetDateTime,
    val modifiedAt: OffsetDateTime?,
    val deletedAt: OffsetDateTime?,
)

/**
 * Creates a [UserOuterClass.User] from this [User].
 */
fun User.toGrpcUser(): UserOuterClass.User = UserOuterClass.User
    .newBuilder()
    .setId(this.id.toString())
    .setEmail(this.email)
    .setFirstName(this.firstName)
    .setLastName(this.lastName)
    .setRole(this.role.toGrpc())
    .setStatus(this.status.toGrpc())
    .build()

/**
 * Creates a list of [UserOuterClass.User]s from this list of [User]s.
 */
fun List<User>.toGrpcUsers(): UserOuterClass.User.List {
    val builder = UserOuterClass.User.List.newBuilder()
    this.forEach { builder.addUsers(it.toGrpcUser()) }
    return builder.build()
}

/**
 * Checks whether the user is a server admin.
 *
 * A user is considered a server admin if their role is set to [UserRole.USER_ROLE_ADMIN].
 */
fun User.isServerAdmin() = this.role == UserRole.USER_ROLE_ADMIN

/**
 * Checks whether the user is active and confirmed.
 *
 * A user is considered active and confirmed if their status is set to [UserStatus.USER_STATUS_ACTIVE].
 * The status [UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED] does count as active, but not as confirmed.
 */
fun User.isActiveAndConfirmed() = this.status == UserStatus.USER_STATUS_ACTIVE

/**
 * Checks whether the user is active.
 *
 * A user is considered active if their status is either [UserStatus.USER_STATUS_ACTIVE] or
 * [UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED].
 */
fun User.isActive() = this.isActiveAndConfirmed() || this.status == UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED

/**
 * Returns the full name of the user by concatenating the first name and last name.
 *
 * If either the first name or last name is empty, it will return the non-empty part as the full name.
 * If both are empty, it will return an empty string.
 *
 * Example:
 * - If firstName is "John" and lastName is "Doe", it will return "John Doe".
 * - If firstName is "John" and lastName is "", it will return "John".
 * - If firstName is "" and lastName is "Doe", it will return "Doe".
 * - If both firstName and lastName are "", it will return "".
 */
fun User.getFullName(): String = "${this.firstName} ${this.lastName}".trim()
