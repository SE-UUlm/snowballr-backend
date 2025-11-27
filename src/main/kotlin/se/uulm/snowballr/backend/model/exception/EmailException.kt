package se.uulm.snowballr.backend.model.exception

import io.grpc.Status
import org.simplejavamail.MailException
import java.io.IOException

/**
 * Represents an exception that occurs within the [se.uulm.snowballr.backend.mail.EmailManager].
 *
 * @param message The message describing the email-related error.
 * @param cause The cause of the exception, which can be another exception, or null
 */
sealed class EmailException(
    message: String,
    cause: Throwable? = null,
) : SnowballRException(Status.INTERNAL, message, cause) {
    /**
     * Thrown when an email template file cannot be found or compiled during application startup.
     * This is a fatal startup error.
     *
     * @param templateFileName The name of the file that failed to compile.
     * @param cause The original [java.io.IOException] from the template engine.
     */
    class TemplateCompilationFailed(
        templateFileName: String,
        cause: IOException,
    ) : EmailException("Failed to compile email template '$templateFileName'.", cause)

    /**
     * Thrown when the email provider fails to send an email.
     *
     * @param recipient The email address of the intended recipient.
     * @param cause The original [org.simplejavamail.MailException] from the mailer library.
     */
    class MailSendFailed(
        recipient: String,
        cause: MailException,
    ) : EmailException("Mailer failed to send email to '$recipient'.", cause)
}
