package se.uulm.snowballr.backend.env

/**
 * Determines which email sender implementation is used to send emails.
 *
 * - **SMTP**: Sends emails via the configured SMTP server.
 * - **DUMMY**: Keeps sent emails in memory instead of sending them. Not allowed in production.
 */
enum class MailerImplementationType {
    SMTP,
    DUMMY,
    ;

    companion object {
        /**
         * Parses a string to a [MailerImplementationType]. The matching is case-insensitive.
         *
         * @param value The string value to parse.
         * @return The corresponding [MailerImplementationType], or null if the [value] doesn't match any of them.
         */
        fun fromString(value: String): MailerImplementationType? =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) }
    }
}
