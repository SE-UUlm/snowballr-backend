package se.uulm.snowballr.backend.model.incoming.authentication

data class LoginRequest(
    val email: String,
    val password: String,
)
