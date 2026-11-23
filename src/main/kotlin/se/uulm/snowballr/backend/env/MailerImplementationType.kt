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
}
