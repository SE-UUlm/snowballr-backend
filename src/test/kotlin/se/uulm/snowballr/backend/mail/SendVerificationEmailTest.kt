package se.uulm.snowballr.backend.mail

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.EmailException
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.email.EmailTemplate

class SendVerificationEmailTest : EmailManagerTest() {
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

        val emailManager = EmailManager(envReaderMock, mailer, emailTemplateManager)

        val verificationToken = "this-is-a-test-token-123"
        val expectedVerificationLink = emailManager.createVerificationLink(verificationToken)
        val emailData = EmailData.EmailVerification("John", "Doe", expectedVerificationLink)

        emailManager.sendVerificationEmail(recipientEmail, emailData)

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
        assertThat(body).contains("<a href=\"$expectedVerificationLink\">$expectedVerificationLink</a>")
    }

    @Test
    fun `When mailer fails to send, then MailSendFailed exception is thrown`() {
        val emailTemplateManager = EmailTemplateManager()
        val emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManager)

        val emailData = EmailData.EmailVerification("John", "Doe", "any-link")
        val mailerException = TestMailException("Mailer failed to send email")

        every { mailerMock.sendMail(any()) } throws mailerException

        val thrownException = assertThrows<EmailException.MailSendFailed> {
            emailManager.sendVerificationEmail(recipientEmail, emailData)
        }
        assertThat(thrownException.cause).isEqualTo(mailerException)

        verify(exactly = 1) { mailerMock.sendMail(any()) }
    }

    @Test
    fun `When template is not pre-compiled, then FailedPreconditionException is thrown`() {
        val emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManagerMock)

        val emailData = EmailData.EmailVerification("John", "Doe", "any-link")

        every { emailTemplateManagerMock.getTemplate(any()) } throws
            SnowballRException.FailedPreconditionException("Template not pre-compiled")

        assertThrows<SnowballRException.FailedPreconditionException> {
            emailManager.sendVerificationEmail(recipientEmail, emailData)
        }

        verify(exactly = 0) { mailerMock.sendMail(any()) }
    }
}
