package se.uulm.snowballr.backend.auth

import se.uulm.snowballr.backend.model.dto.User
import snowballr.UserOuterClass
import java.time.OffsetDateTime
import java.util.UUID

/**
 * A dummy user object that is used for testing purposes.
 *
 * This object contains hardcoded values for a user that can be used in tests or development
 * where a valid user is required.
 *
 * It is not intended for production use and should be replaced with actual user data in real applications.
 */
object DummyUser {
    var id: UUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    var email: String = "alice.smith@example.com"
    var firstName: String = "Alice"
    var lastName: String = "Smith"
    var password: String = "VALIDPassword__1234"
    var passwordHash: String = PasswordUtils.hashPassword(password)
    var role: UserOuterClass.UserRole = UserOuterClass.UserRole.USER_ROLE_ADMIN
    var status: UserOuterClass.UserStatus = UserOuterClass.UserStatus.USER_STATUS_ACTIVE

    /**
     * Converts this dummy user to a gRPC User object.
     *
     * @return A [UserOuterClass.User] instance representing this dummy user.
     */
    fun toUser(): User {
        return User(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            role = role,
            status = status,
            createdAt = OffsetDateTime.now(),
            modifiedAt = null,
            deletedAt = null,
        )
    }

    fun reset() {
        id = UUID.fromString("00000000-0000-0000-0000-000000000000")
        email = "alice.smith@example.com"
        firstName = "Alice"
        lastName = "Smith"
        password = "VALIDPassword__1234"
        passwordHash = PasswordUtils.hashPassword(password)
        role = UserOuterClass.UserRole.USER_ROLE_DEFAULT
        status = UserOuterClass.UserStatus.USER_STATUS_ACTIVE
    }
}
