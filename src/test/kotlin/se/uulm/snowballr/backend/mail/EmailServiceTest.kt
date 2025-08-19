package se.uulm.snowballr.backend.mail

import com.icegreen.greenmail.configuration.GreenMailConfiguration
import com.icegreen.greenmail.junit5.GreenMailExtension
import com.icegreen.greenmail.util.ServerSetupTest
import io.mockk.mockk
import org.junit.jupiter.api.extension.RegisterExtension
import org.simplejavamail.MailException
import org.simplejavamail.api.mailer.Mailer
import se.uulm.snowballr.backend.env.EnvReader

/**
 * A base class for testing the [EmailService].
 *
 * It provides the necessary mocks, [GreenMailExtension] setup, and common environment configuration.
 */
open class EmailServiceTest {
    protected val testSenderName = "SnowballR Test"
    protected val testSenderEmail = "noreply@snowballr.test"
    protected val testFrontendUrl = "https://frontend.test"
    protected val recipientEmail = "test.user@example.com"

    protected val envReaderMock = mockk<EnvReader>()
    protected val mailerMock = mockk<Mailer>()
    protected val emailTemplateManagerMock = mockk<EmailTemplateManager>()

    // Helper class to throw a custom exception from within the mailer.
    protected class TestMailException(message: String, cause: Throwable? = null) : MailException(message, cause)

    companion object {
        @JvmField
        @RegisterExtension
        val greenMail: GreenMailExtension = GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withUser("user", "pass"))
            .withPerMethodLifecycle(true)
    }
}
