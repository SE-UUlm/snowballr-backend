package se.uulm.snowballr.backend.service

import com.github.jknack.handlebars.Handlebars
import com.github.jknack.handlebars.Template
import com.github.jknack.handlebars.io.ClassPathTemplateLoader
import io.github.oshai.kotlinlogging.KotlinLogging
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.EmailTemplate
import se.uulm.snowballr.backend.model.SnowballRException.EmailException
import se.uulm.snowballr.backend.model.SnowballRException.FailedPreconditionException
import java.io.IOException

private val logger = KotlinLogging.logger {}

interface IEmailService {
    /**
     * Sends an email using a specific template and data model.
     *
     * @param to The recipient's email address.
     * @param template The [EmailTemplate] enum instance, containing the template name and subject.
     * @param data The data model (e.g., a Map) to populate the template.
     *
     * TODO: This method will be replaced by more specific methods like `sendInvitationEmail` in later issues
     */
    fun sendEmail(to: String, template: EmailTemplate, data: Any)
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
    private val compiledTemplates: Map<EmailTemplate, Template>

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
                env.smtp.smtpUser?.let { withSMTPServerUsername(it) }
                env.smtp.smtpPassword?.let { withSMTPServerPassword(it) }
            }
            .async()
            .buildMailer()

        val handlebars = Handlebars(ClassPathTemplateLoader("/templates", ".hbs"))

        // Compile all email templates at startup to ensure they are ready for use
        compiledTemplates = EmailTemplate.entries.associateWith { template ->
            logger.info { "Compiling email template: ${template.templateFileName}" }
            try {
                handlebars.compile(template.templateFileName)
            } catch (e: IOException) {
                logger.error(e) { "Could not find or compile template '${template.templateFileName}'." }
                throw EmailException.TemplateCompilationFailed(template.templateFileName, e)
            }
        }

        logger.info { "Initialized Mailer and compiled all email templates." }
    }

    override fun sendEmail(to: String, template: EmailTemplate, data: Any) {
        try {
            val compiledTemplate = compiledTemplates[template]
                ?: throw FailedPreconditionException("Template ${template.name} was not pre-compiled.")

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
            throw EmailException.MailSendFailed(to, e)
        }
    }
}
