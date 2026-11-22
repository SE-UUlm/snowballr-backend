package se.uulm.snowballr.backend.mail

import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.email.EmailTemplate
import se.uulm.snowballr.backend.model.exception.internal.email.MailSendFailedException

interface IEmailManager {
    /**
     * Sends a verification email to the specified recipient.
     *
     * @param to The recipient's email address.
     * @param data The data model containing the user's data.
     * @throws MailSendFailedException if the email could not be sent.
     */
    fun sendVerificationEmail(to: String, data: EmailData.EmailVerification)

    /**
     * Sends an 'accept project invitation' email to the specified recipient.
     *
     * @param to The recipient's email address.
     * @param data The data model containing the user's data.
     * @throws MailSendFailedException if the email could not be sent.
     */
    fun sendAcceptProjectInvitationEmail(to: String, data: EmailData.AcceptProjectInvitation)

    /**
     * Creates a verification link for email verification.
     *
     * This method generates a complete URL that includes the base URL of the frontend and appends the verification token as a query parameter.
     *
     * @param token The verification token to include in the link.
     * @return A string representing the complete email verification link.
     */
    fun createVerificationLink(token: String): String

    /**
     * Creates a link for accepting a project invitation.
     *
     * This method generates a complete URL that includes the base URL of the frontend and appends the invitation token as a query parameter.
     *
     * @param token The invitation token to include in the link.
     * @return A string representing the complete project invitation link.
     */
    fun createAcceptProjectInvitationLink(token: String): String
}

/**
 * The [EmailManager] class is responsible for creating and sending emails.
 *
 * @param envReader The environment reader that provides access to the frontend base URL.
 * @param emailTemplateManager The manager that provides access to pre-compiled email templates.
 * @param emailSender The sender that delivers the rendered emails.
 */
class EmailManager(
    private val envReader: EnvReader,
    private val emailTemplateManager: EmailTemplateManager,
    private val emailSender: IEmailSender,
) : IEmailManager {
    override fun sendVerificationEmail(to: String, data: EmailData.EmailVerification) {
        sendEmail(to, EmailTemplate.EMAIL_VERIFICATION, data)
    }

    override fun sendAcceptProjectInvitationEmail(to: String, data: EmailData.AcceptProjectInvitation) {
        sendEmail(to, EmailTemplate.ACCEPT_PROJECT_INVITATION, data)
    }

    /**
     * Sends an email using the specified template and data.
     *
     * @param to The recipient's email address.
     * @param template The email template to use for the email.
     * @param data The data model containing the information to populate the email template.
     */
    private fun sendEmail(to: String, template: EmailTemplate, data: EmailData) {
        val htmlBody = emailTemplateManager.getTemplate(template).apply(data)

        emailSender.send(to, data.subject, htmlBody)
    }

    override fun createVerificationLink(token: String): String =
        "${envReader.env.miscellaneous.frontendBaseUrl}/verifyemail?token=$token"

    override fun createAcceptProjectInvitationLink(token: String): String =
        "${envReader.env.miscellaneous.frontendBaseUrl}/acceptprojectinvitation?token=$token"
}
