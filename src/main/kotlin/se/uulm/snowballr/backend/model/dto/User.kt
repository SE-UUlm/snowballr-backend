package se.uulm.snowballr.backend.model.dto

import se.uulm.snowballr.backend.table.UserTable
import snowballr.UserOuterClass
import snowballr.UserOuterClass.UserRole
import snowballr.UserOuterClass.UserStatus
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
    .setRole(this.role)
    .setStatus(this.status)
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
