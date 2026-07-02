package se.uulm.snowballr.backend.model.outgoing.invitation

import se.uulm.snowballr.backend.model.dto.user.User
import se.uulm.snowballr.backend.model.dto.user.UserRole
import se.uulm.snowballr.backend.model.dto.user.UserStatus
import snowballr.UserOuterClass
import java.util.UUID

data class InvitationResponse(
    val userId: UUID?,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val status: UserStatus,
) {
    companion object {
        fun fromUser(user: User) = InvitationResponse(
            userId = user.id,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            role = user.role,
            status = user.status,
        )

        fun fromEmail(email: String) = InvitationResponse(
            userId = null,
            email = email,
            firstName = "",
            lastName = "",
            role = UserRole.DEFAULT,
            status = UserStatus.ACTIVE_UNCONFIRMED,
        )
    }
}

fun InvitationResponse.toGrpc(): UserOuterClass.User = UserOuterClass.User.newBuilder()
    .setId(userId?.toString().orEmpty())
    .setEmail(email)
    .setFirstName(firstName)
    .setLastName(lastName)
    .setRole(role.toGrpc())
    .setStatus(status.toGrpc())
    .build()

fun List<InvitationResponse>.toGrpc(): UserOuterClass.User.List = UserOuterClass.User.List.newBuilder()
    .addAllUsers(this.map { it.toGrpc() })
    .build()
