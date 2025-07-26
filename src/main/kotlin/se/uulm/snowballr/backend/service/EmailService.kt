package se.uulm.snowballr.backend.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.EnvReader

private val logger = KotlinLogging.logger {}

interface IEmailService {
    /**
     * Sends an email with the specified subject and HTML body to the given recipient.
     *
     * @param to The recipient's email address.
     * @param subject The subject of the email.
     * @param htmlBody The HTML content of the email.
     *
     * TODO: This method will be replaced by more specific methods like `sendInvitationEmail` in later issues
     */
    fun sendEmail(to: String, subject: String, htmlBody: String)
}

/**
 * The [EmailService] class provides functionality to send emails using an SMTP mailer.
 *
 * This class implements the [IEmailService] and is responsible for:
 * - Initializing the underlying SMTP mailer with configuration values from [EnvReader].
 * - Sending invitation emails to new users.
 * - Sending account verification emails to registered users.
 *
 * @constructor Initializes the [EmailService] with the environment reader to access SMTP settings.
 * @param envReader The environment reader that provides access to SMTP configuration settings.
 */
class EmailService(
    private val envReader: EnvReader,
) : IEmailService {
    private val mailer: Mailer

    init {
        val env = envReader.env

        logger.info { "Initializing Mailer for email service" }

        // Enable debug logging if the log level is DEBUG or TRACE
        val isDebugLogging = env.miscellaneous.logLevel == "DEBUG" || env.miscellaneous.logLevel == "TRACE"

        mailer = MailerBuilder
            .withSMTPServer(env.smtp.smtpHost, env.smtp.smtpPort)
            .withTransportModeLoggingOnly(env.smtp.smtpTransportLoggingOnlyEnabled)
            .withDebugLogging(isDebugLogging)
            .apply {
                val username = env.smtp.smtpUser
                if (username != null) {
                    withSMTPServerUsername(username)
                }
                val password = env.smtp.smtpPassword
                if (password != null) {
                    withSMTPServerPassword(password)
                }
            }
            .buildMailer()

        logger.info { "Initialized Mailer" }
    }

    override fun sendEmail(to: String, subject: String, htmlBody: String) {
        try {
            val email = EmailBuilder
                .startingBlank()
                .to(to)
                .from(envReader.env.smtp.smtpSenderName, envReader.env.smtp.smtpSenderEmail)
                .withSubject(subject)
                .withHTMLText(htmlBody)
                .buildEmailCompletedWithDefaultsAndOverrides()

            mailer.sendMail(email, true)
        } catch (e: MailException) {
            logger.error(e) { "Failed to send email to $to with subject '$subject'" }
        }
    }
}
