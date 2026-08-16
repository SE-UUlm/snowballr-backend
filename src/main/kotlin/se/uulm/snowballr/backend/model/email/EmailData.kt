package se.uulm.snowballr.backend.model.email

/**
 * A sealed interface representing the data models for all possible email templates.
 */
sealed interface EmailData {
    val subject: String

    /**
     * Data required for the email verification email.
     *
     * @property firstName The first name of the user to be included in the email.
     * @property verificationLink The link that the user must click to verify their email address.
     * @property expirationTime A human-readable string representing the time until the verification link expires.
     */
    data class EmailVerification(
        val firstName: String,
        val verificationLink: String,
        val expirationTime: String,
    ) : EmailData {
        override val subject: String = "Verify your SnowballR Account"
    }

    /**
     * Data required for the "accept project invitation" email.
     *
     * @property inviteeFirstName The first name of the invitee.
     * @property inviterName The name of the person who sent the invitation.
     * @property projectName The name of the project that the invitation is for.
     * @property acceptanceLink The link that the user must click to accept the invitation.
     * @property expirationTime A human-readable string representing the time until the invitation expires.
     */
    data class AcceptProjectInvitation(
        val inviteeFirstName: String,
        val inviterName: String,
        val projectName: String,
        val acceptanceLink: String,
        val expirationTime: String,
    ) : EmailData {
        override val subject: String = "$inviterName invited you to $projectName"
    }
}
