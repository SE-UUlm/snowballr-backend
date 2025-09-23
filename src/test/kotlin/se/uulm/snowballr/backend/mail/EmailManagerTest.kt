package se.uulm.snowballr.backend.mail

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import jakarta.mail.internet.MimeMessage
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import org.simplejavamail.api.mailer.config.TransportStrategy
import org.simplejavamail.mailer.MailerBuilder
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.model.SnowballRException
import se.uulm.snowballr.backend.model.SnowballRException.EmailException
import se.uulm.snowballr.backend.model.email.EmailData
import se.uulm.snowballr.backend.model.email.EmailTemplate

/**
 * A base class for testing the [EmailManager].
 *
 * It provides the necessary mocks, [GreenMailExtension] setup, and common environment configuration.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailManagerTest {
    private val testSenderName = "SnowballR Test"
    private val testSenderEmail = "noreply@snowballr.test"
    private val testFrontendUrl = "https://frontend.test"
    private val recipientEmail = "test.user@example.com"

    private val envReaderMock = mockk<EnvReader>()
    private val mailerMock = mockk<Mailer>()
    private val emailTemplateManagerMock = mockk<EmailTemplateManager>()

    // Helper class to throw a custom exception from within the mailer.
    private class TestMailException(message: String, cause: Throwable? = null) : MailException(message, cause)

    companion object {
        @JvmField
        @RegisterExtension
        val greenMail: GreenMailExtension = GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "pass"))
            .withPerMethodLifecycle(true)
    }

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

    @AfterEach
    fun tearDown() {
        clearAllMocks()
    }

    @Nested
    inner class SendEmails {
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
            val emailData = EmailData.EmailVerification("John", expectedVerificationLink)

            emailManager.sendVerificationEmail(recipientEmail, emailData)

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
            assertEquals(EmailTemplate.EMAIL_VERIFICATION.subject, subject)
            assertThat(body).contains("Hello John,")
            assertThat(body).contains("<a href=\"$expectedVerificationLink\">$expectedVerificationLink</a>")
        }

        @Test
        fun `When sending a project invitation acceptance email, then the email is sent with the correct content and headers`() {
            val serverSetup = greenMail.smtp.serverSetup
            val mailer = MailerBuilder
                .withSMTPServer(serverSetup.bindAddress, serverSetup.port)
                .withTransportStrategy(TransportStrategy.SMTP)
                .withProperty("mail.smtp.starttls.enable", "false")
                .async()
                .buildMailer()
            val emailTemplateManager = EmailTemplateManager()

            val emailManager = EmailManager(envReaderMock, mailer, emailTemplateManager)

            val acceptProjectInvitationToken = "this-is-a-test-token-123"
            val expectedAcceptanceLink = emailManager.createAcceptProjectInvitationLink(acceptProjectInvitationToken)
            val emailData = EmailData.AcceptProjectInvitation("John", "Test Project", expectedAcceptanceLink)

            emailManager.sendAcceptProjectInvitationEmail(recipientEmail, emailData)

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
            assertEquals(EmailTemplate.ACCEPT_PROJECT_INVITATION.subject, subject)
            assertThat(body).contains("Hello John,")
            assertThat(body).contains("Test Project")
            assertThat(body).contains("<a href=\"$expectedAcceptanceLink\">$expectedAcceptanceLink</a>")
        }

        @Test
        fun `When mailer fails to send, then MailSendFailed exception is thrown`() {
            val emailTemplateManager = EmailTemplateManager()
            val emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManager)

            val emailData = EmailData.EmailVerification("John", "any-link")
            val mailerException = TestMailException("Mailer failed to send email")

            every { mailerMock.sendMail(any()) } throws mailerException

            val thrownException = assertThrows<EmailException.MailSendFailed> {
                emailManager.sendVerificationEmail(recipientEmail, emailData)
            }
            assertEquals(mailerException, thrownException.cause)
        }

        @Test
        fun `When template is not pre-compiled, then FailedPreconditionException is thrown`() {
            val emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManagerMock)

            val emailData = EmailData.EmailVerification("John", "any-link")

            every { emailTemplateManagerMock.getTemplate(any()) } throws
                SnowballRException.FailedPreconditionException("Template not pre-compiled")

            assertThrows<SnowballRException.FailedPreconditionException> {
                emailManager.sendVerificationEmail(recipientEmail, emailData)
            }
        }
    }

    @Nested
    inner class CreateLinks {
        private val emailManager = EmailManager(envReaderMock, mailerMock, emailTemplateManagerMock)

        @Test
        fun `When given a token, then the correct verification link is created`() {
            val verificationToken = "random-test-token"
            val expectedLink = "$testFrontendUrl/verifyemail?token=$verificationToken"

            val actualLink = emailManager.createVerificationLink(verificationToken)

            assertEquals(expectedLink, actualLink)
        }

        @Test
        fun `When given a token, then the correct accept project invitation link is created`() {
            val acceptProjectInvitationToken = "random-test-token"
            val expectedLink = "$testFrontendUrl/acceptprojectinvitation?token=$acceptProjectInvitationToken"

            val actualLink = emailManager.createAcceptProjectInvitationLink(acceptProjectInvitationToken)

            assertEquals(expectedLink, actualLink)
        }
    }
}
