package se.uulm.snowballr.backend.model.dto.user

import snowballr.UserOuterClass

/**
 * Status of a user. Can be considered the stage in the lifecycle of a user.
 */
enum class UserStatus {
    /**
     * User is registered, but has not yet verified their email address.
     */
    ACTIVE_UNCONFIRMED,

    /**
     * User is registered and has confirmed their email address.
     */
    ACTIVE,

    /**
     * User has requested the deletion of their account.
     */
    DELETED,

    /**
     * User data has been cleared after the user has been soft-deleted.
     */
    CLEARED,

    ;

    companion object {
        fun fromGrpc(status: UserOuterClass.UserStatus): UserStatus = when (status) {
            UserOuterClass.UserStatus.USER_STATUS_ACTIVE -> ACTIVE
            UserOuterClass.UserStatus.USER_STATUS_DELETED -> DELETED
            UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED -> ACTIVE_UNCONFIRMED
            UserOuterClass.UserStatus.UNRECOGNIZED, UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid conversion")
        }
    }

    fun toGrpc() = when (this) {
        ACTIVE_UNCONFIRMED -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        ACTIVE -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE
        DELETED -> UserOuterClass.UserStatus.USER_STATUS_DELETED
        CLEARED -> UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED
    }
}
