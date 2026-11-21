package se.uulm.snowballr.backend.mail

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.mail.Message
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.email.EmailBuilder
import org.simplejavamail.recipient.RecipientBuilder
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.model.exception.internal.email.MailSendFailedException

private val logger = KotlinLogging.logger {}

/**
 * An [IEmailSender] that sends emails using an SMTP mailer.
 *
 * @param config The SMTP configuration that provides the sender settings.
 * @param mailer The SMTP mailer that is used to send emails.
 */
class SmtpEmailSender(
    private val config: Env.Mailer.Smtp,
    private val mailer: Mailer,
) : IEmailSender {
    override fun send(to: String, subject: String, htmlBody: String) {
        try {
            val email = EmailBuilder
                .startingBlank()
                .withRecipients(
                    RecipientBuilder()
                        .withAddress(to)
                        .withType(Message.RecipientType.TO)
                        .build(),
                )
                .from(config.senderName, config.senderEmail)
                .withSubject(subject)
                .withHTMLText(htmlBody)
                .buildEmailCompletedWithDefaultsAndOverrides()

            mailer.sendMail(email)
            logger.info { "Successfully queued email for delivery to $to" }
        } catch (e: MailException) {
            logger.error(e) { "Mailer failed to send email to $to" }
            throw MailSendFailedException(to, e)
        }
    }
}
