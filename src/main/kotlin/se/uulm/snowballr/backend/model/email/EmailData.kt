package se.uulm.snowballr.backend.model.email

/**
 * A sealed interface representing the data models for all possible email templates.
 */
sealed interface EmailData {
    /**
     * Data required for the email verification email.
     *
     * @property firstName The first name of the user to be included in the email.
     * @property verificationLink The link that the user must click to verify their email address.
     */
    data class EmailVerification(
        val firstName: String,
        val verificationLink: String,
    ) : EmailData

    /**
     * Data required for the "accept project invitation" email.
     *
     * @property firstName The first name of the user to be included in the email.
     * @property projectName The name of the project that the invitation is for.
     * @property acceptanceLink The link that the user must click to accept the invitation.
     */
    data class AcceptProjectInvitation(
        val firstName: String,
        val projectName: String,
        val acceptanceLink: String,
    ) : EmailData
}
