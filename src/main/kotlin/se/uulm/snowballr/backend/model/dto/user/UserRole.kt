package se.uulm.snowballr.backend.model.dto.user

import snowballr.UserOuterClass

/**
 * Role of a user inside the SnowballR app.
 */
enum class UserRole {
    /**
     * Default user role. No elevated rights.
     */
    USER_ROLE_DEFAULT,

    /**
     * Admin user role. Has elevated rights.
     */
    USER_ROLE_ADMIN,

    ;

    companion object {
        fun fromGrpc(role: UserOuterClass.UserRole): UserRole = when (role) {
            UserOuterClass.UserRole.USER_ROLE_DEFAULT -> USER_ROLE_DEFAULT
            UserOuterClass.UserRole.USER_ROLE_ADMIN -> USER_ROLE_ADMIN
            UserOuterClass.UserRole.UNRECOGNIZED, UserOuterClass.UserRole.USER_ROLE_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        USER_ROLE_DEFAULT -> UserOuterClass.UserRole.USER_ROLE_DEFAULT
        USER_ROLE_ADMIN -> UserOuterClass.UserRole.USER_ROLE_ADMIN
    }
}
