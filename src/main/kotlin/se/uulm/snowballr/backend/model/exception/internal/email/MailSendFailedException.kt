package se.uulm.snowballr.backend.model.exception.internal.email

import org.simplejavamail.MailException
import se.uulm.snowballr.backend.model.exception.internal.EmailException

/**
 * Thrown when the email provider fails to send an email.
 *
 * @param recipient The email address of the intended recipient.
 * @param cause The original [MailException] from the mailer library.
 */
class MailSendFailedException(
    recipient: String,
    cause: MailException,
) : EmailException("Mailer failed to send email to '$recipient'.", cause)
