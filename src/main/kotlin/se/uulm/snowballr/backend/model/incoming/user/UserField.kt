package se.uulm.snowballr.backend.model.incoming.user

enum class UserField(val grpcPath: String) {
    EMAIL("user.email"),
    FIRST_NAME("user.first_name"),
    LAST_NAME("user.last_name"),
    ROLE("user.role"),
    STATUS("user.status"),
}
