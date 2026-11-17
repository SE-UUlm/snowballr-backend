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
         * Parses a string to an [AppProfile]. The matching is case-insensitive.
         *
         * @param value The string value to parse.
         * @return The corresponding [AppProfile], or null if the [value] doesn't match an [AppProfile].
         */
        fun fromString(value: String): AppProfile? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
