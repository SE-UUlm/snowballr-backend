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
    ;

    companion object {
        /**
         * Parses a string to an [AppProfile], defaulting to [PRODUCTION] for safety.
         * The matching is case-insensitive.
         *
         * @param value The string value to parse.
         * @return The corresponding [AppProfile] or [PRODUCTION] if the input is invalid.
         */
        fun fromString(value: String?): AppProfile {
            return when (value?.uppercase()) {
                "TESTING" -> TESTING
                "DEVELOPMENT" -> DEVELOPMENT
                "PRODUCTION" -> PRODUCTION
                else -> PRODUCTION // Default to the safest profile
            }
        }
    }
}
