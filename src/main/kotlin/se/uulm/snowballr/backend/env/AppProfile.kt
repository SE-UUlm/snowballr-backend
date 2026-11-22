package se.uulm.snowballr.backend.env

/**
 * Represents the application's active configuration profile.
 * This dictates the default values for various environment settings.
 *
 * - **TESTING**: For local testing. Bypasses authentication.
 * - **DEVELOPMENT**: For local development. Enables features like user seeding.
 * - **PRODUCTION**: For live environments. Enforces stricter configuration and disables development features.
 */
enum class AppProfile {
    TESTING,
    DEVELOPMENT,
    PRODUCTION,
}
