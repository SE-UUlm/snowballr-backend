package se.uulm.snowballr.backend.model.incoming.user

import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import java.util.UUID

data class UpdateUserRequest(
    val userId: UUID,
    val firstName: String,
    val lastName: String,
    val email: String,
    val role: UserRole,
    val status: UserStatus,
) {
    companion object {
        fun fromUser(user: User) = UpdateUserRequest(
            userId = user.id,
            firstName = user.firstName,
            lastName = user.lastName,
            email = user.email,
            role = user.role,
            status = user.status,
        )
    }
}
