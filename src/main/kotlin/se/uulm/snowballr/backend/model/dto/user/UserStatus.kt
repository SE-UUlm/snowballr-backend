package se.uulm.snowballr.backend.model.dto.user

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
}
