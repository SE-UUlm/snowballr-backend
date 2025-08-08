package se.uulm.snowballr.backend.service.email

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import se.uulm.snowballr.backend.env.Env
import se.uulm.snowballr.backend.env.EnvReader
import se.uulm.snowballr.backend.service.EmailService
import se.uulm.snowballr.backend.service.IEmailService

/**
 * Test suite for the EmailService.
 *
 * Note: This test requires the Handlebars templates (.hbs files) to be present
 * in the test resources classpath under the `/templates` directory. This is necessary
 * because the EmailService compiles these templates during its initialization.
 */
class CreateEmailVerificationLinkTest {
    private val envReaderMock = mockk<EnvReader>()
    private lateinit var emailService: IEmailService

    private val testFrontendUrl = "https://example.com"

    @BeforeEach
    fun setUp() {
        val miscellaneousMock = mockk<Env.Miscellaneous>()
        every { miscellaneousMock.frontendBaseUrl } returns testFrontendUrl
        every { miscellaneousMock.logLevel } returns "INFO"

        val smtpMock = mockk<Env.SMTP>()
        every { smtpMock.smtpHost } returns "smtp.test.com"
        every { smtpMock.smtpPort } returns 587
        every { smtpMock.smtpTransportLoggingOnlyEnabled } returns true
        every { smtpMock.smtpUser } returns null
        every { smtpMock.smtpPassword } returns null
        every { smtpMock.smtpSenderName } returns "Test"
        every { smtpMock.smtpSenderEmail } returns "noreply@example.com"

        val envMock = mockk<Env>()
        every { envMock.miscellaneous } returns miscellaneousMock
        every { envMock.smtp } returns smtpMock

        every { envReaderMock.env } returns envMock

        emailService = EmailService(envReaderMock)
    }

    @Nested
    inner class CreateEmailVerificationLink {
        @Test
        fun `When given a token, then the correct verification link is created`() {
            val verificationToken = "random-test-token"
            val expectedLink = "$testFrontendUrl/verifyemail?token=$verificationToken"

            val actualLink = emailService.createEmailVerificationLink(verificationToken)

            assertThat(actualLink).isEqualTo(expectedLink)
        }
    }
}
