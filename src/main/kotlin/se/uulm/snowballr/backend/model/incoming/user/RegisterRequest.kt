package se.uulm.snowballr.backend.model.incoming.user

data class RegisterRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String,
)
