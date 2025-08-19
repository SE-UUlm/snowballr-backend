package se.uulm.snowballr.backend.mail

import io.github.oshai.kotlinlogging.KotlinLogging
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.email.EmailTemplate

private val logger = KotlinLogging.logger {}

interface IEmailManager {
    /**
     * Sends a verification email to the specified recipient.
     *
     * @param to The recipient's email address.
     * @param data The data model containing the user's data.
     */
    fun sendVerificationEmail(to: String, data: EmailData.EmailVerification)

    /**
     * Sends an 'accept project invitation' email to the specified recipient.
     *
     * @param to The recipient's email address.
     * @param data The data model containing the user's data.
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
}

/**
 * The [EmailManager] class provides functionality to send emails using an SMTP mailer.
 *
 * This class implements the [IEmailManager] and is responsible for:
 * - Initializing the underlying SMTP mailer with configuration values from [se.uulm.snowballr.backend.env.EnvReader].
 * - Sending invitation emails to new users.
 * - Sending account verification emails to registered users.
 *
 * @constructor Initializes the [EmailManager] with the environment reader to access SMTP settings.
 * @param envReader The environment reader that provides access to SMTP configuration settings.
 * @param mailer The mailer that is used to send emails.
 * @param emailTemplateManager The manager that provides access to pre-compiled email templates.
 */
class EmailManager(
    private val envReader: EnvReader,
    private val mailer: Mailer,
    private val emailTemplateManager: EmailTemplateManager,
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
        try {
            val compiledTemplate = emailTemplateManager.getTemplate(template)
            val htmlBody = compiledTemplate.apply(data)

            val email = EmailBuilder
                .startingBlank()
                .to(to)
                .from(envReader.env.smtp.smtpSenderName, envReader.env.smtp.smtpSenderEmail)
                .withSubject(template.subject)
                .withHTMLText(htmlBody)
                .buildEmailCompletedWithDefaultsAndOverrides()

            mailer.sendMail(email)
            logger.info { "Successfully queued email for delivery to $to with template '${template.name}'" }
        } catch (e: MailException) {
            logger.error(e) { "Mailer failed to send email to $to with template '${template.name}'" }
            throw SnowballRException.EmailException.MailSendFailed(to, e)
        }
    }

    override fun createVerificationLink(token: String): String =
        "${envReader.env.miscellaneous.frontendBaseUrl}/verifyemail?token=$token"
}
