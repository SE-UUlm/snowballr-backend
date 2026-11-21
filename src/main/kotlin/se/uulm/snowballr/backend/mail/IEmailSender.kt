package se.uulm.snowballr.backend.mail

import se.uulm.snowballr.backend.model.exception.internal.email.MailSendFailedException

/**
 * Delivers emails.
 */
interface IEmailSender {
    /**
     * @throws MailSendFailedException if the email could not be delivered.
     */
    fun send(to: String, subject: String, htmlBody: String)
}
