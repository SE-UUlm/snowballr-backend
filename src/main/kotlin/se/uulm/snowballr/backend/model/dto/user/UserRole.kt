package se.uulm.snowballr.backend.model.dto.user

import snowballr.UserOuterClass

/**
 * Role of a user inside the SnowballR app.
 */
enum class UserRole {
    /**
     * Default user role. No elevated rights.
     */
    DEFAULT,

    /**
     * Admin user role. Has elevated rights.
     */
    ADMIN,

    ;

    companion object {
        fun fromGrpc(role: UserOuterClass.UserRole): UserRole = when (role) {
            UserOuterClass.UserRole.USER_ROLE_DEFAULT -> DEFAULT
            UserOuterClass.UserRole.USER_ROLE_ADMIN -> ADMIN
            UserOuterClass.UserRole.UNRECOGNIZED, UserOuterClass.UserRole.USER_ROLE_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        DEFAULT -> UserOuterClass.UserRole.USER_ROLE_DEFAULT
        ADMIN -> UserOuterClass.UserRole.USER_ROLE_ADMIN
    }
}
