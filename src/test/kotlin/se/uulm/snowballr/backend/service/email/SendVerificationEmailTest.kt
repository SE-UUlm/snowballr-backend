package se.uulm.snowballr.backend.service.email

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.mail.EmailTemplateManager
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.EmailException
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.email.EmailTemplate
import se.uulm.snowballr.backend.service.EmailService

class SendVerificationEmailTest {
    companion object {
        @JvmField
        @RegisterExtension
        val greenMail: GreenMailExtension = GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "pass"))
            .withPerMethodLifecycle(true)
    }

    // Helper class to throw a custom exception from within the mailer.
    private class TestMailException(message: String, cause: Throwable? = null) : MailException(message, cause)

    private val envReaderMock = mockk<EnvReader>()
    private val testSenderName = "SnowballR Test"
    private val testSenderEmail = "noreply@snowballr.test"
    private val testFrontendUrl = "https://frontend.test"

    private val emailTemplateManagerMock = mockk<EmailTemplateManager>()

    @BeforeEach
    fun setUp() {
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns testFrontendUrl

        val smtpMock = mockk<Env.SMTP>()
        every { smtpMock.smtpSenderName } returns testSenderName
        every { smtpMock.smtpSenderEmail } returns testSenderEmail

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock
        every { envMock.smtp } returns smtpMock

        every { envReaderMock.env } returns envMock
    }

    @Test
    fun `When sending a verification email, then the email is sent with the correct content and headers`() {
        val serverSetup = greenMail.smtp.serverSetup
        val mailer = MailerBuilder
            .withSMTPServer(serverSetup.bindAddress, serverSetup.port)
            .withTransportStrategy(TransportStrategy.SMTP)
            .withProperty("mail.smtp.starttls.enable", "false")
            .async()
            .buildMailer()
        val emailTemplateManager = EmailTemplateManager()

        val emailService = EmailService(envReaderMock, mailer, emailTemplateManager)

        val recipientEmail = "test.user@example.com"
        val verificationToken = "this-is-a-test-token-123"
        val expectedVerificationLink = emailService.createVerificationLink(verificationToken)
        val emailData = EmailData.EmailVerification(
            firstName = "John",
            lastName = "Doe",
            verificationLink = expectedVerificationLink,
        )

        emailService.sendVerificationEmail(recipientEmail, emailData)

        greenMail.waitForIncomingEmail(1)
        val receivedMessages: Array<MimeMessage> = greenMail.receivedMessages
        assertThat(receivedMessages).hasSize(1)

        val receivedMessage = receivedMessages[0]
        val fromHeader = receivedMessage.from[0].toString()
        val subject = receivedMessage.subject

        @Suppress("NullableToStringCall")
        val body = receivedMessage.content.toString()

        assertThat(receivedMessage.allRecipients[0].toString()).isEqualTo(recipientEmail)
        assertThat(fromHeader).contains(testSenderName)
        assertThat(fromHeader).contains(testSenderEmail)
        assertThat(subject).isEqualTo(EmailTemplate.EMAIL_VERIFICATION.subject)
        assertThat(body).contains("Hello John,")
        assertThat(body).contains("href=\"$expectedVerificationLink\"")
        assertThat(body).contains(">$expectedVerificationLink</a>")
    }

    @Test
    fun `When mailer fails to send, then MailSendFailed exception is thrown`() {
        val mailerMock = mockk<Mailer>()
        val emailTemplateManager = EmailTemplateManager()

        val emailService = EmailService(envReaderMock, mailerMock, emailTemplateManager)

        val recipientEmail = "test.user@example.com"
        val emailData = EmailData.EmailVerification(
            firstName = "John",
            lastName = "Doe",
            verificationLink = "any-link",
        )
        val mailerException = TestMailException("Mailer failed to send email")

        every { mailerMock.sendMail(any()) } throws mailerException

        val thrownException = assertThrows<EmailException.MailSendFailed> {
            emailService.sendVerificationEmail(recipientEmail, emailData)
        }
        assertThat(thrownException.cause).isEqualTo(mailerException)

        verify(exactly = 1) { mailerMock.sendMail(any()) }
    }

    @Test
    fun `When template is not pre-compiled, then FailedPreconditionException is thrown`() {
        val mailerMock = mockk<Mailer>()
        val emailService = EmailService(envReaderMock, mailerMock, emailTemplateManagerMock)

        val recipientEmail = "test.user@example.com"
        val emailData = EmailData.EmailVerification(
            firstName = "John",
            lastName = "Doe",
            verificationLink = "any-link",
        )

        every { emailTemplateManagerMock.getTemplate(any()) } throws SnowballRException.FailedPreconditionException("Template not pre-compiled")

        assertThrows<SnowballRException.FailedPreconditionException> {
            emailService.sendVerificationEmail(
                recipientEmail,
                emailData,
            )
        }

        verify(exactly = 0) { mailerMock.sendMail(any()) }
    }
}
