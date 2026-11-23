package se.uulm.snowballr.backend.mail

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class DummyEmailSenderTest {
    private val recipientEmail = "test.user@example.com"

    private val emailSender = DummyEmailSender()

    @Test
    fun `When no email was sent, then no sent emails are returned`() {
        assertThat(emailSender.getSentEmails()).isEmpty()
    }

    @Test
    fun `When sending an email, then it is kept in memory`() {
        emailSender.send(recipientEmail, "Subject", "<p>body</p>")

        assertThat(emailSender.getSentEmails()).containsExactly(
            DummyEmailSender.SentEmail(recipientEmail, "Subject", "<p>body</p>"),
        )
    }

    @Test
    fun `When sending multiple emails, then they are returned in the order they were sent`() {
        emailSender.send("first@example.com", "Subject", "")
        emailSender.send("second@example.com", "Subject", "")

        assertThat(emailSender.getSentEmails().map { it.to })
            .containsExactly("first@example.com", "second@example.com")
    }
}
