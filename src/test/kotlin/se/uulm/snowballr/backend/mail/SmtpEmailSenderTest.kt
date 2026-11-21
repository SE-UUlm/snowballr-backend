package se.uulm.snowballr.backend.mail

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import io.mockk.every
import io.mockk.mockk
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.model.exception.internal.email.MailSendFailedException

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SmtpEmailSenderTest {
    private val testSenderName = "SnowballR Test"
    private val testSenderEmail = "noreply@snowballr.test"
    private val recipientEmail = "test.user@example.com"
    private val testSubject = "Test Subject"
    private val testHtmlBody = "<p>Hello John,</p>"

    private val config = Env.Mailer.Smtp(
        host = "unused",
        port = 0,
        user = null,
        password = null,
        transportLoggingOnlyEnabled = false,
        senderName = testSenderName,
        senderEmail = testSenderEmail,
    )
    private val mailerMock = mockk<Mailer>()

    // Helper class to throw a custom exception from within the mailer.
    private class TestMailException(message: String, cause: Throwable? = null) : MailException(message, cause)

    companion object {
        @JvmField
        @RegisterExtension
        val greenMail: GreenMailExtension = GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "pass"))
            .withPerMethodLifecycle(true)
    }

    @Test
    fun `When sending an email, then the email is sent with the correct content and headers`() {
        val serverSetup = greenMail.smtp.serverSetup
        val mailer = MailerBuilder
            .withSMTPServer(serverSetup.bindAddress, serverSetup.port)
            .withTransportStrategy(TransportStrategy.SMTP)
            .withProperty("mail.smtp.starttls.enable", "false")
            .async()
            .buildMailer()

        val emailSender = SmtpEmailSender(config, mailer)

        emailSender.send(recipientEmail, testSubject, testHtmlBody)

        greenMail.waitForIncomingEmail(1)
        val receivedMessages: Array<MimeMessage> = greenMail.receivedMessages
        assertThat(receivedMessages).hasSize(1)

        val receivedMessage = receivedMessages[0]
        val fromHeader = receivedMessage.from[0].toString()
        val subject = receivedMessage.subject

        @Suppress("NullableToStringCall")
        val body = receivedMessage.content.toString()

        assertEquals(recipientEmail, receivedMessage.allRecipients[0].toString())
        assertThat(fromHeader).contains(testSenderName)
        assertThat(fromHeader).contains(testSenderEmail)
        assertEquals(testSubject, subject)
        assertThat(body).contains(testHtmlBody)
    }

    @Test
    fun `When mailer fails to send, then MailSendFailed exception is thrown`() {
        val emailSender = SmtpEmailSender(config, mailerMock)

        val mailerException = TestMailException("Mailer failed to send email")

        every { mailerMock.sendMail(any()) } throws mailerException

        val thrownException = assertThrows<MailSendFailedException> {
            emailSender.send(recipientEmail, testSubject, testHtmlBody)
        }
        assertEquals(mailerException, thrownException.cause)
    }
}
