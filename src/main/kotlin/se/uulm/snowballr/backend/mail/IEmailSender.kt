package se.uulm.snowballr.backend.mail

import se.uulm.snowballr.backend.model.exception.internal.email.MailSendFailedException

/**
 * Delivers emails.
 */
interface IEmailSender {
    /**
     * Sends an email with the given subject and HTML body to the given recipient.
     *
     * @param to The recipient's email address.
     * @param subject The subject line of the email.
     * @param htmlBody The already rendered HTML content of the email.
     * @throws MailSendFailedException if the email could not be delivered.
     */
    fun send(to: String, subject: String, htmlBody: String)
}
