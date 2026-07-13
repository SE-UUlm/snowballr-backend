package se.uulm.snowballr.backend.model.dto.user

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
}
