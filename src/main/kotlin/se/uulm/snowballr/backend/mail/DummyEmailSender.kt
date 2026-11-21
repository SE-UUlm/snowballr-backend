package se.uulm.snowballr.backend.mail

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * An [IEmailSender] that keeps sent emails in memory instead of sending them.
 */
class DummyEmailSender : IEmailSender {
    private val sentEmails = mutableListOf<SentEmail>()

    override fun send(to: String, subject: String, htmlBody: String) {
        synchronized(sentEmails) { sentEmails.add(SentEmail(to, subject, htmlBody)) }

        logger.info { "Kept email to $to in memory" }
    }

    /**
     * Returns a snapshot of all emails sent so far, in the order they were sent.
     */
    fun getSentEmails(): List<SentEmail> = synchronized(sentEmails) { sentEmails.toList() }

    /**
     * Removes all emails sent so far.
     */
    fun clearSentEmails() {
        synchronized(sentEmails) { sentEmails.clear() }
    }

    data class SentEmail(
        val to: String,
        val subject: String,
        val htmlBody: String,
    )
}
