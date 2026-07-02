package se.uulm.snowballr.backend.model.incoming.authentication

data class ChangePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
)
