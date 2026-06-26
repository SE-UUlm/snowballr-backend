package se.uulm.snowballr.backend.model.dto.user

import snowballr.UserOuterClass

/**
 * Status of a user. Can be considered the stage in the lifecycle of a user.
 */
enum class UserStatus {
    /**
     * User is registered, but has not yet verified their email address.
     */
    USER_STATUS_ACTIVE_UNCONFIRMED,

    /**
     * User is registered and has confirmed their email address.
     */
    USER_STATUS_ACTIVE,

    /**
     * User has requested the deletion of their account.
     */
    USER_STATUS_DELETED,

    /**
     * User data has been cleared after the user has been soft-deleted.
     */
    USER_STATUS_CLEARED,

    ;

    companion object {
        fun fromGrpc(status: UserOuterClass.UserStatus): UserStatus = when (status) {
            UserOuterClass.UserStatus.USER_STATUS_ACTIVE -> USER_STATUS_ACTIVE
            UserOuterClass.UserStatus.USER_STATUS_DELETED -> USER_STATUS_DELETED
            UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED -> USER_STATUS_ACTIVE_UNCONFIRMED
            UserOuterClass.UserStatus.UNRECOGNIZED, UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED ->
                @Suppress("UseCheckOrError")
                throw IllegalStateException("Invalid convertion")
        }
    }

    fun toGrpc() = when (this) {
        USER_STATUS_ACTIVE_UNCONFIRMED -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE_UNCONFIRMED
        USER_STATUS_ACTIVE -> UserOuterClass.UserStatus.USER_STATUS_ACTIVE
        USER_STATUS_DELETED -> UserOuterClass.UserStatus.USER_STATUS_DELETED
        USER_STATUS_CLEARED -> UserOuterClass.UserStatus.USER_STATUS_UNSPECIFIED
    }
}
